package com.tecsup.aquanqa.data.manager

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tecsup.aquanqa.data.SessionManager
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Manager especializado en la persistencia robusta del estado de sesion
 * para garantizar que sobreviva a kills de proceso y reinicios del sistema.
 * 
 * Funcionalidades:
 * - Guarda estado critico de sesion de forma atomica
 * - Detecta recuperacion tras kill de proceso
 * - Restaura sesion automaticamente
 * - Maneja integridad de datos de sesion
 * - Tracking de salud de sesion
 */
class SessionPersistenceManager private constructor(
    private val context: Context
) {
    
    private val userPreferences = UserPreferences(context)
    
    companion object {
        private const val TAG = "SessionPersistence"
        private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_persistence")
        
        // Claves para estado de sesion critico
        private val SESSION_ID = stringPreferencesKey("session_id")
        private val LAST_ACTIVE_TIMESTAMP = longPreferencesKey("last_active_timestamp")
        private val APP_VERSION = stringPreferencesKey("app_version")
        private val PROCESS_ID = stringPreferencesKey("process_id")
        private val SESSION_HEALTHY = booleanPreferencesKey("session_healthy")
        private val RECOVERY_COUNT = longPreferencesKey("recovery_count")
        private val LAST_RECOVERY_TIMESTAMP = longPreferencesKey("last_recovery_timestamp")
        
        // Estados de sesion
        private val BACKGROUND_TIMESTAMP = longPreferencesKey("background_timestamp")
        private val FOREGROUND_TIMESTAMP = longPreferencesKey("foreground_timestamp")
        private val KILL_RECOVERY_MODE = booleanPreferencesKey("kill_recovery_mode")
        
        @Volatile
        private var INSTANCE: SessionPersistenceManager? = null
        
        fun getInstance(context: Context): SessionPersistenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionPersistenceManager(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }
        }
    }
    
    /**
     * Informacion de estado de sesion
     */
    data class SessionState(
        val sessionId: String,
        val lastActiveTimestamp: Long,
        val appVersion: String,
        val processId: String,
        val isHealthy: Boolean,
        val recoveryCount: Long,
        val wasKilled: Boolean
    )
    
    /**
     * Inicializa el manager de persistencia y detecta recuperacion
     */
    suspend fun initializeSessionPersistence(): SessionState {
        val currentProcessId = generateProcessId()
        val currentTimestamp = System.currentTimeMillis()
        val appVersion = getAppVersion()
        
        val lastProcessId = getLastProcessId()
        val wasKilled = lastProcessId != null && lastProcessId != currentProcessId
        
        if (wasKilled) {
            Log.i(TAG, "Detectada recuperacion tras kill de proceso")
            handleProcessRecovery()
        } else {
            Log.d(TAG, "Inicio normal de aplicacion")
        }
        
        // Crear o actualizar sesion
        val sessionId = getOrCreateSessionId()
        
        val sessionState = SessionState(
            sessionId = sessionId,
            lastActiveTimestamp = currentTimestamp,
            appVersion = appVersion,
            processId = currentProcessId,
            isHealthy = true,
            recoveryCount = getRecoveryCount(),
            wasKilled = wasKilled
        )
        
        // Guardar estado actual
        saveSessionState(sessionState)
        
        return sessionState
    }
    
    /**
     * Guarda el estado critico de sesion de forma atomica
     */
    suspend fun saveSessionState(state: SessionState) {
        try {
            context.sessionDataStore.edit { preferences ->
                preferences[SESSION_ID] = state.sessionId
                preferences[LAST_ACTIVE_TIMESTAMP] = state.lastActiveTimestamp
                preferences[APP_VERSION] = state.appVersion
                preferences[PROCESS_ID] = state.processId
                preferences[SESSION_HEALTHY] = state.isHealthy
                preferences[RECOVERY_COUNT] = state.recoveryCount
                
                if (state.wasKilled) {
                    preferences[LAST_RECOVERY_TIMESTAMP] = System.currentTimeMillis()
                }
            }
            Log.v(TAG, "Estado de sesion guardado exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando estado de sesion", e)
        }
    }
    
    /**
     * Actualiza timestamp de actividad (llamar periodicamente)
     */
    suspend fun updateLastActive() {
        try {
            context.sessionDataStore.edit { preferences ->
                preferences[LAST_ACTIVE_TIMESTAMP] = System.currentTimeMillis()
                preferences[SESSION_HEALTHY] = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando actividad", e)
        }
    }
    
    /**
     * Marca la app como pasando a background
     */
    suspend fun markAppGoingToBackground() {
        try {
            context.sessionDataStore.edit { preferences ->
                preferences[BACKGROUND_TIMESTAMP] = System.currentTimeMillis()
                preferences[SESSION_HEALTHY] = true
            }
            Log.d(TAG, "App marcada como yendo a background")
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando background", e)
        }
    }
    
    /**
     * Marca la app como volviendo a foreground
     */
    suspend fun markAppComingToForeground(): Long {
        return try {
            val backgroundTime = getBackgroundTimestamp()
            val currentTime = System.currentTimeMillis()
            val timeInBackground = if (backgroundTime > 0) currentTime - backgroundTime else 0
            
            context.sessionDataStore.edit { preferences ->
                preferences[FOREGROUND_TIMESTAMP] = currentTime
                preferences[LAST_ACTIVE_TIMESTAMP] = currentTime
                preferences[SESSION_HEALTHY] = true
            }
            
            Log.d(TAG, "App volvio a foreground tras ${timeInBackground}ms en background")
            timeInBackground
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando foreground", e)
            0L
        }
    }
    
    /**
     * Verifica si la sesion necesita recuperacion
     */
    suspend fun needsSessionRecovery(): Boolean {
        return try {
            val sessionHealthy = isSessionHealthy()
            val hasValidTokens = hasValidTokens()
            val timeSinceLastActive = getTimeSinceLastActive()
            
            // Necesita recuperacion si:
            // - Sesion no saludable
            // - No hay tokens validos 
            // - Mucho tiempo inactivo (>24 horas)
            val needsRecovery = !sessionHealthy || !hasValidTokens || timeSinceLastActive > 24 * 60 * 60 * 1000
            
            if (needsRecovery) {
                Log.w(TAG, "Sesion necesita recuperacion: healthy=$sessionHealthy, tokens=$hasValidTokens, inactive=${timeSinceLastActive}ms")
            }
            
            needsRecovery
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando necesidad de recuperacion", e)
            true // En caso de error, asumir que necesita recuperacion
        }
    }
    
    /**
     * Ejecuta recuperacion completa de sesion
     */
    suspend fun performSessionRecovery(sessionManager: SessionManager): Boolean {
        return try {
            Log.i(TAG, "Iniciando recuperacion completa de sesion")
            
            // 1. Incrementar contador de recuperaciones
            incrementRecoveryCount()
            
            // 2. Verificar si hay sesion activa
            val hasActiveSession = sessionManager.isSessionActive()
            
            if (hasActiveSession) {
                Log.d(TAG, "Sesion activa encontrada, intentando refrescar tokens")
                
                // 3. Intentar refrescar tokens
                val newToken = sessionManager.refreshAccessToken()
                
                if (!newToken.isNullOrEmpty()) {
                    Log.i(TAG, "Recuperacion exitosa: tokens refrescados")
                    markSessionHealthy()
                    return true
                } else {
                    Log.w(TAG, "No se pudo refrescar tokens durante recuperacion")
                }
            } else {
                Log.w(TAG, "No hay sesion activa para recuperar")
            }
            
            // 4. Si llegamos aqui, la recuperacion fallo
            markSessionUnhealthy()
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error durante recuperacion de sesion", e)
            markSessionUnhealthy()
            false
        }
    }
    
    /**
     * Obtiene estadisticas de salud de sesion
     */
    suspend fun getSessionHealthStats(): String {
        return try {
            val sessionId = getSessionId()
            val lastActive = getLastActiveTimestamp()
            val recoveryCount = getRecoveryCount()
            val isHealthy = isSessionHealthy()
            val timeSinceActive = System.currentTimeMillis() - lastActive
            val backgroundTime = getBackgroundTimestamp()
            
            buildString {
                appendLine("=== Estado de Sesion ===")
                appendLine("Session ID: $sessionId")
                appendLine("Saludable: $isHealthy")
                appendLine("Tiempo desde actividad: ${timeSinceActive}ms")
                appendLine("Recuperaciones: $recoveryCount")
                appendLine("Ultimo background: $backgroundTime")
                appendLine("Version: ${getAppVersion()}")
            }
        } catch (e: Exception) {
            "Error obteniendo estadisticas: ${e.message}"
        }
    }
    
    // Metodos privados de utilidad
    
    private suspend fun handleProcessRecovery() {
        incrementRecoveryCount()
        context.sessionDataStore.edit { preferences ->
            preferences[KILL_RECOVERY_MODE] = true
        }
        Log.i(TAG, "Proceso de recuperacion iniciado")
    }
    
    private suspend fun getOrCreateSessionId(): String {
        return try {
            context.sessionDataStore.data.map { preferences ->
                preferences[SESSION_ID]
            }.first() ?: UUID.randomUUID().toString().also { newId ->
                context.sessionDataStore.edit { preferences ->
                    preferences[SESSION_ID] = newId
                }
                Log.d(TAG, "Nuevo session ID creado: $newId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo session ID", e)
            UUID.randomUUID().toString()
        }
    }
    
    private suspend fun getLastProcessId(): String? {
        return try {
            context.sessionDataStore.data.map { preferences ->
                preferences[PROCESS_ID]
            }.first()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generateProcessId(): String {
        return "${android.os.Process.myPid()}_${System.currentTimeMillis()}"
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName}_${packageInfo.longVersionCode}"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private suspend fun getRecoveryCount(): Long {
        return try {
            context.sessionDataStore.data.map { preferences ->
                preferences[RECOVERY_COUNT] ?: 0L
            }.first()
        } catch (e: Exception) {
            0L
        }
    }
    
    private suspend fun incrementRecoveryCount() {
        try {
            context.sessionDataStore.edit { preferences ->
                val current = preferences[RECOVERY_COUNT] ?: 0L
                preferences[RECOVERY_COUNT] = current + 1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementando contador de recuperacion", e)
        }
    }
    
    private suspend fun isSessionHealthy(): Boolean {
        return try {
            context.sessionDataStore.data.map { preferences ->
                preferences[SESSION_HEALTHY] ?: false
            }.first()
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun hasValidTokens(): Boolean {
        return try {
            val accessToken = userPreferences.accessToken.first()
            val refreshToken = userPreferences.refreshToken.first()
            !accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun getTimeSinceLastActive(): Long {
        return try {
            val lastActive = getLastActiveTimestamp()
            if (lastActive > 0) {
                System.currentTimeMillis() - lastActive
            } else {
                Long.MAX_VALUE
            }
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }
    
    private suspend fun getLastActiveTimestamp(): Long {
        return try {
            context.sessionDataStore.data.map { preferences ->
                preferences[LAST_ACTIVE_TIMESTAMP] ?: 0L
            }.first()
        } catch (e: Exception) {
            0L
        }
    }
    
    private suspend fun getBackgroundTimestamp(): Long {
        return try {
            context.sessionDataStore.data.map { preferences ->
                preferences[BACKGROUND_TIMESTAMP] ?: 0L
            }.first()
        } catch (e: Exception) {
            0L
        }
    }
    
    private suspend fun getSessionId(): String {
        return try {
            context.sessionDataStore.data.map { preferences ->
                preferences[SESSION_ID] ?: "unknown"
            }.first()
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private suspend fun markSessionHealthy() {
        try {
            context.sessionDataStore.edit { preferences ->
                preferences[SESSION_HEALTHY] = true
                preferences[LAST_ACTIVE_TIMESTAMP] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando sesion como saludable", e)
        }
    }
    
    private suspend fun markSessionUnhealthy() {
        try {
            context.sessionDataStore.edit { preferences ->
                preferences[SESSION_HEALTHY] = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando sesion como no saludable", e)
        }
    }
    
    /**
     * Limpia datos de persistencia (solo para logout completo)
     */
    suspend fun clearPersistenceData() {
        try {
            context.sessionDataStore.edit { preferences ->
                preferences.clear()
            }
            Log.d(TAG, "Datos de persistencia limpiados")
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando datos de persistencia", e)
        }
    }
}