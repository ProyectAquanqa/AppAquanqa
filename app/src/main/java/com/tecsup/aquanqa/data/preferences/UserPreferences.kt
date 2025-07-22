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
 */
class UserPreferences(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")
        
        // Claves para las preferencias
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val USER_DNI = stringPreferencesKey("user_dni")
    }

    // Obtener el token de acceso
    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN]
    }

    // Obtener el token de refresco
    val refreshToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN]
    }

    // Obtener el DNI del usuario
    val userDni: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_DNI]
    }

    // Guardar tokens de autenticación
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = accessToken
            preferences[REFRESH_TOKEN] = refreshToken
        }
    }

    // Guardar DNI del usuario
    suspend fun saveUserDni(dni: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_DNI] = dni
        }
    }

    // Limpiar todos los datos (logout)
    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
} 