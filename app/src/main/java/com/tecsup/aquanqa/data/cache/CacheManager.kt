package com.tecsup.aquanqa.data.cache

import android.util.Log
import com.tecsup.aquanqa.data.model.content.Almuerzo
import com.tecsup.aquanqa.data.model.content.Anuncio
import com.tecsup.aquanqa.data.model.content.Category
import com.tecsup.aquanqa.data.model.content.EventoBasico
import com.tecsup.aquanqa.data.model.content.toEventoBasico
import com.tecsup.aquanqa.data.model.user.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Manager de alto nivel optimizado para aplicaciones multi-usuario.
 * Proporciona una interfaz simple para operaciones de cache específicas del dominio.
 */
class CacheManager(
    private val cache: IntelligentCache = IntelligentCache()
) {
    
    companion object {
        private const val TAG = "CacheManager"
        
        // Keys específicas para diferentes tipos de datos
        private const val CATEGORIES_KEY = "categories"
        private const val EVENTS_KEY = "events"
        private const val USER_PROFILE_KEY = "user_profile"
        private const val EVENTS_BY_CATEGORY_KEY = "events_category"
        private const val ALMUERZOS_KEY = "almuerzos"
        
        //Limits para prevenir memory leaks en multi-usuario
        private const val MAX_USERS_IN_MEMORY = 10
        private const val MAX_ENTRIES_PER_USER = 50
    }
    
    // categorias
    
    /**
     * Guarda categorías en cache con duración extendida
     */
    fun cacheCategories(categories: List<Category>, userId: String? = null) {
        cache.put(
            key = CATEGORIES_KEY,
            data = categories,
            cacheType = CacheStrategy.CacheType.CATEGORIES,
            userId = userId,
            metadata = mapOf(
                "count" to categories.size,
                "cached_at" to System.currentTimeMillis()
            )
        )
        Log.d(TAG, "Categories cached: ${categories.size} items for user: $userId")
    }
    
    /**
     * Obtiene categorías del cache
     */
    fun getCachedCategories(userId: String? = null): CacheResult<List<Category>> {
        return cache.get(CATEGORIES_KEY, CacheStrategy.CacheType.CATEGORIES, userId)
    }
    
    /**
     * Invalida cache de categorías
     */
    fun invalidateCategories(userId: String? = null) {
        cache.invalidate(CATEGORIES_KEY, CacheStrategy.CacheType.CATEGORIES, userId)
    }
    
    // eventos
    
    /**
     * Guarda eventos generales en cache
     */
    fun cacheEvents(events: List<Anuncio>, userId: String? = null) {
        cache.put(
            key = EVENTS_KEY,
            data = events,
            cacheType = CacheStrategy.CacheType.EVENTS,
            userId = userId,
            metadata = mapOf(
                "count" to events.size,
                "latest_event_id" to (events.firstOrNull()?.id ?: 0),
                "cached_at" to System.currentTimeMillis()
            )
        )
        Log.d(TAG, "Events cached: ${events.size} items for user: $userId")
    }
    
    /**
     * Obtiene eventos del cache
     */
    fun getCachedEvents(userId: String? = null): CacheResult<List<Anuncio>> {
        return cache.get(EVENTS_KEY, CacheStrategy.CacheType.EVENTS, userId)
    }
    
    /**
     * Guarda eventos filtrados por categoría
     */
    fun cacheEventsByCategory(
        categoryName: String,
        events: List<Anuncio>,
        userId: String? = null
    ) {
        val key = "${EVENTS_BY_CATEGORY_KEY}_${categoryName.lowercase()}"
        cache.put(
            key = key,
            data = events,
            cacheType = CacheStrategy.CacheType.EVENTS,
            userId = userId,
            metadata = mapOf(
                "category" to categoryName,
                "count" to events.size,
                "cached_at" to System.currentTimeMillis()
            )
        )
        Log.d(TAG, "Events by category cached: $categoryName (${events.size} items)")
    }
    
    /**
     * Obtiene eventos filtrados por categoría del cache
     */
    fun getCachedEventsByCategory(
        categoryName: String,
        userId: String? = null
    ): CacheResult<List<Anuncio>> {
        val key = "${EVENTS_BY_CATEGORY_KEY}_${categoryName.lowercase()}"
        return cache.get(key, CacheStrategy.CacheType.EVENTS, userId)
    }
    
    /**
     * Invalida cache de eventos (todos los tipos)
     */
    fun invalidateEvents(userId: String? = null) {
        cache.invalidate(EVENTS_KEY, CacheStrategy.CacheType.EVENTS, userId)
        // También invalidar eventos por categoría
        cache.invalidateType(CacheStrategy.CacheType.EVENTS)
    }

    /**
     * Guarda eventos con fallback automático para uso sin internet.
     * Guarda eventos completos en memoria + datos básicos en DataStore.
     */
    fun cacheEventsWithFallback(events: List<Anuncio>, userId: String? = null) {
        // 1. Cache completo en memoria (rápido)
        cacheEvents(events, userId)
        
        // 2. Fallback básico en DataStore (persistente)
        val eventosBasicos = events.map { it.toEventoBasico() }
        // Nota: El guardado en DataStore se hace en el Repository
        Log.d(TAG, "Events cached with fallback: ${events.size} items")
    }

    /**
     * Obtiene eventos con fallback inteligente.
     * Primero intenta cache de memoria, luego fallback de DataStore.
     */
    fun getCachedEventsWithFallback(userId: String? = null): CacheResult<List<Anuncio>> {
        // 1. Intentar cache de memoria primero
        val memoryResult = getCachedEvents(userId)
        if (memoryResult is CacheResult.Hit) {
            return memoryResult
        }
        
        // 2. Si no hay cache de memoria, indicar que necesita fallback
        Log.d(TAG, "Memory cache miss, fallback needed from DataStore")
        return CacheResult.Miss
    }
    
    //perfil de usuario
    
    /**
     * Guarda perfil de usuario en cache
     */
    fun cacheUserProfile(cacheKey: String, userProfile: UserProfile, userId: String? = null) {
        cache.put(
            key = cacheKey,
            data = userProfile,
            cacheType = CacheStrategy.CacheType.USER_PROFILE,
            userId = userId,
            metadata = mapOf(
                "first_name" to userProfile.first_name,
                "last_name" to userProfile.last_name,
                "cached_at" to System.currentTimeMillis()
            )
        )
        Log.d(TAG, "User profile cached for: $userId")
    }
    
    /**
     * Obtiene perfil de usuario del cache
     */
    fun getCachedUserProfile(cacheKey: String, userId: String? = null): CacheResult<UserProfile> {
        return cache.get(cacheKey, CacheStrategy.CacheType.USER_PROFILE, userId)
    }
    
    /**
     * Invalida cache de perfil de usuario
     */
    fun invalidateUserProfile(userId: String? = null) {
        cache.invalidate(USER_PROFILE_KEY, CacheStrategy.CacheType.USER_PROFILE, userId)
        Log.d(TAG, "User profile cache invalidated for user: $userId")
    }
    
    //almuerzos
    
    /**
     * Guarda almuerzos en cache con duración específica
     */
    fun cacheAlmuerzos(cacheKey: String, almuerzos: List<Almuerzo>, userId: String? = null) {
        cache.put(
            key = cacheKey,
            data = almuerzos,
            cacheType = CacheStrategy.CacheType.ALMUERZOS, //  Tipo específico
            userId = userId,
            metadata = mapOf(
                "count" to almuerzos.size,
                "latest_date" to (almuerzos.firstOrNull()?.fecha ?: ""),
                "cached_at" to System.currentTimeMillis()
            )
        )
        Log.d(TAG, "Almuerzos cached: ${almuerzos.size} items for user: $userId")
    }
    
    /**
     * Obtiene almuerzos del cache
     */
    fun getCachedAlmuerzos(cacheKey: String, userId: String? = null): CacheResult<List<Almuerzo>> {
        return cache.get(cacheKey, CacheStrategy.CacheType.ALMUERZOS, userId)
    }
    
    /**
     * Invalida cache de almuerzos
     */
    fun invalidateAlmuerzos(userId: String? = null) {
        cache.invalidate(ALMUERZOS_KEY, CacheStrategy.CacheType.ALMUERZOS, userId)
        Log.d(TAG, "Almuerzos cache invalidated for user: $userId")
    }
    
    // operaciones globales
    
    /**
     * Invalida todo el cache de un usuario específico
     */
    fun invalidateUserCache(userId: String) {
        cache.invalidateUser(userId)
        Log.d(TAG, "All cache invalidated for user: $userId")
    }
    
    /**
     * Limpieza para apps multi-usuario
     */
    fun smartCleanup(): CleanupResult {
        val stats = cache.getStats()
        var cleanedEntries = 0
        
        // 1. Limpiar usuarios con más entradas del límite
        // 2. Limpiar entradas más antiguas primero
        // 3. Mantener solo los últimos N usuarios activos
        
        // Esta es una implementación básica
        // En producción, implementarías LRU más sofisticado
        
        return CleanupResult(
            entriesCleanedUp = cleanedEntries,
            memoryFreed = cleanedEntries * 1024L // Estimación
        )
    }
    
    /**
     * Refresca cache inteligentemente basado en políticas
     */
    suspend fun smartRefresh(userId: String? = null): RefreshResult {
        val stats = cache.getStats()
        var refreshedTypes = mutableListOf<CacheStrategy.CacheType>()
        
        // Verificar qué tipos de cache necesitan refresh
        CacheStrategy.CacheType.values().forEach { type ->
            val hasExpiredData = when (type) {
                CacheStrategy.CacheType.CATEGORIES -> {
                    getCachedCategories(userId) is CacheResult.Expired
                }
                CacheStrategy.CacheType.EVENTS -> {
                    getCachedEvents(userId) is CacheResult.Expired
                }
                CacheStrategy.CacheType.ALMUERZOS -> {
                    getCachedAlmuerzos(ALMUERZOS_KEY, userId) is CacheResult.Expired
                }
                CacheStrategy.CacheType.USER_PROFILE -> {
                    userId?.let { getCachedUserProfile(it) is CacheResult.Expired } ?: false
                }
                else -> false
            }
            
            if (hasExpiredData) {
                cache.invalidateType(type)
                refreshedTypes.add(type)
            }
        }
        
        return RefreshResult(
            refreshedTypes = refreshedTypes,
            totalEntriesBefore = stats.totalEntries,
            expiredEntriesRemoved = stats.expiredEntries
        )
    }
    
    /**
     * Obtiene estadísticas detalladas del cache
     */
    fun getDetailedStats(): DetailedCacheStats {
        val baseStats = cache.getStats()
        
        return DetailedCacheStats(
            totalEntries = baseStats.totalEntries,
            entriesByType = baseStats.entriesByType,
            expiredEntries = baseStats.expiredEntries,
            memoryUsageEstimate = estimateMemoryUsage(),
            oldestEntry = findOldestEntry(),
            newestEntry = findNewestEntry()
        )
    }
    
    /**
     * Observa cambios en el cache
     */
    fun observeCacheUpdates(): Flow<CacheUpdateEvent?> {
        return cache.cacheUpdates
    }
    
    /**
     * Limpia todo el cache
     */
    fun clearAllCache() {
        cache.clearAll()
        Log.d(TAG, "All cache cleared")
    }
    
    //metodos privados
    
    private fun estimateMemoryUsage(): Long {
        // Estimación simple basada en número de entradas
        // En una implementación real, podrías usar reflection o serialización
        return cache.getStats().totalEntries * 1024L // ~1KB por entrada estimado
    }
    
    private fun findOldestEntry(): Long? {
        // En una implementación real, mantendrías un registro de timestamps
        return null
    }
    
    private fun findNewestEntry(): Long? {
        // En una implementación real, mantendrías un registro de timestamps
        return null
    }
}

/**
 *Resultado de limpieza automática
 */
data class CleanupResult(
    val entriesCleanedUp: Int,
    val memoryFreed: Long
)

/**
 * Resultado de una operación de refresh inteligente
 */
data class RefreshResult(
    val refreshedTypes: List<CacheStrategy.CacheType>,
    val totalEntriesBefore: Int,
    val expiredEntriesRemoved: Int
)

/**
 * Estadísticas detalladas del cache
 */
data class DetailedCacheStats(
    val totalEntries: Int,
    val entriesByType: Map<CacheStrategy.CacheType, Int>,
    val expiredEntries: Int,
    val memoryUsageEstimate: Long,
    val oldestEntry: Long?,
    val newestEntry: Long?
)