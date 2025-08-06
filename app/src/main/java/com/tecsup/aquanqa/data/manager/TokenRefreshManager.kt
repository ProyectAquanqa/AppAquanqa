package com.tecsup.aquanqa.data.manager

import android.content.Context
import android.util.Log
import com.tecsup.aquanqa.data.SessionManager
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager que se encarga del refresh automático y proactivo de tokens
 * para evitar que expiren durante el uso de la aplicación.
 * 
 * Características:
 * - Monitoreo periódico del estado de los tokens
 * - Refresh automático antes de que expiren los tokens
 * - Prevención de múltiples trabajos concurrentes
 * - Configuración flexible de intervalos de verificación
 */
class TokenRefreshManager private constructor(
    private val context: Context
) {
    
    private val userPreferences = UserPreferences(context)
    private val sessionManager = SessionManager(context, userPreferences)
    
    private var refreshJob: Job? = null
    private val isRunning = AtomicBoolean(false)
    
    companion object {
        private const val TAG = "TokenRefreshManager"
        
        // Intervalo de verificación en milisegundos (cada 5 minutos - menos agresivo)
        private const val CHECK_INTERVAL_MS = 5 * 60 * 1000L
        
        // Threshold para refresh automático (10 minutos antes de expirar)
        private const val REFRESH_THRESHOLD_SECONDS = 10 * 60L
        
        @Volatile
        private var INSTANCE: TokenRefreshManager? = null
        
        fun getInstance(context: Context): TokenRefreshManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenRefreshManager(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }
        }
    }
    
    /**
     * Inicia el monitoreo automático de tokens
     */
    fun startTokenMonitoring() {
        if (isRunning.compareAndSet(false, true)) {
            Log.d(TAG, "Starting token monitoring")
            
            refreshJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    while (isActive && isRunning.get()) {
                        try {
                            checkAndRefreshTokensIfNeeded()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during token check", e)
                        }
                        
                        // Esperar antes de la siguiente verificación
                        delay(CHECK_INTERVAL_MS)
                    }
                } catch (e: CancellationException) {
                    Log.d(TAG, "Token monitoring cancelled")
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error in token monitoring", e)
                } finally {
                    isRunning.set(false)
                    Log.d(TAG, "Token monitoring stopped")
                }
            }
        } else {
            Log.d(TAG, "Token monitoring already running")
        }
    }
    
    /**
     * Detiene el monitoreo automático de tokens
     */
    fun stopTokenMonitoring() {
        Log.d(TAG, "Stopping token monitoring")
        isRunning.set(false)
        refreshJob?.cancel()
        refreshJob = null
    }
    
    /**
     * Verifica si los tokens necesitan ser refrescados y los refresca si es necesario
     */
    private suspend fun checkAndRefreshTokensIfNeeded() {
        try {
            // Verificar si hay una sesión activa
            if (!sessionManager.isSessionActive()) {
                Log.d(TAG, "No active session, skipping token check")
                return
            }
            
            val accessToken = userPreferences.accessToken.first()
            if (accessToken.isNullOrEmpty()) {
                Log.d(TAG, "No access token available")
                return
            }
            
            // Verificar si el token necesita refresh proactivo
            if (userPreferences.shouldRefreshAccessToken(REFRESH_THRESHOLD_SECONDS)) {
                Log.d(TAG, "Access token will expire soon, starting proactive refresh")
                
                val newToken = sessionManager.refreshAccessToken()
                if (!newToken.isNullOrEmpty()) {
                    Log.d(TAG, "Proactive token refresh successful")
                } else {
                    Log.w(TAG, "Proactive token refresh failed")
                }
            } else {
                Log.v(TAG, "Access token is still valid, no refresh needed")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking token status", e)
        }
    }
    
    /**
     * Fuerza un refresh inmediato del token si es necesario
     * 
     * @return true si el refresh fue exitoso o no era necesario
     */
    suspend fun forceTokenRefreshIfNeeded(): Boolean {
        return try {
            if (!sessionManager.isSessionActive()) {
                Log.d(TAG, "No active session for forced refresh")
                return false
            }
            
            if (userPreferences.isAccessTokenExpired()) {
                Log.d(TAG, "Forcing token refresh due to expiration")
                val newToken = sessionManager.refreshAccessToken()
                !newToken.isNullOrEmpty()
            } else {
                Log.d(TAG, "Token is still valid, no forced refresh needed")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during forced token refresh", e)
            false
        }
    }
    
    /**
     * Obtiene información de debug sobre el estado actual de los tokens
     */
    suspend fun getTokenStatus(): String {
        return try {
            val accessToken = userPreferences.accessToken.first()
            val refreshToken = userPreferences.refreshToken.first()
            
            buildString {
                appendLine("=== Token Status ===")
                appendLine("Session Active: ${sessionManager.isSessionActive()}")
                appendLine("Has Access Token: ${!accessToken.isNullOrEmpty()}")
                appendLine("Has Refresh Token: ${!refreshToken.isNullOrEmpty()}")
                appendLine("Access Token Expired: ${userPreferences.isAccessTokenExpired()}")
                appendLine("Refresh Token Expired: ${userPreferences.isRefreshTokenExpired()}")
                appendLine("Should Refresh Access Token: ${userPreferences.shouldRefreshAccessToken()}")
                appendLine("Monitoring Active: ${isRunning.get()}")
            }
        } catch (e: Exception) {
            "Error getting token status: ${e.message}"
        }
    }
    
    /**
     * Limpia el estado interno del manager
     */
    fun cleanup() {
        stopTokenMonitoring()
        refreshJob = null
    }
}