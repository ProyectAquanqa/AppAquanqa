package com.tecsup.aquanqa.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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
        
        // Claves para los datos básicos del usuario
        private val USER_DNI = stringPreferencesKey("user_dni")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_FIRST_NAME = stringPreferencesKey("user_first_name")
        private val USER_LAST_NAME = stringPreferencesKey("user_last_name")
        private val USER_PHOTO_URL = stringPreferencesKey("user_photo_url")
    }

    // ================= TOKENS DE AUTENTICACIÓN =================

    /**
     * Flow que emite el token de acceso actual del usuario.
     * @return Flow<String?> Token de acceso o null si no existe
     */
    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN]
    }

    /**
     * Flow que emite el token de refresco actual del usuario.
     * @return Flow<String?> Token de refresco o null si no existe
     */
    val refreshToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN]
    }

    // ================= DATOS DEL USUARIO =================

    /**
     * Flow que emite el DNI del usuario autenticado.
     * @return Flow<String?> DNI del usuario o null si no existe
     */
    val userDni: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_DNI]
    }

    /**
     * Flow que emite el email del usuario autenticado.
     * @return Flow<String?> Email del usuario o null si no existe
     */
    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL]
    }

    /**
     * Flow que emite el primer nombre del usuario autenticado.
     * @return Flow<String?> Primer nombre del usuario o null si no existe
     */
    val userFirstName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_FIRST_NAME]
    }

    /**
     * Flow que emite el apellido del usuario autenticado.
     * @return Flow<String?> Apellido del usuario o null si no existe
     */
    val userLastName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_LAST_NAME]
    }

    /**
     * Flow que emite la URL de la foto de perfil del usuario.
     * @return Flow<String?> URL de la foto de perfil o null si no existe
     */
    val userPhotoUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_PHOTO_URL]
    }

    // ================= MÉTODOS DE GUARDADO =================

    /**
     * Guarda los tokens de autenticación en el almacenamiento persistente.
     * @param accessToken Token de acceso JWT
     * @param refreshToken Token de refresco JWT
     */
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = accessToken
            preferences[REFRESH_TOKEN] = refreshToken
        }
    }

    /**
     * Guarda el DNI del usuario en el almacenamiento persistente.
     * @param dni DNI del usuario
     */
    suspend fun saveUserDni(dni: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_DNI] = dni
        }
    }

    /**
     * Guarda el email del usuario en el almacenamiento persistente.
     * @param email Email del usuario
     */
    suspend fun saveUserEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL] = email
        }
    }

    /**
     * Guarda los datos básicos del usuario obtenidos del perfil.
     * @param firstName Primer nombre del usuario
     * @param lastName Apellido del usuario
     * @param photoUrl URL de la foto de perfil (opcional)
     */
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

    /**
     * Actualiza solo la foto de perfil del usuario.
     * @param photoUrl Nueva URL de la foto de perfil
     */
    suspend fun updateUserPhoto(photoUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_PHOTO_URL] = photoUrl
        }
    }

    // ================= UTILIDADES =================

    /**
     * Verifica si el usuario tiene una sesión activa válida.
     * @return Flow<Boolean> true si tiene tokens guardados, false en caso contrario
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
        }
    }
} 