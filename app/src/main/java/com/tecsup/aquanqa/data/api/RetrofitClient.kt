package com.tecsup.aquanqa.data.api

import android.content.Context
import android.util.Log
import com.tecsup.aquanqa.data.preferences.UserPreferences
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * RetrofitClient mejorado con reconexion inteligente y manejo robusto de red
 */
class RetrofitClient(private val context: Context) {

    private val userPreferences = UserPreferences(context)
    private val connectivityManager = NetworkConnectivityManager.getInstance(context)
    private val clientBuilder = RobustOkHttpClientBuilder(context)
    
    companion object {
        private const val TAG = "RetrofitClient"
        private const val CACHE_SIZE = 50 * 1024 * 1024L // 50MB cache
    }

    // Cache para modo offline
    private val cache = Cache(
        directory = File(context.cacheDir, "http_cache"),
        maxSize = CACHE_SIZE
    )

    // Configurar interceptor de logging
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (android.util.Log.isLoggable(TAG, Log.DEBUG)) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.BASIC
        }
    }

    // Cliente HTTP principal con todas las mejoras
    private val defaultOkHttpClient = clientBuilder.build().newBuilder()
        .cache(cache)
        .addNetworkInterceptor(loggingInterceptor) // Network interceptor para cache
        .build()

    // Cliente especializado para operaciones criticas
    private val criticalOkHttpClient = clientBuilder.buildForCriticalOperations().newBuilder()
        .cache(cache)
        .addNetworkInterceptor(loggingInterceptor)
        .build()

    // Cliente especializado para subida de archivos
    private val uploadOkHttpClient = clientBuilder.buildForFileUploads().newBuilder()
        .cache(cache)
        .addNetworkInterceptor(loggingInterceptor)
        .build()

    // Cliente Retrofit principal
    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(defaultOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // Cliente Retrofit para operaciones criticas
    private val criticalRetrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)  
        .client(criticalOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // Cliente Retrofit para subidas
    private val uploadRetrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(uploadOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // Servicios API con diferentes configuraciones
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val criticalApiService: ApiService by lazy {
        criticalRetrofit.create(ApiService::class.java)
    }

    val uploadApiService: ApiService by lazy {
        uploadRetrofit.create(ApiService::class.java)
    }

    val chatbotApiService: ChatbotApiService by lazy {
        retrofit.create(ChatbotApiService::class.java)
    }
    
    /**
     * Obtiene el servicio API apropiado segun el tipo de operacion
     */
    fun getApiService(operationType: NetworkConfig.OperationType = NetworkConfig.OperationType.DEFAULT): ApiService {
        return when (operationType) {
            NetworkConfig.OperationType.AUTHENTICATION,
            NetworkConfig.OperationType.TOKEN_REFRESH,
            NetworkConfig.OperationType.SESSION_OPERATIONS -> criticalApiService
            
            NetworkConfig.OperationType.FILE_UPLOAD -> uploadApiService
            
            else -> apiService
        }
    }
    
    /**
     * Verifica y reporta el estado de conectividad
     */
    fun checkConnectivity(): NetworkConnectivityManager.ConnectivityInfo {
        connectivityManager.checkConnectivityNow()
        return connectivityManager.getConnectivityInfo()
    }
    
    /**
     * Inicia monitoreo de conectividad
     */
    fun startConnectivityMonitoring() {
        Log.d(TAG, "Starting connectivity monitoring")
        connectivityManager.startMonitoring()
    }
    
    /**
     * Detiene monitoreo de conectividad
     */
    fun stopConnectivityMonitoring() {
        Log.d(TAG, "Stopping connectivity monitoring")
        connectivityManager.stopMonitoring()
    }
    
    /**
     * Limpia cache de red
     */
    fun clearNetworkCache() {
        try {
            cache.evictAll()
            Log.d(TAG, "Network cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing network cache", e)
        }
    }
    
    /**
     * Obtiene estadisticas de cache
     */
    fun getCacheStats(): String {
        return buildString {
            appendLine("=== Network Cache Stats ===")
            appendLine("Cache Directory: ${cache.directory}")
            appendLine("Cache Size: ${cache.size()} bytes")
            appendLine("Cache Max Size: ${cache.maxSize()} bytes")
            appendLine("Cache Hit Count: ${cache.hitCount()}")
            appendLine("Cache Request Count: ${cache.requestCount()}")
            appendLine("Cache Write Success Count: ${cache.writeSuccessCount()}")
            appendLine("Cache Write Abort Count: ${cache.writeAbortCount()}")
        }
    }
    
    /**
     * Obtiene estadisticas completas de red
     */
    fun getNetworkStats(): String {
        return buildString {
            appendLine("=== RetrofitClient Network Stats ===")
            append(connectivityManager.getNetworkStats())
            appendLine()
            append(getCacheStats())
            appendLine()
            append(clientBuilder.getClientStats())
        }
    }
    
    /**
     * Fuerza una verificacion de salud de conexion
     */
    suspend fun performHealthCheck(): Boolean {
        return try {
            Log.d(TAG, "Performing network health check")
            val connectivityInfo = checkConnectivity()
            
            if (!connectivityInfo.isConnected) {
                Log.w(TAG, "Health check failed: no connectivity")
                return false
            }
            
            // Esperar por conectividad si es necesario
            if (connectivityInfo.connectivityState == NetworkConfig.ConnectivityState.NO_CONNECTION) {
                Log.d(TAG, "Waiting for connectivity improvement...")
                connectivityManager.waitForConnectivity(10000L)
            }
            
            val finalConnectivity = connectivityManager.getConnectivityInfo()
            val isHealthy = finalConnectivity.isConnected && 
                           finalConnectivity.connectivityState != NetworkConfig.ConnectivityState.NO_CONNECTION
            
            Log.i(TAG, "Health check result: $isHealthy (state: ${finalConnectivity.connectivityState})")
            isHealthy
            
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed with exception", e)
            false
        }
    }
} 