package com.tecsup.aquanqa.data

import android.content.Context
import android.util.Log
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.model.auth.RefreshTokenRequest
import com.tecsup.aquanqa.data.model.auth.RefreshTokenResponse
import com.tecsup.aquanqa.data.preferences.UserPreferences
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

    //Verifica si el usuario tiene una sesión activa válida

    suspend fun isSessionActive(): Boolean {
        return try {
            val isLoggedIn = userPreferences.isUserLoggedIn.first()
            val accessToken = userPreferences.accessToken.first()
            val refreshToken = userPreferences.refreshToken.first()
            
            isLoggedIn && !accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking session status", e)
            false
        }
    }

    // Obtiene el token de acceso actual, intentando renovarlo si es necesario

    suspend fun getValidAccessToken(): String? {
        return try {
            val accessToken = userPreferences.accessToken.first()
            
            if (!accessToken.isNullOrEmpty()) {
                // TODO: Aquí se podría validar si el token está expirado
                // Por ahora retornamos el token actual
                accessToken
            } else {
                // Intentar refrescar token
                refreshAccessToken()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting valid access token", e)
            null
        }
    }

    // Refresca el token de acceso usando el refresh token

    suspend fun refreshAccessToken(): String? {
        return try {
            val refreshToken = userPreferences.refreshToken.first()
            
            if (refreshToken.isNullOrEmpty()) {
                Log.w(TAG, "No refresh token available")
                return null
            }

            val refreshRequest = RefreshTokenRequest(refresh = refreshToken)
            val response = ApiClient.apiService.refreshToken(refreshRequest)
            
            if (response.isSuccessful) {
                val refreshResponse = response.body()
                refreshResponse?.access?.let { newAccessToken ->
                    // Guardar el nuevo token de acceso
                    userPreferences.saveTokens(newAccessToken, refreshToken)
                    Log.d(TAG, "Access token refreshed successfully")
                    return newAccessToken
                }
            } else {
                Log.e(TAG, "Token refresh failed: ${response.code()} - ${response.message()}")
                // Si el refresh falla, la sesión probablemente expiró
                clearSession()
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