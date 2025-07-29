package com.tecsup.aquanqa.data

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Gestor de Firebase para manejar tokens FCM y notificaciones
 */
class FirebaseManager(
    private val context: Context,
    private val userPreferences: UserPreferences,
    private val sessionManager: SessionManager
) {
    
    companion object {
        private const val TAG = "FirebaseManager"
    }

    /**
     * Inicializa Firebase y obtiene el token FCM
     */
    fun initializeFirebase() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (sessionManager.isSessionActive()) {
                    getAndRegisterFcmToken()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando Firebase", e)
            }
        }
    }

    /**
     * Obtiene el token FCM y lo registra en el servidor
     */
    suspend fun getAndRegisterFcmToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            userPreferences.saveFcmToken(token)
            registerTokenWithServer(token)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo token FCM", e)
        }
    }

    /**
     * Registra el token FCM en el servidor
     */
    suspend fun registerTokenWithServer(token: String) {
        try {
            val accessToken = sessionManager.getValidAccessToken()
            if (accessToken.isNullOrEmpty()) {
                Log.w(TAG, "No hay token de acceso válido para registrar FCM token")
                return
            }

            val requestBody = mapOf(
                "token" to token,
                "device_type" to "android"
            )

            val response = ApiClient.apiService.registerFcmToken(
                token = "Bearer $accessToken",
                fcmTokenRequest = requestBody
            )

            if (!response.isSuccessful) {
                Log.e(TAG, "Error registrando token FCM: ${response.code()}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error enviando token FCM al servidor", e)
        }
    }

    /**
     * Remueve el token FCM del servidor (para logout)
     */
    suspend fun unregisterTokenFromServer() {
        try {
            val fcmToken = userPreferences.getFcmToken()
            if (fcmToken.isNullOrEmpty()) return

            val accessToken = sessionManager.getValidAccessToken()
            if (accessToken.isNullOrEmpty()) return

            // Buscar el token en el servidor para desactivarlo
            try {
                val listResponse = ApiClient.apiService.getFcmTokens("Bearer $accessToken")
                
                if (listResponse.isSuccessful) {
                    val tokens = listResponse.body()
                    val tokenToDeactivate = tokens?.find { it.token == fcmToken }
                    
                    tokenToDeactivate?.let {
                        val updateBody = mapOf("is_active" to false)
                        ApiClient.apiService.updateFcmToken(
                            token = "Bearer $accessToken",
                            tokenId = it.id,
                            updateData = updateBody
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error desactivando token en servidor", e)
            }
            
            // Limpiar token local
            userPreferences.saveFcmToken("")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error removiendo token FCM", e)
        }
    }
} 