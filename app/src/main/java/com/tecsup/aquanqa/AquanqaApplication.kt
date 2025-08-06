package com.tecsup.aquanqa

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.tecsup.aquanqa.data.SessionManager
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.api.RetrofitClient
import com.tecsup.aquanqa.data.manager.SessionPersistenceManager
import com.tecsup.aquanqa.data.manager.TokenRefreshManager
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AquanqaApplication : Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    
    private lateinit var sessionPersistenceManager: SessionPersistenceManager
    private lateinit var sessionManager: SessionManager
    private lateinit var userPreferences: UserPreferences
    private lateinit var tokenRefreshManager: TokenRefreshManager
    
    // Scope para operaciones de ciclo de vida
    private val applicationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Tracking de estado de aplicacion
    private var isAppInForeground = false
    private var activeActivities = 0
    
    companion object {
        private const val TAG = "AquanqaApplication"
        
        @Volatile
        private var instance: AquanqaApplication? = null
        
        fun getInstance(): AquanqaApplication? = instance
    }
    
    override fun onCreate() {
        super<Application>.onCreate()
        instance = this
        
        Log.i(TAG, "Inicializando AquanqaApplication")
        
        // Inicializar componentes core
        initializeCoreComponents()
        
        // Configurar observers de ciclo de vida
        setupLifecycleObservers()
        
        // Inicializar persistencia de sesion
        initializeSessionPersistence()
        
        Log.i(TAG, "AquanqaApplication inicializada completamente")
    }
    
    private fun initializeCoreComponents() {
        try {
            // Inicializar ApiClient con capacidades de red robustas
            val apiClient = ApiClient.getClient(this)
            
            // Iniciar monitoreo de conectividad inmediatamente
            if (apiClient is RetrofitClient) {
                apiClient.startConnectivityMonitoring()
                Log.d(TAG, "Connectivity monitoring iniciado")
            }
            
            // Inicializar managers
            userPreferences = UserPreferences(this)
            sessionPersistenceManager = SessionPersistenceManager.getInstance(this)
            sessionManager = SessionManager(this, userPreferences)
            tokenRefreshManager = TokenRefreshManager.getInstance(this)
            
            Log.d(TAG, "Componentes core inicializados con capacidades de red robustas")
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando componentes core", e)
        }
    }
    
    private fun setupLifecycleObservers() {
        try {
            // Observer para ciclo de vida del proceso completo
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            
            // Observer para actividades individuales
            registerActivityLifecycleCallbacks(this)
            
            Log.d(TAG, "Lifecycle observers configurados")
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando lifecycle observers", e)
        }
    }
    
    private fun initializeSessionPersistence() {
        applicationScope.launch {
            try {
                Log.d(TAG, "Inicializando persistencia de sesion")
                
                val sessionState = sessionPersistenceManager.initializeSessionPersistence()
                
                if (sessionState.wasKilled) {
                    Log.i(TAG, "Detectada recuperacion tras kill - iniciando recuperacion automatica")
                    handlePostKillRecovery()
                } else {
                    Log.d(TAG, "Inicio normal - sesion saludable")
                }
                
                // Iniciar monitoreo de tokens si hay sesion activa
                if (sessionManager.isSessionActive()) {
                    tokenRefreshManager.startTokenMonitoring()
                    Log.d(TAG, "Monitoreo de tokens iniciado")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando persistencia de sesion", e)
            }
        }
    }
    
    private suspend fun handlePostKillRecovery() {
        try {
            Log.i(TAG, "Ejecutando recuperacion post-kill")
            
            val recoverySuccess = sessionPersistenceManager.performSessionRecovery(sessionManager)
            
            if (recoverySuccess) {
                Log.i(TAG, "Recuperacion post-kill exitosa")
                // Reiniciar monitoreo de tokens
                tokenRefreshManager.startTokenMonitoring()
            } else {
                Log.w(TAG, "Recuperacion post-kill fallo - sesion puede requerir relogin")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error durante recuperacion post-kill", e)
        }
    }
    
    // Lifecycle Observer Methods (Para el proceso completo)
    
    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "Aplicacion volvio a foreground")
        isAppInForeground = true
        
        applicationScope.launch {
            try {
                val timeInBackground = sessionPersistenceManager.markAppComingToForeground()
                
                // Verificar salud de conectividad al volver a foreground
                val apiClient = ApiClient.getClient(this@AquanqaApplication)
                if (apiClient is RetrofitClient) {
                    val networkHealthy = apiClient.performHealthCheck()
                    Log.d(TAG, "Network health check: $networkHealthy")
                    
                    if (!networkHealthy && sessionManager.isSessionActive()) {
                        Log.w(TAG, "Network unhealthy but session active - monitoring closely")
                    }
                }
                
                // Si estuvo mucho tiempo en background, verificar salud de sesion
                if (timeInBackground > 30 * 60 * 1000) { // 30 minutos
                    Log.d(TAG, "App estuvo ${timeInBackground}ms en background - verificando sesion")
                    
                    if (sessionPersistenceManager.needsSessionRecovery()) {
                        Log.i(TAG, "Sesion necesita recuperacion tras background prolongado")
                        sessionPersistenceManager.performSessionRecovery(sessionManager)
                    }
                }
                
                // Reiniciar monitoreo si hay sesion activa
                if (sessionManager.isSessionActive()) {
                    tokenRefreshManager.startTokenMonitoring()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error manejando foreground", e)
            }
        }
    }
    
    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "Aplicacion paso a background")
        isAppInForeground = false
        
        applicationScope.launch {
            try {
                sessionPersistenceManager.markAppGoingToBackground()
                
                // Opcional: pausar algunos servicios para ahorrar bateria
                // pero mantener sesion activa
                
            } catch (e: Exception) {
                Log.e(TAG, "Error manejando background", e)
            }
        }
    }
    
    // Activity Lifecycle Callbacks
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.v(TAG, "Activity creada: ${activity.localClassName}")
    }
    
    override fun onActivityStarted(activity: Activity) {
        activeActivities++
        Log.v(TAG, "Activity iniciada: ${activity.localClassName} (activas: $activeActivities)")
        
        // Actualizar timestamp de actividad
        applicationScope.launch {
            try {
                sessionPersistenceManager.updateLastActive()
            } catch (e: Exception) {
                Log.e(TAG, "Error actualizando actividad", e)
            }
        }
    }
    
    override fun onActivityResumed(activity: Activity) {
        Log.v(TAG, "Activity resumida: ${activity.localClassName}")
    }
    
    override fun onActivityPaused(activity: Activity) {
        Log.v(TAG, "Activity pausada: ${activity.localClassName}")
    }
    
    override fun onActivityStopped(activity: Activity) {
        activeActivities--
        Log.v(TAG, "Activity detenida: ${activity.localClassName} (activas: $activeActivities)")
    }
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        Log.v(TAG, "Guardando estado: ${activity.localClassName}")
    }
    
    override fun onActivityDestroyed(activity: Activity) {
        Log.v(TAG, "Activity destruida: ${activity.localClassName}")
    }
    
    // Metodos publicos para acceso global
    
    fun getSessionPersistenceManager(): SessionPersistenceManager = sessionPersistenceManager
    
    fun getSessionManager(): SessionManager = sessionManager
    
    fun getUserPreferences(): UserPreferences = userPreferences
    
    fun getTokenRefreshManager(): TokenRefreshManager = tokenRefreshManager
    
    fun isAppInForeground(): Boolean = isAppInForeground
    
    fun getActiveActivitiesCount(): Int = activeActivities
    
    /**
     * Fuerza una verificacion de salud de sesion
     */
    fun checkSessionHealth() {
        applicationScope.launch {
            try {
                val stats = sessionPersistenceManager.getSessionHealthStats()
                Log.i(TAG, "Estadisticas de sesion:\n$stats")
                
                if (sessionPersistenceManager.needsSessionRecovery()) {
                    Log.w(TAG, "Sesion necesita recuperacion")
                    sessionPersistenceManager.performSessionRecovery(sessionManager)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error verificando salud de sesion", e)
            }
        }
    }
    
    /**
     * Limpia completamente la sesion (para logout)
     */
    fun clearCompleteSession() {
        applicationScope.launch {
            try {
                Log.i(TAG, "Limpiando sesion completa")
                
                tokenRefreshManager.stopTokenMonitoring()
                sessionPersistenceManager.clearPersistenceData()
                userPreferences.clear()
                
                // Limpiar cache de red
                val apiClient = ApiClient.getClient(this@AquanqaApplication)
                if (apiClient is RetrofitClient) {
                    apiClient.clearNetworkCache()
                }
                
                Log.d(TAG, "Sesion completamente limpiada")
            } catch (e: Exception) {
                Log.e(TAG, "Error limpiando sesion completa", e)
            }
        }
    }
    
    /**
     * Obtiene estadisticas completas de red y conectividad
     */
    fun getNetworkStats(): String {
        return try {
            val apiClient = ApiClient.getClient(this)
            if (apiClient is RetrofitClient) {
                apiClient.getNetworkStats()
            } else {
                "Network stats not available"
            }
        } catch (e: Exception) {
            "Error getting network stats: ${e.message}"
        }
    }
    
    /**
     * Fuerza verificacion de salud de red
     */
    fun checkNetworkHealth() {
        applicationScope.launch {
            try {
                val apiClient = ApiClient.getClient(this@AquanqaApplication)
                if (apiClient is RetrofitClient) {
                    val healthy = apiClient.performHealthCheck()
                    Log.i(TAG, "Network health check result: $healthy")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking network health", e)
            }
        }
    }
} 