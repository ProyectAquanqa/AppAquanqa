package com.tecsup.aquanqa.data.api

import android.content.Context
import android.util.Log
import com.tecsup.aquanqa.data.SessionManager
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Interceptor que automáticamente agrega el token de autenticación a las peticiones
 * y maneja la renovación automática de tokens cuando expiran
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    private val userPreferences = UserPreferences(context)
    private val sessionManager = SessionManager(context, userPreferences)
    
    companion object {
        private const val TAG = "AuthInterceptor"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Si la petición ya tiene Authorization header, no hace nada
        if (originalRequest.header(AUTHORIZATION_HEADER) != null) {
            return chain.proceed(originalRequest)
        }

        return runBlocking {
            try {
                // Obtener token válido (puede incluir refresh automático)
                val accessToken = sessionManager.getValidAccessToken()
                
                if (accessToken.isNullOrEmpty()) {
                    Log.w(TAG, "No access token available")
                    return@runBlocking chain.proceed(originalRequest)
                }

                // Crear nueva petición con el token
                val authenticatedRequest = originalRequest.newBuilder()
                    .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessToken")
                    .build()

                // Ejecutar la petición
                val response = chain.proceed(authenticatedRequest)

                // Si obtenemos 401, que se intente refrescar el token
                if (response.code == 401) {
                    Log.d(TAG, "Received 401, attempting token refresh")
                    response.close()
                    
                    val newAccessToken = sessionManager.refreshAccessToken()
                    
                    if (!newAccessToken.isNullOrEmpty()) {
                        Log.d(TAG, "Token refreshed successfully, retrying request")
                        
                        // Reintentar la petición con el nuevo token
                        val retryRequest = originalRequest.newBuilder()
                            .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$newAccessToken")
                            .build()
                        
                        return@runBlocking chain.proceed(retryRequest)
                    } else {
                        Log.e(TAG, "Token refresh failed, clearing session")
                        sessionManager.clearSession()
                    }
                }

                response
            } catch (e: Exception) {
                Log.e(TAG, "Error in auth interceptor", e)
                chain.proceed(originalRequest)
            }
        }
    }
} 