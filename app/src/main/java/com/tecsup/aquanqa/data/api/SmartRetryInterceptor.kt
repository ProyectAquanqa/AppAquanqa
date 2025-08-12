package com.tecsup.aquanqa.data.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlin.math.min
import kotlin.math.pow

/**
 * Interceptor inteligente que implementa politicas de retry sofisticadas
 * basadas en el tipo de error, estado de conectividad, y criticidad de la operacion
 */
class SmartRetryInterceptor(
    private val context: Context
) : Interceptor {
    
    private val connectivityManager = NetworkConnectivityManager.getInstance(context)
    
    companion object {
        private const val TAG = "SmartRetryInterceptor"
        private const val MAX_ABSOLUTE_RETRIES = 10 // Limite absoluto para evitar loops infinitos
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 30000L // 30 segundos max por el delay
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Determinar tipo de operacion basado en URL y headers
        val operationType = determineOperationType(originalRequest)
        
        // Verificar si deberiamos proceder con la operacion
        if (!connectivityManager.shouldProceedWithOperation(operationType)) {
            Log.w(TAG, "Skipping operation due to poor connectivity: $operationType")
            
            // Para operaciones criticas, esperar por conectividad
            if (isCriticalOperation(operationType)) {
                return runBlocking {
                    if (connectivityManager.waitForConnectivity(30000L)) {
                        Log.i(TAG, "Connectivity restored, proceeding with critical operation")
                        executeWithRetry(chain, originalRequest, operationType)
                    } else {
                        Log.e(TAG, "Connectivity timeout for critical operation")
                        // Crear respuesta de error personalizada
                        createConnectivityErrorResponse(originalRequest)
                    }
                }
            } else {
                return createConnectivityErrorResponse(originalRequest)
            }
        }
        
        return executeWithRetry(chain, originalRequest, operationType)
    }
    
    /**
     * Ejecuta la request con retry inteligente
     */
    private fun executeWithRetry(
        chain: Interceptor.Chain,
        originalRequest: Request,
        operationType: NetworkConfig.OperationType
    ): Response {
        val connectivityState = connectivityManager.connectivityState.value
        val timeoutConfig = NetworkConfig.getAdjustedTimeouts(operationType, connectivityState)
        
        var lastException: Exception? = null
        
        for (attempt in 0 until min(timeoutConfig.maxRetries, MAX_ABSOLUTE_RETRIES)) {
            try {
                Log.v(TAG, "Attempt ${attempt + 1}/${timeoutConfig.maxRetries} for ${originalRequest.url}")
                
                // Ejecutar request
                val response = chain.proceed(originalRequest)
                
                // Verificar si la respuesta es exitosa o si necesita retry
                val errorType = analyzeResponse(response)
                
                if (errorType == null) {
                    // Respuesta exitosa
                    Log.v(TAG, "Successful response on attempt ${attempt + 1}")
                    return response
                }
                
                // Analizar si debemos hacer retry
                if (!errorType.shouldRetry || attempt == timeoutConfig.maxRetries - 1) {
                    Log.d(TAG, "Not retrying: errorType=$errorType, attempt=${attempt + 1}")
                    return response
                }
                
                // Cerrar la respuesta actual antes de retry (CRÍTICO)
                // Esto previene el error: "cannot make a new request because the previous response is still open"
                response.close()
                
                // Calcular delay para retry
                val delay = calculateRetryDelay(errorType, attempt, timeoutConfig.retryDelay)
                Log.d(TAG, "Retrying after ${delay}ms due to: $errorType")
                
                runBlocking { delay(delay) }
                
            } catch (e: Exception) {
                Log.w(TAG, "Request failed on attempt ${attempt + 1}: ${e.message}")
                lastException = e
                
                val errorType = analyzeException(e)
                
                // Si no debemos hacer retry o es el ultimo intento
                if (!errorType.shouldRetry || attempt == timeoutConfig.maxRetries - 1) {
                    Log.e(TAG, "Giving up after ${attempt + 1} attempts", e)
                    throw e
                }
                
                // Verificar conectividad antes del siguiente intento
                if (errorType == NetworkConfig.NetworkErrorType.NO_CONNECTION ||
                    errorType == NetworkConfig.NetworkErrorType.TIMEOUT) {
                    
                    connectivityManager.checkConnectivityNow()
                    
                    // Para operaciones criticas, esperar por mejor conectividad
                    if (isCriticalOperation(operationType)) {
                        runBlocking {
                            val waitTime = min(10000L, calculateRetryDelay(errorType, attempt, timeoutConfig.retryDelay))
                            if (!connectivityManager.waitForConnectivity(waitTime)) {
                                Log.w(TAG, "Connectivity did not improve, continuing with retry")
                            }
                        }
                    }
                }
                
                // Calcular delay para retry
                val delay = calculateRetryDelay(errorType, attempt, timeoutConfig.retryDelay)
                Log.d(TAG, "Retrying after ${delay}ms due to exception: ${e.javaClass.simpleName}")
                
                runBlocking { delay(delay) }
            }
        }
        
        // Si llegamos aqui, todos los intentos fallaron
        lastException?.let { throw it }
        
        // Fallback (no deberia llegar aqui por las validaciones implementada anteroirmente )
        throw IOException("All retry attempts failed")
    }
    
    /**
     * Determina el tipo de operacion basado en la request
     */
    private fun determineOperationType(request: Request): NetworkConfig.OperationType {
        val url = request.url.toString()
        val path = request.url.encodedPath
        
        // Verificar header personalizado primero
        request.header(NetworkConfig.CriticalHeaders.OPERATION_TYPE)?.let { operationType ->
            try {
                return NetworkConfig.OperationType.valueOf(operationType)
            } catch (e: Exception) {
                Log.w(TAG, "Invalid operation type header: $operationType")
            }
        }
        
        // Determinar basado en URL
        return when {
            path.contains("/auth/login") -> NetworkConfig.OperationType.AUTHENTICATION
            path.contains("/auth/refresh") -> NetworkConfig.OperationType.TOKEN_REFRESH
            path.contains("/auth/") -> NetworkConfig.OperationType.SESSION_OPERATIONS
            path.contains("/user/") -> NetworkConfig.OperationType.SESSION_OPERATIONS
            path.contains("/upload") || request.method == "POST" && 
                request.body?.contentLength() ?: 0 > 1024 * 1024 -> NetworkConfig.OperationType.FILE_UPLOAD
            path.contains("/chat") || path.contains("/realtime") -> NetworkConfig.OperationType.REALTIME
            request.method == "GET" -> NetworkConfig.OperationType.CONTENT_LOADING
            else -> NetworkConfig.OperationType.DEFAULT
        }
    }
    
    /**
     * Analiza la respuesta para determinar si necesita retry
     */
    private fun analyzeResponse(response: Response): NetworkConfig.NetworkErrorType? {
        return when (response.code) {
            in 200..299 -> null // Exito
            408 -> NetworkConfig.NetworkErrorType.TIMEOUT
            429 -> NetworkConfig.NetworkErrorType.RATE_LIMITED
            in 400..499 -> {
                if (response.code == 401 || response.code == 403) {
                    NetworkConfig.NetworkErrorType.AUTHENTICATION_ERROR
                } else {
                    NetworkConfig.NetworkErrorType.CLIENT_ERROR_4XX
                }
            }
            in 500..599 -> NetworkConfig.NetworkErrorType.SERVER_ERROR_5XX
            else -> NetworkConfig.NetworkErrorType.SERVER_ERROR_5XX
        }
    }
    
    /**
     * Analiza excepciones para determinar tipo de error
     */
    private fun analyzeException(exception: Exception): NetworkConfig.NetworkErrorType {
        return when (exception) {
            is SocketTimeoutException -> NetworkConfig.NetworkErrorType.TIMEOUT
            is ConnectException -> NetworkConfig.NetworkErrorType.NO_CONNECTION
            is UnknownHostException -> NetworkConfig.NetworkErrorType.DNS_ERROR
            is SSLException -> NetworkConfig.NetworkErrorType.SSL_ERROR
            else -> NetworkConfig.NetworkErrorType.NO_CONNECTION
        }
    }
    
    /**
     * Calcula el delay para retry usando exponential backoff
     */
    private fun calculateRetryDelay(
        errorType: NetworkConfig.NetworkErrorType,
        attempt: Int,
        baseDelay: Long
    ): Long {
        val delay = if (errorType.exponentialBackoff) {
            // Exponential backoff: baseDelay * 2^attempt
            (baseDelay * 2.0.pow(attempt.toDouble())).toLong()
        } else {
            // Delay fijo mas tiempo base del error
            baseDelay + (errorType.retryAfterSeconds * 1000)
        }
        
        // Aplicar jitter aleatorio (+/- 20%) para evitar thundering herd
        val jitter = (delay * 0.2 * (Math.random() - 0.5)).toLong()
        
        return min(delay + jitter, MAX_DELAY_MS)
    }
    
    /**
     * Verifica si una operacion es critica
     */
    private fun isCriticalOperation(operationType: NetworkConfig.OperationType): Boolean {
        return when (operationType) {
            NetworkConfig.OperationType.AUTHENTICATION,
            NetworkConfig.OperationType.TOKEN_REFRESH,
            NetworkConfig.OperationType.SESSION_OPERATIONS -> true
            else -> false
        }
    }
    
    /**
     * Crea una respuesta de error cuando no hay conectividad
     */
    private fun createConnectivityErrorResponse(request: Request): Response {
        return Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(503) // Service Unavailable
            .message("No network connectivity")
            .body(okhttp3.ResponseBody.create(null, "No network connectivity available"))
            .build()
    }
    
    /**
     * Obtiene estadisticas de retry para debugging
     */
    fun getRetryStats(): String {
        return buildString {
            appendLine("=== Smart Retry Statistics ===")
            appendLine("Connectivity State: ${connectivityManager.connectivityState.value}")
            appendLine("Network Type: ${connectivityManager.networkType.value}")
            appendLine("Is Connected: ${connectivityManager.isConnected.value}")
            appendLine("Network Stats:")
            append(connectivityManager.getNetworkStats())
        }
    }
}

/**
 * Extension para anadir headers de operacion critica
 */
fun Request.Builder.setCriticalOperation(operationType: NetworkConfig.OperationType): Request.Builder {
    return this.header(NetworkConfig.CriticalHeaders.OPERATION_TYPE, operationType.name)
        .header(NetworkConfig.CriticalHeaders.SESSION_CRITICAL, "true")
}

/**
 * Extension para verificar si una request es critica
 */
fun Request.isCriticalOperation(): Boolean {
    return this.header(NetworkConfig.CriticalHeaders.SESSION_CRITICAL) == "true"
}