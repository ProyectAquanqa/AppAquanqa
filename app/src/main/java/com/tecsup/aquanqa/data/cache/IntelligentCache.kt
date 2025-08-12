package com.tecsup.aquanqa.data.cache

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache inteligente que maneja diferentes tipos de datos con políticas específicas.
 * Thread-safe y optimizado para aplicaciones Android con múltiples usuarios.
 */
class IntelligentCache {
    
    // Storage thread-safe para las entradas de cache
    private val cacheStorage = ConcurrentHashMap<String, CacheEntry<*>>()
    
    // StateFlow para observar cambios en el cache
    private val _cacheUpdates = MutableStateFlow<CacheUpdateEvent?>(null)
    val cacheUpdates: StateFlow<CacheUpdateEvent?> = _cacheUpdates.asStateFlow()
    
    companion object {
        private const val TAG = "IntelligentCache"
        private const val SEPARATOR = "::"
        //Límites para prevenir memory leaks
        private const val MAX_CACHE_ENTRIES = 500
        private const val CLEANUP_THRESHOLD = 450 // Limpiar cuando llegue a 450
    }
    
    /**
     * Almacena datos en el cache con metadata completa
     */
    fun <T> put(
        key: String,
        data: T,
        cacheType: CacheStrategy.CacheType,
        userId: String? = null,
        etag: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val fullKey = buildKey(key, cacheType, userId)
        val entry = CacheEntry(
            data = data,
            userId = userId,
            etag = etag,
            metadata = metadata
        )
        
        cacheStorage[fullKey] = entry
        
        //  Auto-cleanup cuando se alcanza el límite
        if (cacheStorage.size >= CLEANUP_THRESHOLD) {
            performAutomaticCleanup()
        }
        
        // Notificar actualización
        _cacheUpdates.value = CacheUpdateEvent.DataUpdated(fullKey, cacheType)
        
        Log.d(TAG, "Cache updated: $fullKey - ${entry.getDebugInfo()}")
    }
    
    /**
     * Obtiene datos del cache si están válidos
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(
        key: String,
        cacheType: CacheStrategy.CacheType,
        userId: String? = null
    ): CacheResult<T> {
        val fullKey = buildKey(key, cacheType, userId)
        val entry = cacheStorage[fullKey] as? CacheEntry<T>
        
        return when {
            entry == null -> {
                Log.d(TAG, "Cache miss: $fullKey")
                CacheResult.Miss
            }
            
            !entry.isValidForUser(userId) -> {
                Log.d(TAG, "Cache invalid for user: $fullKey")
                CacheResult.Miss
            }
            
            entry.isExpired(cacheType.duration) -> {
                Log.d(TAG, "Cache expired: $fullKey - ${entry.getDebugInfo()}")
                CacheResult.Expired(entry.data as T)
            }
            
            else -> {
                Log.d(TAG, "Cache hit: $fullKey - ${entry.getDebugInfo()}")
                CacheResult.Hit(entry.data as T, entry.source)
            }
        }
    }
    
    /**
     * Invalida cache específico
     */
    fun invalidate(
        key: String,
        cacheType: CacheStrategy.CacheType,
        userId: String? = null
    ) {
        val fullKey = buildKey(key, cacheType, userId)
        cacheStorage.remove(fullKey)
        
        _cacheUpdates.value = CacheUpdateEvent.DataInvalidated(fullKey, cacheType)
        Log.d(TAG, "Cache invalidated: $fullKey")
    }
    
    /**
     * Invalida todo el cache de un tipo específico
     */
    fun invalidateType(cacheType: CacheStrategy.CacheType) {
        val keysToRemove = cacheStorage.keys.filter { it.contains("${cacheType.name}$SEPARATOR") }
        keysToRemove.forEach { cacheStorage.remove(it) }
        
        _cacheUpdates.value = CacheUpdateEvent.TypeInvalidated(cacheType)
        Log.d(TAG, "Cache type invalidated: ${cacheType.name} (${keysToRemove.size} entries)")
    }
    
    /**
     * Invalida todo el cache de un usuario específico
     */
    fun invalidateUser(userId: String) {
        val keysToRemove = cacheStorage.keys.filter { key ->
            val entry = cacheStorage[key]
            entry?.userId == userId
        }
        keysToRemove.forEach { cacheStorage.remove(it) }
        
        _cacheUpdates.value = CacheUpdateEvent.UserInvalidated(userId)
        Log.d(TAG, "User cache invalidated: $userId (${keysToRemove.size} entries)")
    }
    
    /**
     * Limpia todo el cache
     */
    fun clearAll() {
        val size = cacheStorage.size
        cacheStorage.clear()
        
        _cacheUpdates.value = CacheUpdateEvent.AllCleared
        Log.d(TAG, "All cache cleared ($size entries)")
    }
    
    /**
     * Obtiene estadísticas del cache
     */
    fun getStats(): CacheStats {
        val totalEntries = cacheStorage.size
        val entriesByType = CacheStrategy.CacheType.values().associateWith { type ->
            cacheStorage.keys.count { it.contains("${type.name}$SEPARATOR") }
        }
        val expiredEntries = cacheStorage.values.count { entry ->
            val type = CacheStrategy.CacheType.values().find { 
                cacheStorage.keys.any { key -> key.contains("${it.name}$SEPARATOR") }
            }
            type?.let { (entry as CacheEntry<*>).isExpired(it.duration) } ?: false
        }
        
        return CacheStats(
            totalEntries = totalEntries,
            entriesByType = entriesByType,
            expiredEntries = expiredEntries
        )
    }
    
    /**
     * Construye la clave completa del cache
     */
    private fun buildKey(
        key: String,
        cacheType: CacheStrategy.CacheType,
        userId: String?
    ): String {
        return "${cacheType.name}$SEPARATOR${userId ?: "global"}$SEPARATOR$key"
    }
    
    /**
     * Limpieza automática para prevenir memory leaks
     */
    private fun performAutomaticCleanup() {
        try {
            val currentTime = System.currentTimeMillis()
            var removedCount = 0
            
            // 1. Remover entradas expiradas primero
            val expiredKeys = cacheStorage.entries
                .filter { (_, entry) ->
                    val type = CacheStrategy.CacheType.values().find { 
                        entry.toString().contains(it.name) 
                    } ?: CacheStrategy.CacheType.EVENTS
                    entry.isExpired(type.duration)
                }
                .map { it.key }
                .take(50) // Máximo 50 entradas por limpieza
            
            expiredKeys.forEach { key ->
                cacheStorage.remove(key)
                removedCount++
            }
            
            // 2. Si aún hay muchas entradas, remover las más antiguas
            if (cacheStorage.size > MAX_CACHE_ENTRIES) {
                val oldestKeys = cacheStorage.entries
                    .sortedBy { it.value.timestamp }
                    .take(50)
                    .map { it.key }
                
                oldestKeys.forEach { key ->
                    cacheStorage.remove(key)
                    removedCount++
                }
            }
            
            Log.i(TAG, "Auto-cleanup completed: $removedCount entries removed, ${cacheStorage.size} remaining")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during auto-cleanup: ${e.message}")
        }
    }
}

/**
 * Resultado de una operación de cache
 */
sealed class CacheResult<out T> {
    object Miss : CacheResult<Nothing>()
    data class Hit<T>(val data: T, val source: DataSource) : CacheResult<T>()
    data class Expired<T>(val data: T) : CacheResult<T>()
}

/**
 * Eventos de actualización del cache
 */
sealed class CacheUpdateEvent {
    data class DataUpdated(val key: String, val type: CacheStrategy.CacheType) : CacheUpdateEvent()
    data class DataInvalidated(val key: String, val type: CacheStrategy.CacheType) : CacheUpdateEvent()
    data class TypeInvalidated(val type: CacheStrategy.CacheType) : CacheUpdateEvent()
    data class UserInvalidated(val userId: String) : CacheUpdateEvent()
    object AllCleared : CacheUpdateEvent()
}

/**
 * Estadísticas del cache
 */
data class CacheStats(
    val totalEntries: Int,
    val entriesByType: Map<CacheStrategy.CacheType, Int>,
    val expiredEntries: Int
)