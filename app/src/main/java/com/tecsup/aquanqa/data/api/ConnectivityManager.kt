package com.tecsup.aquanqa.data.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

/**
 * Manager inteligente para detectar y monitorear el estado de conectividad
 * Proporciona informacion detallada sobre la calidad de la conexion
 * para ajustar dinamicamente timeouts y politicas de retry
 */
class NetworkConnectivityManager private constructor(
    private val context: Context
) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Estados de conectividad
    private val _connectivityState = MutableStateFlow(NetworkConfig.ConnectivityState.GOOD)
    val connectivityState: StateFlow<NetworkConfig.ConnectivityState> = _connectivityState.asStateFlow()
    
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _networkType = MutableStateFlow(NetworkType.WIFI)
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()
    
    // Metricas de conectividad
    private var lastConnectivityCheck = 0L
    private var consecutiveFailures = 0
    private var averageResponseTime = 0L
    private var isMonitoring = false
    
    companion object {
        private const val TAG = "NetworkConnectivity"
        private const val CONNECTIVITY_TEST_URL = "https://www.google.com/generate_204"
        private const val CONNECTIVITY_CHECK_INTERVAL = 30000L // 30 segundos
        private const val RESPONSE_TIME_SAMPLES = 5
        
        @Volatile
        private var INSTANCE: NetworkConnectivityManager? = null
        
        fun getInstance(context: Context): NetworkConnectivityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkConnectivityManager(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }
        }
    }
    
    enum class NetworkType {
        WIFI,
        CELLULAR_5G,
        CELLULAR_4G,
        CELLULAR_3G,
        CELLULAR_2G,
        ETHERNET,
        UNKNOWN,
        NONE
    }
    
    /**
     * Informacion detallada de conectividad
     */
    data class ConnectivityInfo(
        val isConnected: Boolean,
        val networkType: NetworkType,
        val connectivityState: NetworkConfig.ConnectivityState,
        val averageResponseTime: Long,
        val consecutiveFailures: Int,
        val isMetered: Boolean,
        val signalStrength: Int // 0-4, -1 si no disponible
    )
    
    /**
     * Inicia el monitoreo de conectividad
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "Monitoring already started")
            return
        }
        
        isMonitoring = true
        Log.i(TAG, "Starting connectivity monitoring")
        
        // Registrar callback para cambios de red
        registerNetworkCallback()
        
        // Iniciar verificacion periodica
        startPeriodicConnectivityCheck()
        
        // Verificacion inicial
        checkConnectivityNow()
    }
    
    /**
     * Detiene el monitoreo de conectividad
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        isMonitoring = false
        Log.i(TAG, "Stopping connectivity monitoring")
        
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network callback", e)
        }
    }
    
    /**
     * Callback para cambios de red
     */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available: $network")
            scope.launch {
                updateConnectivityState()
            }
        }
        
        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost: $network")
            scope.launch {
                updateConnectivityState()
            }
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            Log.v(TAG, "Network capabilities changed")
            scope.launch {
                updateConnectivityState()
            }
        }
    }
    
    /**
     * Registra el callback de red
     */
    private fun registerNetworkCallback() {
        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()
            
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            Log.d(TAG, "Network callback registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback", e)
        }
    }
    
    /**
     * Inicia verificacion periodica de conectividad
     */
    private fun startPeriodicConnectivityCheck() {
        scope.launch {
            while (isMonitoring) {
                try {
                    delay(CONNECTIVITY_CHECK_INTERVAL)
                    if (isMonitoring) {
                        checkConnectivityNow()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic connectivity check", e)
                }
            }
        }
    }
    
    /**
     * Verifica conectividad inmediatamente
     */
    fun checkConnectivityNow() {
        scope.launch {
            updateConnectivityState()
        }
    }
    
    /**
     * Actualiza el estado de conectividad
     */
    private suspend fun updateConnectivityState() {
        try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            
            val isConnected = activeNetwork != null && networkCapabilities != null &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            
            _isConnected.value = isConnected
            
            if (isConnected && networkCapabilities != null) {
                // Determinar tipo de red
                val networkType = determineNetworkType(networkCapabilities)
                _networkType.value = networkType
                
                // Medir calidad de conexion
                val responseTime = measureResponseTime()
                updateAverageResponseTime(responseTime)
                
                // Determinar estado de conectividad basado en metricas
                val connectivityState = determineConnectivityState(networkType, averageResponseTime, consecutiveFailures)
                _connectivityState.value = connectivityState
                
                Log.d(TAG, "Connectivity updated: type=$networkType, state=$connectivityState, responseTime=${averageResponseTime}ms")
                
            } else {
                _networkType.value = NetworkType.NONE
                _connectivityState.value = NetworkConfig.ConnectivityState.NO_CONNECTION
                consecutiveFailures++
                Log.w(TAG, "No connectivity detected, failures: $consecutiveFailures")
            }
            
            lastConnectivityCheck = System.currentTimeMillis()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating connectivity state", e)
            consecutiveFailures++
        }
    }
    
    /**
     * Determina el tipo de red
     */
    private fun determineNetworkType(capabilities: NetworkCapabilities): NetworkType {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                // Determinar generacion celular basado en velocidad
                when {
                    capabilities.linkDownstreamBandwidthKbps >= 100000 -> NetworkType.CELLULAR_5G // >100 Mbps
                    capabilities.linkDownstreamBandwidthKbps >= 10000 -> NetworkType.CELLULAR_4G  // >10 Mbps
                    capabilities.linkDownstreamBandwidthKbps >= 1000 -> NetworkType.CELLULAR_3G   // >1 Mbps
                    else -> NetworkType.CELLULAR_2G
                }
            }
            else -> NetworkType.UNKNOWN
        }
    }
    
    /**
     * Mide tiempo de respuesta haciendo ping a un servidor confiable
     */
    private suspend fun measureResponseTime(): Long {
        return try {
            val startTime = System.currentTimeMillis()
            
            val connection = URL(CONNECTIVITY_TEST_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.useCaches = false
            connection.instanceFollowRedirects = false
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            val responseTime = System.currentTimeMillis() - startTime
            
            if (responseCode == 204 || responseCode == 200) {
                consecutiveFailures = 0 // Reset failures en conexion exitosa
                responseTime
            } else {
                consecutiveFailures++
                Long.MAX_VALUE // Indicar falla
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error measuring response time", e)
            consecutiveFailures++
            Long.MAX_VALUE
        }
    }
    
    /**
     * Actualiza el promedio de tiempo de respuesta
     */
    private fun updateAverageResponseTime(newResponseTime: Long) {
        if (newResponseTime == Long.MAX_VALUE) return
        
        averageResponseTime = if (averageResponseTime == 0L) {
            newResponseTime
        } else {
            // Promedio movil simple
            (averageResponseTime + newResponseTime) / 2
        }
    }
    
    /**
     * Determina el estado de conectividad basado en metricas
     */
    private fun determineConnectivityState(
        networkType: NetworkType,
        avgResponseTime: Long,
        failures: Int
    ): NetworkConfig.ConnectivityState {
        
        // Si hay muchas fallas consecutivas
        if (failures >= 3) {
            return NetworkConfig.ConnectivityState.POOR
        }
        
        // Basado en tipo de red y tiempo de respuesta
        return when (networkType) {
            NetworkType.WIFI, NetworkType.ETHERNET -> {
                when {
                    avgResponseTime <= 100 -> NetworkConfig.ConnectivityState.EXCELLENT
                    avgResponseTime <= 300 -> NetworkConfig.ConnectivityState.GOOD
                    avgResponseTime <= 1000 -> NetworkConfig.ConnectivityState.FAIR
                    else -> NetworkConfig.ConnectivityState.POOR
                }
            }
            
            NetworkType.CELLULAR_5G, NetworkType.CELLULAR_4G -> {
                when {
                    avgResponseTime <= 200 -> NetworkConfig.ConnectivityState.EXCELLENT
                    avgResponseTime <= 500 -> NetworkConfig.ConnectivityState.GOOD
                    avgResponseTime <= 1500 -> NetworkConfig.ConnectivityState.FAIR
                    else -> NetworkConfig.ConnectivityState.POOR
                }
            }
            
            NetworkType.CELLULAR_3G -> {
                when {
                    avgResponseTime <= 500 -> NetworkConfig.ConnectivityState.GOOD
                    avgResponseTime <= 2000 -> NetworkConfig.ConnectivityState.FAIR
                    else -> NetworkConfig.ConnectivityState.POOR
                }
            }
            
            NetworkType.CELLULAR_2G -> {
                when {
                    avgResponseTime <= 2000 -> NetworkConfig.ConnectivityState.FAIR
                    else -> NetworkConfig.ConnectivityState.POOR
                }
            }
            
            NetworkType.NONE -> NetworkConfig.ConnectivityState.NO_CONNECTION
            NetworkType.UNKNOWN -> NetworkConfig.ConnectivityState.FAIR
        }
    }
    
    /**
     * Obtiene informacion completa de conectividad
     */
    fun getConnectivityInfo(): ConnectivityInfo {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        
        return ConnectivityInfo(
            isConnected = _isConnected.value,
            networkType = _networkType.value,
            connectivityState = _connectivityState.value,
            averageResponseTime = averageResponseTime,
            consecutiveFailures = consecutiveFailures,
            isMetered = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false,
            signalStrength = -1 // TODO: Implementar si es necesario
        )
    }
    
    /**
     * Verifica si una operacion deberia continuar basado en conectividad
     */
    fun shouldProceedWithOperation(operationType: NetworkConfig.OperationType): Boolean {
        val connectivityInfo = getConnectivityInfo()
        
        return when {
            !connectivityInfo.isConnected -> false
            connectivityInfo.connectivityState == NetworkConfig.ConnectivityState.NO_CONNECTION -> false
            operationType == NetworkConfig.OperationType.TOKEN_REFRESH && 
                connectivityInfo.connectivityState == NetworkConfig.ConnectivityState.POOR -> true // Siempre intentar refresh
            operationType == NetworkConfig.OperationType.AUTHENTICATION &&
                connectivityInfo.connectivityState == NetworkConfig.ConnectivityState.POOR -> true // Siempre intentar auth
            connectivityInfo.consecutiveFailures >= 5 -> false
            else -> true
        }
    }
    
    /**
     * Espera hasta que la conectividad mejore o timeout
     */
    suspend fun waitForConnectivity(maxWaitTimeMs: Long = 30000L): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < maxWaitTimeMs) {
            if (_isConnected.value && _connectivityState.value != NetworkConfig.ConnectivityState.NO_CONNECTION) {
                return true
            }
            
            delay(min(1000L, maxWaitTimeMs / 10)) // Check every second or 1/10 of max wait time
        }
        
        return false
    }
    
    /**
     * Obtiene estadisticas de conectividad para debugging
     */
    fun getNetworkStats(): String {
        val info = getConnectivityInfo()
        return buildString {
            appendLine("=== Network Statistics ===")
            appendLine("Connected: ${info.isConnected}")
            appendLine("Network Type: ${info.networkType}")
            appendLine("Connectivity State: ${info.connectivityState}")
            appendLine("Average Response Time: ${info.averageResponseTime}ms")
            appendLine("Consecutive Failures: ${info.consecutiveFailures}")
            appendLine("Is Metered: ${info.isMetered}")
            appendLine("Last Check: ${System.currentTimeMillis() - lastConnectivityCheck}ms ago")
        }
    }
}