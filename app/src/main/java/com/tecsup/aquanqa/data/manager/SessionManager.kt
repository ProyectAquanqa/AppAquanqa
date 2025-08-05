package com.tecsup.aquanqa.data

import android.content.Context
import android.util.Log
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.model.auth.RefreshTokenRequest
import com.tecsup.aquanqa.data.model.auth.RefreshTokenResponse
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.tecsup.aquanqa.data.utils.JwtDecoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.Response

/**
 * SessionManager para centralizar la lógica de autenticación y manejo de tokens
 */
class SessionManager(
    private val context: Context,
    private val userPreferences: UserPreferences
) {
    
    companion object {
        private const val TAG = "SessionManager"
    }

    /**
     * Verifica si el usuario tiene una sesión activa válida.
     * Considera tanto la presencia de tokens como su estado de expiración.
     * 
     * @return true si la sesión es activa y los tokens son válidos
     */
    suspend fun isSessionActive(): Boolean {
        return try {
            val isLoggedIn = userPreferences.isUserLoggedIn.first()
            val accessToken = userPreferences.accessToken.first()
            val refreshToken = userPreferences.refreshToken.first()
            
            // Verificar que el usuario esté logueado y tenga tokens
            if (!isLoggedIn || accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
                Log.d(TAG, "Session inactive: missing login status or tokens")
                return false
            }
            
            // Verificar que el refresh token no esté expirado
            // Si está expirado, la sesión no es válida
            if (userPreferences.isRefreshTokenExpired()) {
                Log.d(TAG, "Session inactive: refresh token expired")
                clearSession()
                return false
            }
            
            // La sesión es activa si tenemos tokens y el refresh token es válido
            // No importa si el access token está expirado, se puede refrescar
            Log.d(TAG, "Session is active")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking session status", e)
            false
        }
    }

    /**
     * Obtiene un token de acceso válido, manejando automáticamente la renovación
     * si el token actual está expirado o próximo a expirar.
     * 
     * @return Token de acceso válido o null si no se puede obtener
     */
    suspend fun getValidAccessToken(): String? {
        return try {
            val accessToken = userPreferences.accessToken.first()
            
            if (accessToken.isNullOrEmpty()) {
                Log.w(TAG, "No access token available, attempting refresh")
                return refreshAccessToken()
            }
            
            // Validar si el token está expirado o próximo a expirar
            if (userPreferences.isAccessTokenExpired()) {
                Log.d(TAG, "Access token is expired, attempting refresh")
                return refreshAccessToken()
            }
            
            // Verificar si necesita refresh preventivo (antes de que expire)
            if (userPreferences.shouldRefreshAccessToken()) {
                Log.d(TAG, "Access token will expire soon, proactive refresh")
                val newToken = refreshAccessToken()
                // Si el refresh falla, usar el token actual (aún válido)
                return newToken ?: accessToken
            }
            
            // Token es válido y no necesita refresh
            Log.d(TAG, "Access token is valid")
            accessToken
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting valid access token", e)
            null
        }
    }

    /**
     * Refresca el token de acceso usando el refresh token.
     * Valida que el refresh token no esté expirado antes de intentar usarlo.
     * 
     * @return Nuevo token de acceso o null si no se puede refrescar
     */
    suspend fun refreshAccessToken(): String? {
        return try {
            val refreshToken = userPreferences.refreshToken.first()
            
            if (refreshToken.isNullOrEmpty()) {
                Log.w(TAG, "No refresh token available")
                clearSession()
                return null
            }
            
            // Validar que el refresh token no esté expirado
            if (userPreferences.isRefreshTokenExpired()) {
                Log.w(TAG, "Refresh token is expired, clearing session")
                clearSession()
                return null
            }

            Log.d(TAG, "Attempting to refresh access token")
            val refreshRequest = RefreshTokenRequest(refresh = refreshToken)
            val response = ApiClient.apiService.refreshToken(refreshRequest)
            
            if (response.isSuccessful) {
                val refreshResponse = response.body()
                refreshResponse?.access?.let { newAccessToken ->
                    // Guardar el nuevo token de acceso (manteniendo el mismo refresh token)
                    userPreferences.saveTokens(newAccessToken, refreshToken)
                    Log.d(TAG, "Access token refreshed successfully")
                    
                    // Debug: mostrar información del nuevo token
                    Log.d(TAG, JwtDecoder.getTokenDebugInfo(newAccessToken))
                    
                    return newAccessToken
                }
                Log.e(TAG, "Refresh response body is null or missing access token")
            } else {
                Log.e(TAG, "Token refresh failed: ${response.code()} - ${response.message()}")
                // Si el refresh falla, la sesión probablemente expiró
                if (response.code() == 401 || response.code() == 403) {
                    Log.w(TAG, "Refresh token rejected by server, clearing session")
                    clearSession()
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token", e)
            clearSession()
            null
        }
    }

    // Limpia completamente la sesión del usuario

    suspend fun clearSession() {
        try {
            userPreferences.clear()
            Log.d(TAG, "Session cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing session", e)
        }
    }

    // Verifica si el usuario está logueado (Flow reactivo)

    fun isUserLoggedInFlow(): Flow<Boolean> {
        return userPreferences.isUserLoggedIn
    }

    // Obtiene el DNI del usuario actual

    suspend fun getCurrentUserDni(): String? {
        return try {
            userPreferences.userDni.first()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user DNI", e)
            null
        }
    }

    // Obtiene el nombre del usuario actual

    suspend fun getCurrentUserName(): String? {
        return try {
            userPreferences.userFirstName.first()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user name", e)
            null
        }
    }
} 