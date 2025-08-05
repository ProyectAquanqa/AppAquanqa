package com.tecsup.aquanqa.data.api

import android.content.Context
import android.util.Log
import com.tecsup.aquanqa.data.SessionManager
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Interceptor que automáticamente agrega el token de autenticación a las peticiones
 * y maneja la renovación automática de tokens cuando expiran.
 * 
 * Características:
 * - Validación proactiva de expiración de tokens
 * - Renovación automática antes de que expire el token
 * - Manejo robusto de errores 401/403
 * - Prevención de múltiples refreshes concurrentes
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    private val userPreferences = UserPreferences(context)
    private val sessionManager = SessionManager(context, userPreferences)
    
    companion object {
        private const val TAG = "AuthInterceptor"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        
        // Para prevenir múltiples refreshes concurrentes usando Mutex
        @Volatile
        private var isRefreshing = false
        private val refreshMutex = Mutex()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Si la petición ya tiene Authorization header, no modificarla
        if (originalRequest.header(AUTHORIZATION_HEADER) != null) {
            return chain.proceed(originalRequest)
        }

        // Si es una petición de refresh de token, no agregar auth header para evitar loops
        if (originalRequest.url.pathSegments.contains("auth") && 
            originalRequest.url.pathSegments.contains("refresh")) {
            return chain.proceed(originalRequest)
        }

        return runBlocking {
            try {
                // Obtener token válido (con validación proactiva incluida)
                val accessToken = getAccessTokenWithRefresh()
                
                if (accessToken.isNullOrEmpty()) {
                    Log.w(TAG, "No valid access token available, proceeding without auth")
                    return@runBlocking chain.proceed(originalRequest)
                }

                // Crear petición autenticada
                val authenticatedRequest = originalRequest.newBuilder()
                    .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessToken")
                    .build()

                // Ejecutar la petición
                val response = chain.proceed(authenticatedRequest)

                // Manejar respuestas de autenticación fallida
                when (response.code) {
                    401 -> {
                        Log.d(TAG, "Received 401 Unauthorized")
                        response.close()
                        return@runBlocking handleUnauthorizedResponse(chain, originalRequest)
                    }
                    403 -> {
                        Log.d(TAG, "Received 403 Forbidden - token may be invalid")
                        response.close()
                        return@runBlocking handleForbiddenResponse(chain, originalRequest)
                    }
                    else -> response
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in auth interceptor", e)
                chain.proceed(originalRequest)
            }
        }
    }

    /**
     * Obtiene un token de acceso válido, manejando refresh con concurrencia
     */
    private suspend fun getAccessTokenWithRefresh(): String? {
        // Si ya hay un refresh en progreso, esperar
        if (isRefreshing) {
            Log.d(TAG, "Token refresh already in progress, waiting...")
            refreshMutex.withLock {
                // Si aún está refrescando cuando obtenemos el lock, el token ya debería estar actualizado
                Log.d(TAG, "Acquired refresh lock, getting updated token")
            }
        }
        
        return sessionManager.getValidAccessToken()
    }

    /**
     * Maneja respuesta 401 (Unauthorized)
     */
    private suspend fun handleUnauthorizedResponse(
        chain: Interceptor.Chain, 
        originalRequest: Request
    ): Response {
        return refreshMutex.withLock {
            if (isRefreshing) {
                Log.d(TAG, "Token refresh already in progress for 401, waiting...")
                // Intentar con el token actualizado
                val newToken = sessionManager.getValidAccessToken()
                return@withLock if (!newToken.isNullOrEmpty()) {
                    val retryRequest = originalRequest.newBuilder()
                        .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$newToken")
                        .build()
                    chain.proceed(retryRequest)
                } else {
                    chain.proceed(originalRequest)
                }
            }
            
            isRefreshing = true
            
            try {
                Log.d(TAG, "Attempting token refresh due to 401")
                val newAccessToken = sessionManager.refreshAccessToken()
                
                if (!newAccessToken.isNullOrEmpty()) {
                    Log.d(TAG, "Token refreshed successfully, retrying request")
                    val retryRequest = originalRequest.newBuilder()
                        .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$newAccessToken")
                        .build()
                    chain.proceed(retryRequest)
                } else {
                    Log.e(TAG, "Token refresh failed after 401")
                    chain.proceed(originalRequest)
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    /**
     * Maneja respuesta 403 (Forbidden)
     */
    private suspend fun handleForbiddenResponse(
        chain: Interceptor.Chain, 
        originalRequest: Request
    ): Response {
        Log.w(TAG, "403 Forbidden - possible invalid token, attempting refresh")
        
        return refreshMutex.withLock {
            if (isRefreshing) {
                Log.d(TAG, "Token refresh already in progress for 403, skipping")
                return@withLock chain.proceed(originalRequest)
            }
            
            isRefreshing = true
            
            try {
                val newAccessToken = sessionManager.refreshAccessToken()
                if (!newAccessToken.isNullOrEmpty()) {
                    val retryRequest = originalRequest.newBuilder()
                        .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$newAccessToken")
                        .build()
                    chain.proceed(retryRequest)
                } else {
                    chain.proceed(originalRequest)
                }
            } finally {
                isRefreshing = false
            }
        }
    }
} 