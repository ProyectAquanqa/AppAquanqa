package com.tecsup.aquanqa.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tecsup.aquanqa.data.utils.JwtDecoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Clase para manejar el almacenamiento de tokens y datos de usuario
 * utilizando DataStore de Android.
 * 
 * Esta clase gestiona el almacenamiento persistente y seguro de:
 * - Tokens de autenticación (access y refresh)
 * - Información básica del usuario (DNI, email, nombre)
 * 
 * @param context Contexto de la aplicación para acceder al DataStore
 */
class UserPreferences(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")
        
        // Claves para las preferencias de autenticación
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val ACCESS_TOKEN_EXPIRATION = longPreferencesKey("access_token_expiration")
        private val REFRESH_TOKEN_EXPIRATION = longPreferencesKey("refresh_token_expiration")
        
        // Claves para los datos básicos del usuario
        private val USER_DNI = stringPreferencesKey("user_dni")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_FIRST_NAME = stringPreferencesKey("user_first_name")
        private val USER_LAST_NAME = stringPreferencesKey("user_last_name")
        private val USER_PHOTO_URL = stringPreferencesKey("user_photo_url")
        
        // Clave para el token FCM
        private val FCM_TOKEN = stringPreferencesKey("fcm_token")
    }


    /**
     * Flow que emite el token de acceso actual del usuario.
     * retorna Flow<String?> Token de acceso o null si no existe
     */
    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN]
    }

    /**
     * Flow que emite el token de refresco actual del usuario.
     * retorna Flow<String?> Token de refresco o null si no existe
     */
    val refreshToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN]
    }

    /**
     * Flow que emite el timestamp de expiración del token de acceso.
     * retorna Flow<Long?> Timestamp de expiración en segundos o null si no existe
     */
    val accessTokenExpiration: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN_EXPIRATION]
    }

    /**
     * Flow que emite el timestamp de expiración del token de refresco.
     * retorna Flow<Long?> Timestamp de expiración en segundos o null si no existe
     */
    val refreshTokenExpiration: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN_EXPIRATION]
    }

    // ================= DATOS DEL USUARIO =================

    /**
     * Flow que emite el DNI del usuario autenticado.
     * retorna Flow<String?> DNI del usuario o null si no existe
     */
    val userDni: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_DNI]
    }

    /**
     * Flow que emite el email del usuario autenticado.
     * retorna Flow<String?> Email del usuario o null si no existe
     */
    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL]
    }

    /**
     * Flow que emite el primer nombre del usuario autenticado.
     * retorna Flow<String?> Primer nombre del usuario o null si no existe
     */
    val userFirstName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_FIRST_NAME]
    }

    /**
     * Flow que emite el apellido del usuario autenticado.
     * retorna Flow<String?> Apellido del usuario o null si no existe
     */
    val userLastName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_NAME]
    }

    /**
     * Flow que emite la URL de la foto de perfil del usuario.
     * retorna Flow<String?> URL de la foto de perfil o null si no existe
     */
    val userPhotoUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_PHOTO_URL]
    }

    /**
     * Flow que emite el token FCM del dispositivo.
     * retorna Flow<String?> Token FCM o null si no existe
     */
    val fcmToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[FCM_TOKEN]
    }

    // metodos de guardado

    /**
     * Guarda los tokens de autenticación en el almacenamiento persistente.
     * También extrae y almacena automáticamente los timestamps de expiración de los tokens JWT.
     * 
     * @param accessToken Token de acceso JWT
     * @param refreshToken Token de refresco JWT
     */
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = accessToken
            preferences[REFRESH_TOKEN] = refreshToken
            
            // Extraer y guardar timestamps de expiración
            JwtDecoder.decodeToken(accessToken)?.let { tokenInfo ->
                preferences[ACCESS_TOKEN_EXPIRATION] = tokenInfo.expirationTime
            }
            
            JwtDecoder.decodeToken(refreshToken)?.let { tokenInfo ->
                preferences[REFRESH_TOKEN_EXPIRATION] = tokenInfo.expirationTime
            }
        }
    }

    /**
     * Guarda el DNI del usuario en el almacenamiento persistente.
     */
    suspend fun saveUserDni(dni: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_DNI] = dni
        }
    }

    /**
     * Guarda el email del usuario en el almacenamiento persistente.
     */
    suspend fun saveUserEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL] = email
        }
    }

    //Guarda los datos básicos del usuario obtenidos del perfil.

    suspend fun saveUserProfile(
        firstName: String,
        lastName: String,
        photoUrl: String? = null
    ) {
        context.dataStore.edit { preferences ->
            preferences[USER_FIRST_NAME] = firstName
            preferences[USER_LAST_NAME] = lastName
            photoUrl?.let { 
                preferences[USER_PHOTO_URL] = it
            }
        }
    }

    //Actualiza solo la foto de perfil del usuario.

    suspend fun updateUserPhoto(photoUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_PHOTO_URL] = photoUrl
        }
    }

    //Guarda el token FCM del dispositivo.

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[FCM_TOKEN] = token
        }
    }

    // utilidades
    /**
     * Verifica si el usuario tiene una sesión activa válida.
     * retorna Flow<Boolean> true si tiene tokens guardados, false en caso contrario
     */
    val isUserLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN] != null && preferences[REFRESH_TOKEN] != null
    }

    /**
     * Limpia todos los datos almacenados (logout completo).
     * Esto incluye tokens y datos del usuario.
     */
    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Limpia solo los tokens de autenticación, manteniendo datos del usuario.
     * Útil para logout parcial o cuando los tokens expiran.
     */
    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
            preferences.remove(ACCESS_TOKEN_EXPIRATION)
            preferences.remove(REFRESH_TOKEN_EXPIRATION)
        }
    }

    /**
     * Verifica si el token de acceso está expirado o próximo a expirar
     * 
     * @param bufferSeconds Segundos de buffer antes de considerar expirado (default: 30s)
     * @return true si el token está expirado, inválido o próximo a expirar
     */
    suspend fun isAccessTokenExpired(bufferSeconds: Long = 30L): Boolean {
        return try {
            val token = accessToken.first()
            JwtDecoder.isTokenExpired(token, bufferSeconds)
        } catch (e: Exception) {
            true // Si hay error, considerar expirado
        }
    }

    /**
     * Verifica si el token de refresco está expirado
     * 
     * @param bufferSeconds Segundos de buffer antes de considerar expirado (default: 30s)
     * @return true si el token está expirado, inválido o próximo a expirar
     */
    suspend fun isRefreshTokenExpired(bufferSeconds: Long = 30L): Boolean {
        return try {
            val token = refreshToken.first()
            JwtDecoder.isTokenExpired(token, bufferSeconds)
        } catch (e: Exception) {
            true // Si hay error, considerar expirado
        }
    }

    /**
     * Verifica si el token de acceso necesita ser refrescado pronto
     * 
     * @param thresholdSeconds Umbral en segundos (default: 5 minutos)
     * @return true si el token expira en menos del threshold especificado
     */
    suspend fun shouldRefreshAccessToken(thresholdSeconds: Long = 300L): Boolean {
        return try {
            val token = accessToken.first()
            JwtDecoder.shouldRefreshToken(token, thresholdSeconds)
        } catch (e: Exception) {
            true // Si hay error, considerar que necesita refresh
        }
    }

    //Obtiene el token FCM actual del dispositivo, null si no existe

    suspend fun getFcmToken(): String? {
        return fcmToken.first()
    }
} 