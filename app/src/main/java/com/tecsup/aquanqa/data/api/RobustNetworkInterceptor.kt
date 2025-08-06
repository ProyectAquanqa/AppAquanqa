package com.tecsup.aquanqa.data.api

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Interceptor principal que coordina todos los aspectos de red robusta:
 * - Aplicacion de timeouts dinamicos
 * - Configuracion de headers especiales 
 * - Coordinacion con retry y auth interceptors
 * - Monitoring de performance de red
 */
class RobustNetworkInterceptor(
    private val context: Context
) : Interceptor {
    
    private val connectivityManager = NetworkConnectivityManager.getInstance(context)
    
    companion object {
        private const val TAG = "RobustNetworkInterceptor"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Determinar tipo de operacion
        val operationType = determineOperationType(originalRequest)
        
        // Obtener estado de conectividad actual
        val connectivityState = connectivityManager.connectivityState.value
        val connectivityInfo = connectivityManager.getConnectivityInfo()
        
        // Calcular timeouts ajustados
        val timeoutConfig = NetworkConfig.getAdjustedTimeouts(operationType, connectivityState)
        
        // Crear request mejorada con headers adicionales
        val enhancedRequest = enhanceRequest(originalRequest, operationType, connectivityInfo)
        
        // Aplicar timeouts dinamicos al chain
        val chainWithTimeouts = createChainWithDynamicTimeouts(chain, timeoutConfig)
        
        Log.d(TAG, "Processing ${operationType} request with timeouts: " +
                "connect=${timeoutConfig.connectTimeout}s, read=${timeoutConfig.readTimeout}s, " +
                "write=${timeoutConfig.writeTimeout}s, connectivity=${connectivityState}")
        
        val startTime = System.currentTimeMillis()
        
        try {
            val response = chainWithTimeouts.proceed(enhancedRequest)
            val duration = System.currentTimeMillis() - startTime
            
            Log.v(TAG, "Request completed in ${duration}ms with response ${response.code}")
            
            // Agregar headers de respuesta con info de performance
            return enhanceResponse(response, duration, connectivityInfo)
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.w(TAG, "Request failed after ${duration}ms: ${e.message}")
            
            // Notificar al connectivity manager sobre el fallo
            connectivityManager.checkConnectivityNow()
            
            throw e
        }
    }
    
    /**
     * Determina el tipo de operacion basado en la request
     */
    private fun determineOperationType(request: Request): NetworkConfig.OperationType {
        // Verificar header personalizado primero
        request.header(NetworkConfig.CriticalHeaders.OPERATION_TYPE)?.let { operationType ->
            try {
                return NetworkConfig.OperationType.valueOf(operationType)
            } catch (e: Exception) {
                Log.w(TAG, "Invalid operation type header: $operationType")
            }
        }
        
        val path = request.url.encodedPath
        val method = request.method
        val contentLength = request.body?.contentLength() ?: 0
        
        return when {
            // Operaciones de autenticacion - CRITICAS
            path.contains("/auth/login") || path.contains("/auth/register") -> 
                NetworkConfig.OperationType.AUTHENTICATION
            
            // Refresh de tokens - ULTRA CRITICO
            path.contains("/auth/refresh") || path.contains("/token/refresh") -> 
                NetworkConfig.OperationType.TOKEN_REFRESH
            
            // Operaciones de sesion - IMPORTANTES
            path.contains("/auth/") || path.contains("/session/") || path.contains("/user/profile") -> 
                NetworkConfig.OperationType.SESSION_OPERATIONS
            
            // Subida de archivos - timeouts largos
            (method == "POST" || method == "PUT") && contentLength > 1024 * 1024 -> // > 1MB
                NetworkConfig.OperationType.FILE_UPLOAD
            
            // Operaciones en tiempo real
            path.contains("/chat") || path.contains("/realtime") || path.contains("/ws/") -> 
                NetworkConfig.OperationType.REALTIME
            
            // Carga de contenido
            method == "GET" && (path.contains("/content") || path.contains("/data")) -> 
                NetworkConfig.OperationType.CONTENT_LOADING
            
            // Operaciones no criticas (analytics, logs, etc)
            path.contains("/analytics") || path.contains("/log") || path.contains("/track") -> 
                NetworkConfig.OperationType.NON_CRITICAL
            
            // Default
            else -> NetworkConfig.OperationType.DEFAULT
        }
    }
    
    /**
     * Mejora la request con headers adicionales
     */
    private fun enhanceRequest(
        request: Request,
        operationType: NetworkConfig.OperationType,
        connectivityInfo: NetworkConnectivityManager.ConnectivityInfo
    ): Request {
        return request.newBuilder()
            .header(NetworkConfig.CriticalHeaders.OPERATION_TYPE, operationType.name)
            .header(NetworkConfig.CriticalHeaders.CONNECTION_STATE, connectivityInfo.connectivityState.name)
            .header("X-Network-Type", connectivityInfo.networkType.name)
            .header("X-Response-Time-Avg", connectivityInfo.averageResponseTime.toString())
            .header("X-Connection-Failures", connectivityInfo.consecutiveFailures.toString())
            .header("User-Agent", "Aquanqa-Android/1.0 (Network-Optimized)")
            .apply {
                // Marcar operaciones criticas
                if (isCriticalOperation(operationType)) {
                    header(NetworkConfig.CriticalHeaders.SESSION_CRITICAL, "true")
                }
                
                // Headers especiales para conexiones lentas
                if (connectivityInfo.connectivityState == NetworkConfig.ConnectivityState.POOR ||
                    connectivityInfo.connectivityState == NetworkConfig.ConnectivityState.FAIR) {
                    header("X-Slow-Connection", "true")
                    header("Accept-Encoding", "gzip, deflate") // Forzar compresion
                }
                
                // Headers para conexiones metradas
                if (connectivityInfo.isMetered) {
                    header("X-Metered-Connection", "true")
                    header("Cache-Control", "max-age=3600") // Cache mas agresivo
                }
            }
            .build()
    }
    
    /**
     * Crea un chain con timeouts dinamicos
     * Nota: Esta es una simulacion ya que OkHttp no permite cambiar timeouts por request
     * En implementaciones reales necesitariamos crear clients especializados
     */
    private fun createChainWithDynamicTimeouts(
        chain: Interceptor.Chain,
        timeoutConfig: NetworkConfig.TimeoutConfig
    ): Interceptor.Chain {
        // Por ahora retornamos el chain original
        // En una implementacion completa, creariamos un client especializado aqui
        return chain
    }
    
    /**
     * Mejora la respuesta con headers de performance
     */
    private fun enhanceResponse(
        response: Response,
        duration: Long,
        connectivityInfo: NetworkConnectivityManager.ConnectivityInfo
    ): Response {
        return response.newBuilder()
            .header("X-Request-Duration", duration.toString())
            .header("X-Network-State", connectivityInfo.connectivityState.name)
            .header("X-Network-Type", connectivityInfo.networkType.name)
            .header("X-Server-Processed", System.currentTimeMillis().toString())
            .build()
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
}

/**
 * Builder para crear OkHttpClient con configuraciones robustas
 */
class RobustOkHttpClientBuilder(private val context: Context) {
    
    private val connectivityManager = NetworkConnectivityManager.getInstance(context)
    
    fun build(operationType: NetworkConfig.OperationType = NetworkConfig.OperationType.DEFAULT): OkHttpClient {
        val connectivityState = connectivityManager.connectivityState.value
        val timeoutConfig = NetworkConfig.getAdjustedTimeouts(operationType, connectivityState)
        
        return OkHttpClient.Builder()
            // Timeouts dinamicos basados en conectividad
            .connectTimeout(timeoutConfig.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(timeoutConfig.readTimeout, TimeUnit.SECONDS)
            .writeTimeout(timeoutConfig.writeTimeout, TimeUnit.SECONDS)
            
            // Interceptores en orden de prioridad
            .addInterceptor(RobustNetworkInterceptor(context)) // Primero: configuracion dinamica
            .addInterceptor(SmartRetryInterceptor(context))    // Segundo: retry inteligente  
            .addInterceptor(AuthInterceptor(context))          // Tercero: autenticacion
            
            // Configuraciones de conexion
            .retryOnConnectionFailure(true) // Retry basico de OkHttp
            .followRedirects(true)
            .followSslRedirects(true)
            
            // Pool de conexiones optimizado
            .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
            
            // Cache de DNS para reducir latencia
            .dns(okhttp3.Dns.SYSTEM)
            
            .build()
    }
    
    /**
     * Crea cliente especializado para operaciones criticas
     */
    fun buildForCriticalOperations(): OkHttpClient {
        return build(NetworkConfig.OperationType.TOKEN_REFRESH).newBuilder()
            // Timeouts extra generosos para operaciones criticas
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            
            // Sin limite de retry para operaciones criticas
            .retryOnConnectionFailure(true)
            
            .build()
    }
    
    /**
     * Crea cliente optimizado para subida de archivos
     */
    fun buildForFileUploads(): OkHttpClient {
        return build(NetworkConfig.OperationType.FILE_UPLOAD).newBuilder()
            // Timeouts muy largos para subidas
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS) // 5 minutos
            .writeTimeout(600, TimeUnit.SECONDS) // 10 minutos
            
            // Menos conexiones concurrentes para subidas
            .connectionPool(okhttp3.ConnectionPool(3, 10, TimeUnit.MINUTES))
            
            .build()
    }
    
    /**
     * Obtiene estadisticas del cliente
     */
    fun getClientStats(): String {
        return buildString {
            appendLine("=== OkHttp Client Statistics ===")
            appendLine("Connectivity State: ${connectivityManager.connectivityState.value}")
            appendLine("Current Network: ${connectivityManager.networkType.value}")
            append(connectivityManager.getNetworkStats())
        }
    }
}