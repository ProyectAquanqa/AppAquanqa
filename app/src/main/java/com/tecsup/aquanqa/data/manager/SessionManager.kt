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
     retorna true si la sesión es activa y los tokens son válidos
     */
    suspend fun isSessionActive(): Boolean {
        return try {
            val isLoggedIn = userPreferences.isUserLoggedIn.first()
            val accessToken = userPreferences.accessToken.first()
            val refreshToken = userPreferences.refreshToken.first()
            
            Log.v(TAG, "Checking session status - logged in: $isLoggedIn, has access token: ${!accessToken.isNullOrEmpty()}, has refresh token: ${!refreshToken.isNullOrEmpty()}")
            
            // Verificar que el usuario esté logueado y tenga tokens
            if (!isLoggedIn || accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
                Log.d(TAG, "Session inactive: missing login status or tokens (logged: $isLoggedIn, access: ${!accessToken.isNullOrEmpty()}, refresh: ${!refreshToken.isNullOrEmpty()})")
                return false
            }
            
            // Verificar estado de tokens con logging detallado
            val accessTokenExpired = userPreferences.isAccessTokenExpired()
            val refreshTokenExpired = userPreferences.isRefreshTokenExpired()
            
            Log.v(TAG, "Token status - access expired: $accessTokenExpired, refresh expired: $refreshTokenExpired")
            
            // Verificar que el refresh token no esté expirado
            // Si está expirado, la sesión no es válida
            if (refreshTokenExpired) {
                Log.w(TAG, "Session inactive: refresh token expired - clearing session")
                clearSession()
                return false
            }
            
            // La sesión es activa si tenemos tokens y el refresh token es válido
            // No importa si el access token está expirado, se puede refrescar
            Log.d(TAG, "Session is active (access token expired: $accessTokenExpired, but can be refreshed)")
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
                    // Determinar qué refresh token usar (nuevo si está disponible, o el anterior)
                    val newRefreshToken = refreshResponse.refresh ?: refreshToken
                    
                    // Guardar ambos tokens (access token nuevo y refresh token actualizado)
                    userPreferences.saveTokens(newAccessToken, newRefreshToken)
                    
                    if (refreshResponse.refresh != null) {
                        Log.d(TAG, "Access token and refresh token refreshed successfully (rotation enabled)")
                    } else {
                        Log.d(TAG, "Access token refreshed successfully (using existing refresh token)")
                    }
                    
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

    /**
     * Obtiene información completa de diagnóstico de la sesión
     * Útil para debuggear problemas de autenticación
     */
    suspend fun getSessionDiagnostics(): String {
        return try {
            val isLoggedIn = userPreferences.isUserLoggedIn.first()
            val accessToken = userPreferences.accessToken.first()
            val refreshToken = userPreferences.refreshToken.first()
            val accessTokenExpired = userPreferences.isAccessTokenExpired()
            val refreshTokenExpired = userPreferences.isRefreshTokenExpired()
            val shouldRefresh = userPreferences.shouldRefreshAccessToken()
            val userName = getCurrentUserName()
            val userDni = getCurrentUserDni()
            
            buildString {
                appendLine("=== SESSION DIAGNOSTICS ===")
                appendLine("User Logged In: $isLoggedIn")
                appendLine("User Name: $userName")
                appendLine("User DNI: $userDni")
                appendLine("Has Access Token: ${!accessToken.isNullOrEmpty()}")
                appendLine("Has Refresh Token: ${!refreshToken.isNullOrEmpty()}")
                appendLine("Access Token Expired: $accessTokenExpired")
                appendLine("Refresh Token Expired: $refreshTokenExpired")
                appendLine("Should Refresh Access Token: $shouldRefresh")
                appendLine("Session Active: ${isSessionActive()}")
                appendLine("")
                if (!accessToken.isNullOrEmpty()) {
                    appendLine("Access Token Info:")
                    appendLine(JwtDecoder.getTokenDebugInfo(accessToken))
                }
                if (!refreshToken.isNullOrEmpty()) {
                    appendLine("Refresh Token Info:")
                    appendLine(JwtDecoder.getTokenDebugInfo(refreshToken))
                }
            }
        } catch (e: Exception) {
            "Error generating session diagnostics: ${e.message}"
        }
    }
} 