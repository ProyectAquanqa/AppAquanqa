package com.tecsup.aquanqa.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.api.ApiService
import com.tecsup.aquanqa.data.cache.CacheManager
import com.tecsup.aquanqa.data.cache.CacheResult
import com.tecsup.aquanqa.data.cache.DataSource
import com.tecsup.aquanqa.data.model.content.Anuncio
import com.tecsup.aquanqa.data.model.content.Category
import com.tecsup.aquanqa.data.model.content.EventoBasico
import com.tecsup.aquanqa.data.model.content.toEventoBasico
import com.tecsup.aquanqa.data.model.common.PaginatedResponse
import com.tecsup.aquanqa.data.model.common.PaginationInfo
import com.tecsup.aquanqa.data.model.user.UserProfile
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repositorio refactorizado con cache strategy inteligente.
 * Implementa mejores prácticas de cache con duraciones específicas,
 * cache por usuario y invalidación inteligente.
 */
class HomeRepository(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences,
    private val cacheManager: CacheManager = CacheManager()
) {
    
    // Información de paginación actual (no cacheada, es temporal)
    private var currentPaginationInfo: PaginationInfo? = null
    
    companion object {
        private const val TAG = "HomeRepository"
    }
    
    private val gson = Gson()
    
    /**
     * Obtiene el perfil del usuario con cache inteligente.
     * Cache duration: 15 minutos (datos de perfil cambian poco)
     */
    suspend fun getUserProfile(forceRefresh: Boolean = false): Result<UserProfile> {
        return try {
            val token = userPreferences.accessToken.first()
            if (token == null) {
                return Result.Error(Exception("Token de acceso no disponible"))
            }
            
            val userId = getCurrentUserId()
            
            // Verificar cache primero (si no es refresh forzado)
            if (!forceRefresh) {
                when (val cacheResult = cacheManager.getCachedUserProfile("user_profile", userId)) {
                    is CacheResult.Hit -> {
                        Log.d(TAG, "User profile from cache (${cacheResult.source})")
                        return Result.Success(cacheResult.data)
                    }
                    is CacheResult.Expired -> {
                        Log.d(TAG, "User profile cache expired, fetching fresh data")
                        // Continúa para obtener datos frescos, pero mantiene expired como fallback
                    }
                    is CacheResult.Miss -> {
                        Log.d(TAG, "User profile cache miss, fetching from API")
                    }
                }
            }
            
            // Obtener datos frescos de la API
            val response = apiService.getUserProfile("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val userProfile = response.body()!!
                
                // Guardar en preferencias (como antes)
                userPreferences.saveUserProfile(
                    firstName = userProfile.first_name,
                    lastName = userProfile.last_name,
                    photoUrl = userProfile.foto_perfil
                )
                
                // Guardar en cache inteligente
                cacheManager.cacheUserProfile("user_profile", userProfile, userId)
                
                Log.d(TAG, "User profile updated from API and cached")
                Result.Success(userProfile)
            } else {
                // Si hay datos expirados en cache, usarlos como fallback
                when (val cacheResult = cacheManager.getCachedUserProfile("user_profile", userId)) {
                    is CacheResult.Expired -> {
                        Log.w(TAG, "API failed, using expired cache as fallback")
                        Result.Success(cacheResult.data)
                    }
                    else -> Result.Error(Exception("No se pudo cargar la información del usuario"))
                }
            }
        } catch (e: Exception) {
            // Intentar usar cache expirado como último recurso
            val userId = getCurrentUserId()
            when (val cacheResult = cacheManager.getCachedUserProfile("user_profile", userId)) {
                is CacheResult.Expired -> {
                    Log.w(TAG, "Network error, using expired cache as fallback: ${e.message}")
                    Result.Success(cacheResult.data)
                }
                else -> Result.Error(e)
            }
        }
    }
    
    /**
     * Obtiene el nombre del usuario desde preferencias.
     */
    fun getUserFirstName(): Flow<String> = userPreferences.userFirstName.map { it ?: "Usuario" }
    
    /**
     * Obtiene categorías con cache persistente en DataStore.
     * Cache duration: 24 horas (categorías cambian muy poco)
     */
    suspend fun getCategories(forceRefresh: Boolean = false): Result<List<Category>> {
        return try {
            val token = userPreferences.accessToken.first()
            if (token == null) {
                return Result.Error(Exception("Token de acceso no disponible"))
            }
            
            // 1. VERIFICAR CACHE PERSISTENTE (DataStore)
            if (!forceRefresh) {
                val cachedCategoriesJson = userPreferences.cachedCategories.first()
                val isCacheExpired = userPreferences.isCategoriesCacheExpired()
                
                if (cachedCategoriesJson != null && !isCacheExpired) {
                    // Cache válido - devolver inmediatamente
                    val cachedCategories = deserializeCategories(cachedCategoriesJson)
                    if (cachedCategories != null) {
                        val allCategories = listOf(Category.createAllCategoriesOption()) + cachedCategories
                        Log.d(TAG, "Categories from DataStore cache: ${cachedCategories.size} items")
                        return Result.Success(allCategories)
                    }
                }
            }
            
            // 2. LLAMAR API PARA DATOS FRESCOS
            val response = apiService.getCategories("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val categories = response.body()!!
                
                // 3. GUARDAR EN DATASTORE
                val categoriesJson = serializeCategories(categories)
                userPreferences.saveCategories(categoriesJson)
                
                val allCategories = listOf(Category.createAllCategoriesOption()) + categories
                Log.d(TAG, "Categories updated from API and cached in DataStore: ${categories.size} items")
                Result.Success(allCategories)
            } else {
                // 4. API FALLÓ - USAR CACHE EXPIRADO COMO FALLBACK
                val cachedCategoriesJson = userPreferences.cachedCategories.first()
                if (cachedCategoriesJson != null) {
                    val cachedCategories = deserializeCategories(cachedCategoriesJson)
                    if (cachedCategories != null) {
                        val allCategories = listOf(Category.createAllCategoriesOption()) + cachedCategories
                        Log.w(TAG, "API failed, using expired DataStore cache as fallback: ${cachedCategories.size} items")
                        return Result.Success(allCategories)
                    }
                }
                
                Result.Error(Exception("No se pudieron cargar las categorías"))
            }
        } catch (e: Exception) {
            // 5. ERROR DE RED - USAR CACHE COMO ÚLTIMO RECURSO
            try {
                val cachedCategoriesJson = userPreferences.cachedCategories.first()
                if (cachedCategoriesJson != null) {
                    val cachedCategories = deserializeCategories(cachedCategoriesJson)
                    if (cachedCategories != null) {
                        val allCategories = listOf(Category.createAllCategoriesOption()) + cachedCategories
                        Log.w(TAG, "Network error, using DataStore cache as fallback: ${e.message}")
                        return Result.Success(allCategories)
                    }
                }
            } catch (cacheException: Exception) {
                Log.e(TAG, "Error accessing categories cache: ${cacheException.message}")
            }
            
            Result.Error(e)
        }
    }
    
    /**
     * Obtiene eventos paginados del feed público.
     * Perfecto para implementar infinite scroll.
     * 
     * @param page Número de página a obtener (empezando desde 1)
     * @param pageSize Cantidad de elementos por página
     * @param reset Si es true, reinicia la paginación desde la página 1
     */
    suspend fun getAllEvents(
        page: Int = 1, 
        pageSize: Int = 10,
        reset: Boolean = false
    ): Result<Pair<List<Anuncio>, PaginationInfo>> {
        return try {
            val response = apiService.getAllEvents(page, pageSize)
            if (response.isSuccessful && response.body() != null) {
                val paginatedResponse = response.body()!!
                val paginationInfo = PaginationInfo.from(paginatedResponse).copy(currentPage = page)
                
                currentPaginationInfo = paginationInfo
                
                Result.Success(Pair(paginatedResponse.results, paginationInfo))
            } else {
                Result.Error(Exception("No se pudieron cargar los eventos"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Obtiene eventos filtrados por categoría con paginación.
     * 
     * @param categoryName Nombre de la categoría para filtrar
     * @param page Número de página a obtener
     * @param pageSize Cantidad de elementos por página
     */
    suspend fun getEventsByCategory(
        categoryName: String,
        page: Int = 1,
        pageSize: Int = 10
    ): Result<Pair<List<Anuncio>, PaginationInfo>> {
        return try {
            val token = userPreferences.accessToken.first()
            if (token == null) {
                return Result.Error(Exception("No hay token de autenticación disponible"))
            }
            
            val response = apiService.getEventsByCategory("Bearer $token", categoryName, page, pageSize)
            if (response.isSuccessful && response.body() != null) {
                val paginatedResponse = response.body()!!
                val paginationInfo = PaginationInfo.from(paginatedResponse).copy(currentPage = page)
                
                currentPaginationInfo = paginationInfo
                
                Result.Success(Pair(paginatedResponse.results, paginationInfo))
            } else {
                Result.Error(Exception("No se pudieron cargar los eventos de esta categoría"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Obtiene eventos con cache híbrido inteligente.
     * Memoria (rápido) + DataStore fallback (persistente)
     * 
     * @param categoryName Nombre de la categoría (null o "Todos" para obtener todos)
     * @param page Número de página
     * @param pageSize Elementos por página
     * @param forceRefresh Forzar actualización desde API
     */
    suspend fun getFilteredEvents(
        categoryName: String? = null,
        page: Int = 1,
        pageSize: Int = 10,
        forceRefresh: Boolean = false
    ): Result<Pair<List<Anuncio>, PaginationInfo>> {
        return try {
            val token = userPreferences.accessToken.first()
            if (token == null) {
                return Result.Error(Exception("Token de acceso no disponible"))
            }
            
            val userId = getCurrentUserId()
            val normalizedCategoryName = if (categoryName == Category.ALL_CATEGORIES_NAME) null else categoryName
            
            // 1. VERIFICAR CACHE DE MEMORIA (si no es refresh forzado)
            if (!forceRefresh) {
                val memoryCacheResult = if (normalizedCategoryName != null) {
                    cacheManager.getCachedEventsByCategory(normalizedCategoryName, userId)
                } else {
                    cacheManager.getCachedEventsWithFallback(userId)
                }
                
                when (memoryCacheResult) {
                    is CacheResult.Hit -> {
                        val paginationInfo = createPaginationInfo(memoryCacheResult.data, page)
                        Log.d(TAG, "Events from memory cache: ${memoryCacheResult.data.size} items")
                        return Result.Success(Pair(memoryCacheResult.data, paginationInfo))
                    }
                    is CacheResult.Miss -> {
                        // Cache de memoria vacío, intentar fallback de DataStore
                        val fallbackResult = getEventsFallbackFromDataStore()
                        if (fallbackResult != null) {
                            val paginationInfo = createPaginationInfo(fallbackResult, page)
                            Log.d(TAG, "Events from DataStore fallback: ${fallbackResult.size} items")
                            return Result.Success(Pair(fallbackResult, paginationInfo))
                        }
                    }
                    else -> {
                        Log.d(TAG, "Memory cache expired, fetching from API")
                    }
                }
            }
            
            // 2. LLAMAR API PARA DATOS FRESCOS
            val response = apiService.getEventosAndroid(
                token = "Bearer $token",
                categoriaNombre = normalizedCategoryName,
                ordering = "-created_at"
            )
            
            if (response.isSuccessful && response.body() != null) {
                val events = response.body()!!
                
                // Filtrado en cliente si es necesario
                val filteredEvents = if (normalizedCategoryName != null) {
                    events.filter { evento ->
                        evento.categoria.nombre.equals(normalizedCategoryName, ignoreCase = true)
                    }
                } else {
                    events
                }
                
                // 3. GUARDAR EN CACHE HÍBRIDO
                // Cache de memoria (rápido)
                if (normalizedCategoryName != null) {
                    cacheManager.cacheEventsByCategory(normalizedCategoryName, filteredEvents, userId)
                } else {
                    cacheManager.cacheEventsWithFallback(filteredEvents, userId)
                }
                
                // Fallback en DataStore (persistente)
                saveEventsFallbackToDataStore(filteredEvents)
                
                val paginationInfo = createPaginationInfo(filteredEvents, page)
                currentPaginationInfo = paginationInfo
                
                Log.d(TAG, "Events updated from API with hybrid cache: ${filteredEvents.size} items")
                Result.Success(Pair(filteredEvents, paginationInfo))
            } else {
                // 4. API FALLÓ - USAR FALLBACKS
                // Primero intentar cache de memoria expirado
                val memoryCacheResult = if (normalizedCategoryName != null) {
                    cacheManager.getCachedEventsByCategory(normalizedCategoryName, userId)
                } else {
                    cacheManager.getCachedEvents(userId)
                }
                
                when (memoryCacheResult) {
                    is CacheResult.Expired -> {
                        val paginationInfo = createPaginationInfo(memoryCacheResult.data, page)
                        Log.w(TAG, "API failed, using expired memory cache as fallback")
                        return Result.Success(Pair(memoryCacheResult.data, paginationInfo))
                    }
                    else -> {
                        // Intentar fallback de DataStore
                        val fallbackResult = getEventsFallbackFromDataStore()
                        if (fallbackResult != null) {
                            val paginationInfo = createPaginationInfo(fallbackResult, page)
                            Log.w(TAG, "API failed, using DataStore fallback: ${fallbackResult.size} items")
                            return Result.Success(Pair(fallbackResult, paginationInfo))
                        }
                    }
                }
                
                Result.Error(Exception("No se pudieron cargar más eventos"))
            }
        } catch (e: Exception) {
            // 5. ERROR DE RED - USAR TODOS LOS FALLBACKS DISPONIBLES
            Log.w(TAG, "Network error, trying all fallbacks: ${e.message}")
            
            // Intentar fallback de DataStore
            val fallbackResult = getEventsFallbackFromDataStore()
            if (fallbackResult != null) {
                val paginationInfo = createPaginationInfo(fallbackResult, page)
                Log.w(TAG, "Network error, using DataStore fallback: ${fallbackResult.size} items")
                return Result.Success(Pair(fallbackResult, paginationInfo))
            }
            
            Result.Error(Exception("Error de red: ${e.message}", e))
        }
    }
    
    /**
     * Refresca todos los datos con invalidación inteligente de cache.
     * Utiliza el smart refresh del cache manager.
     */
    suspend fun refreshAllData(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            
            // Usar smart refresh del cache manager
            val refreshResult = cacheManager.smartRefresh(userId)
            
            // Limpiar paginación temporal
            currentPaginationInfo = null
            
            Log.d(TAG, "Smart refresh completed: ${refreshResult.refreshedTypes.size} types refreshed")
            Result.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error during refresh: ${e.message}")
            Result.Error(e)
        }
    }
    
    /**
     * Invalida cache específico por tipo y categoría.
     */
    suspend fun invalidateCache(
        categoryName: String? = null,
        invalidateCategories: Boolean = false,
        invalidateEvents: Boolean = true
    ) {
        val userId = getCurrentUserId()
        
        if (invalidateCategories) {
            cacheManager.invalidateCategories(userId)
            Log.d(TAG, "Categories cache invalidated for user: $userId")
        }
        
        if (invalidateEvents) {
            if (categoryName != null && categoryName != Category.ALL_CATEGORIES_NAME) {
                // Invalidar solo eventos de una categoría específica
                // Nota: El cache manager maneja esto internamente
                cacheManager.invalidateEvents(userId)
                Log.d(TAG, "Events cache invalidated for category: $categoryName")
            } else {
                // Invalidar todos los eventos
                cacheManager.invalidateEvents(userId)
                Log.d(TAG, "All events cache invalidated for user: $userId")
            }
        }
    }
    
    /**
     * Obtiene estadísticas detalladas del cache.
     */
    fun getCacheStats() = cacheManager.getDetailedStats()
    
    /**
     * Observa cambios en el cache para debugging o analytics.
     */
    fun observeCacheUpdates() = cacheManager.observeCacheUpdates()
    
    /**
     * Obtiene la información de paginación actual.
     */
    fun getCurrentPaginationInfo(): PaginationInfo? = currentPaginationInfo
    
    /**
     * Verifica si hay datos válidos en cache.
     */
    suspend fun hasCachedData(): Boolean {
        val userId = getCurrentUserId()
        val stats = cacheManager.getDetailedStats()
        return stats.totalEntries > 0
    }
    
    // ================= MÉTODOS PRIVADOS AUXILIARES =================
    
    /**
     * Obtiene el ID del usuario actual desde las preferencias.
     * Fallback a "anonymous" si no hay usuario autenticado.
     */
    private suspend fun getCurrentUserId(): String {
        return try {
            userPreferences.userDni.first() ?: "anonymous"
        } catch (e: Exception) {
            Log.w(TAG, "Could not get user ID, using anonymous: ${e.message}")
            "anonymous"
        }
    }
    
    /**
     * Crea información de paginación basada en los datos actuales.
     */
    private fun createPaginationInfo(events: List<Anuncio>, currentPage: Int): PaginationInfo {
        return PaginationInfo(
            count = events.size,
            hasNext = false, // Para cache local, asumimos que tenemos todos los datos
            hasPrevious = false,
            currentPage = currentPage
        )
    }

    // ================= MÉTODOS PRIVADOS DE SERIALIZACIÓN =================

    /**
     * Serializa categorías para DataStore.
     */
    private fun serializeCategories(categories: List<Category>): String {
        return try {
            gson.toJson(categories)
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing categories: ${e.message}")
            "[]"
        }
    }

    /**
     * Deserializa categorías desde DataStore.
     */
    private fun deserializeCategories(json: String): List<Category>? {
        return try {
            val type = object : TypeToken<List<Category>>() {}.type
            gson.fromJson<List<Category>>(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing categories: ${e.message}")
            null
        }
    }

    /**
     * Guarda fallback de eventos en DataStore.
     */
    private suspend fun saveEventsFallbackToDataStore(events: List<Anuncio>) {
        try {
            val eventosBasicos = events.map { it.toEventoBasico() }
            val json = gson.toJson(eventosBasicos)
            userPreferences.saveEventsFallback(json)
            Log.d(TAG, "Events fallback saved to DataStore: ${eventosBasicos.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving events fallback: ${e.message}")
        }
    }

    /**
     * Obtiene fallback de eventos desde DataStore.
     */
    private suspend fun getEventsFallbackFromDataStore(): List<Anuncio>? {
        return try {
            val fallbackJson = userPreferences.cachedEventsFallback.first()
            val isExpired = userPreferences.isEventsFallbackExpired()
            
            if (fallbackJson != null && !isExpired) {
                val type = object : TypeToken<List<EventoBasico>>() {}.type
                val eventosBasicos = gson.fromJson<List<EventoBasico>>(fallbackJson, type)
                val eventos = eventosBasicos.map { it.toAnuncio() }
                Log.d(TAG, "Events fallback loaded from DataStore: ${eventos.size} items")
                eventos
            } else {
                Log.d(TAG, "Events fallback expired or not available")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading events fallback: ${e.message}")
            null
        }
    }
} 