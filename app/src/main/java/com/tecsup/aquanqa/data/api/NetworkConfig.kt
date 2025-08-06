package com.tecsup.aquanqa.data.api

import java.util.concurrent.TimeUnit

/**
 * Configuraciones de red diferenciadas por tipo de operacion
 * para optimizar timeouts segun la criticidad y naturaleza de cada request
 */
object NetworkConfig {
    
    /**
     * Configuracion de timeouts por tipo de operacion
     */
    enum class OperationType(
        val connectTimeoutSeconds: Long,
        val readTimeoutSeconds: Long,
        val writeTimeoutSeconds: Long,
        val maxRetries: Int,
        val retryDelayMs: Long
    ) {
        // Operaciones criticas de autenticacion - timeouts MUY generosos
        AUTHENTICATION(
            connectTimeoutSeconds = 30L,
            readTimeoutSeconds = 45L, 
            writeTimeoutSeconds = 60L,
            maxRetries = 5,
            retryDelayMs = 2000L
        ),
        
        // Refresh de tokens - ultra critico, no debe fallar
        TOKEN_REFRESH(
            connectTimeoutSeconds = 45L,
            readTimeoutSeconds = 60L,
            writeTimeoutSeconds = 60L, 
            maxRetries = 8,
            retryDelayMs = 1500L
        ),
        
        // Operaciones de sesion - importantes pero no criticas
        SESSION_OPERATIONS(
            connectTimeoutSeconds = 25L,
            readTimeoutSeconds = 35L,
            writeTimeoutSeconds = 45L,
            maxRetries = 4,
            retryDelayMs = 1000L
        ),
        
        // Carga de contenido principal - balanceado
        CONTENT_LOADING(
            connectTimeoutSeconds = 20L,
            readTimeoutSeconds = 30L,
            writeTimeoutSeconds = 40L,
            maxRetries = 3,
            retryDelayMs = 800L
        ),
        
        // Subida de archivos - mas tiempo para write
        FILE_UPLOAD(
            connectTimeoutSeconds = 25L,
            readTimeoutSeconds = 40L,
            writeTimeoutSeconds = 120L, // 2 minutos para subidas
            maxRetries = 3,
            retryDelayMs = 2000L
        ),
        
        // Operaciones en tiempo real (chat, notificaciones)
        REALTIME(
            connectTimeoutSeconds = 15L,
            readTimeoutSeconds = 25L,
            writeTimeoutSeconds = 30L,
            maxRetries = 2,
            retryDelayMs = 500L
        ),
        
        // Operaciones no criticas - timeouts cortos
        NON_CRITICAL(
            connectTimeoutSeconds = 10L,
            readTimeoutSeconds = 15L,
            writeTimeoutSeconds = 20L,
            maxRetries = 2,
            retryDelayMs = 1000L
        ),
        
        // Default para operaciones no especificadas
        DEFAULT(
            connectTimeoutSeconds = 20L,
            readTimeoutSeconds = 30L,
            writeTimeoutSeconds = 40L,
            maxRetries = 3,
            retryDelayMs = 1000L
        )
    }
    
    /**
     * Configuraciones especiales para diferentes estados de conectividad
     */
    enum class ConnectivityState(
        val timeoutMultiplier: Float,
        val retryMultiplier: Int,
        val maxConcurrentRequests: Int
    ) {
        // Conexion excelente (WiFi fuerte, 4G/5G)
        EXCELLENT(
            timeoutMultiplier = 1.0f,
            retryMultiplier = 1,
            maxConcurrentRequests = 10
        ),
        
        // Conexion buena (WiFi normal, 4G estable)
        GOOD(
            timeoutMultiplier = 1.2f,
            retryMultiplier = 1,
            maxConcurrentRequests = 8
        ),
        
        // Conexion regular (3G, WiFi lento)
        FAIR(
            timeoutMultiplier = 1.8f,
            retryMultiplier = 2,
            maxConcurrentRequests = 5
        ),
        
        // Conexion pobre (2G, WiFi muy lento)
        POOR(
            timeoutMultiplier = 3.0f,
            retryMultiplier = 3,
            maxConcurrentRequests = 2
        ),
        
        // Sin conexion detectada
        NO_CONNECTION(
            timeoutMultiplier = 1.0f,
            retryMultiplier = 0, // No retry si no hay conexion
            maxConcurrentRequests = 0
        )
    }
    
    /**
     * Tipos de errores de red y sus estrategias de manejo
     */
    enum class NetworkErrorType(
        val shouldRetry: Boolean,
        val retryAfterSeconds: Long,
        val exponentialBackoff: Boolean
    ) {
        // Timeout - siempre retry con backoff
        TIMEOUT(
            shouldRetry = true,
            retryAfterSeconds = 2L,
            exponentialBackoff = true
        ),
        
        // Sin conexion - retry con delay largo
        NO_CONNECTION(
            shouldRetry = true,
            retryAfterSeconds = 5L,
            exponentialBackoff = true
        ),
        
        // DNS error - retry con delay medio
        DNS_ERROR(
            shouldRetry = true,
            retryAfterSeconds = 3L,
            exponentialBackoff = true
        ),
        
        // Error 5xx del servidor - retry agresivo
        SERVER_ERROR_5XX(
            shouldRetry = true,
            retryAfterSeconds = 1L,
            exponentialBackoff = true
        ),
        
        // Error 429 (too many requests) - retry con delay largo
        RATE_LIMITED(
            shouldRetry = true,
            retryAfterSeconds = 10L,
            exponentialBackoff = false
        ),
        
        // Error 4xx client - generalmente no retry
        CLIENT_ERROR_4XX(
            shouldRetry = false,
            retryAfterSeconds = 0L,
            exponentialBackoff = false
        ),
        
        // Error 401/403 - no retry (maneja AuthInterceptor)
        AUTHENTICATION_ERROR(
            shouldRetry = false,
            retryAfterSeconds = 0L,
            exponentialBackoff = false
        ),
        
        // SSL/TLS error - retry limitado
        SSL_ERROR(
            shouldRetry = true,
            retryAfterSeconds = 5L,
            exponentialBackoff = false
        )
    }
    
    /**
     * Calcula timeouts ajustados segun el estado de conectividad
     */
    fun getAdjustedTimeouts(
        operationType: OperationType,
        connectivityState: ConnectivityState
    ): TimeoutConfig {
        val multiplier = connectivityState.timeoutMultiplier
        
        return TimeoutConfig(
            connectTimeout = (operationType.connectTimeoutSeconds * multiplier).toLong(),
            readTimeout = (operationType.readTimeoutSeconds * multiplier).toLong(),
            writeTimeout = (operationType.writeTimeoutSeconds * multiplier).toLong(),
            maxRetries = operationType.maxRetries * connectivityState.retryMultiplier,
            retryDelay = operationType.retryDelayMs
        )
    }
    
    /**
     * Configuracion de timeout calculada
     */
    data class TimeoutConfig(
        val connectTimeout: Long,
        val readTimeout: Long, 
        val writeTimeout: Long,
        val maxRetries: Int,
        val retryDelay: Long
    )
    
    /**
     * Headers especiales para operaciones criticas
     */
    object CriticalHeaders {
        const val OPERATION_TYPE = "X-Operation-Type"
        const val RETRY_COUNT = "X-Retry-Count"
        const val CONNECTION_STATE = "X-Connection-State"
        const val SESSION_CRITICAL = "X-Session-Critical"
    }
    
    /**
     * Configuraciones especiales para mantener sesion
     */
    object SessionMaintenance {
        // Timeouts extra largos para operaciones que mantienen sesion viva
        const val HEARTBEAT_TIMEOUT_SECONDS = 60L
        
        // Intervalo para verificacion de conectividad
        const val CONNECTIVITY_CHECK_INTERVAL_MS = 30000L // 30 segundos
        
        // Tiempo maximo sin conectividad antes de modo offline
        const val MAX_OFFLINE_TIME_MS = 300000L // 5 minutos
        
        // Operaciones que NUNCA deben fallar por timeout
        val CRITICAL_ENDPOINTS = setOf(
            "/auth/refresh",
            "/auth/login", 
            "/auth/verify",
            "/user/heartbeat",
            "/session/maintain"
        )
    }
}