package com.tecsup.aquanqa.ui.home

import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.api.ApiService
import com.tecsup.aquanqa.data.model.Anuncio
import com.tecsup.aquanqa.data.model.Category
import com.tecsup.aquanqa.data.model.PaginatedResponse
import com.tecsup.aquanqa.data.model.PaginationInfo
import com.tecsup.aquanqa.data.model.UserProfile
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repositorio optimizado para la pantalla Home.
 * Maneja la obtención de datos con cache inteligente y llamadas optimizadas.
 * Reduce latencia y mejora la experiencia del usuario.
 */
class HomeRepository(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {
    
    // Cache mejorado con timestamp para invalidación automática
    private var cachedCategories: List<Category>? = null
    private var categoriesCacheTime: Long = 0
    private var cachedEvents: Pair<String?, List<Anuncio>>? = null
    private var eventsCacheTime: Long = 0
    private var currentPaginationInfo: PaginationInfo? = null
    
    companion object {
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutos
        private const val EVENTS_CACHE_DURATION_MS = 2 * 60 * 1000L // 2 minutos
    }
    
    /**
     * Obtiene el perfil del usuario desde preferencias locales.
     */
    suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val token = userPreferences.accessToken.first()
            if (token == null) {
                return Result.Error(Exception("Token de acceso no disponible"))
            }
            
            val response = apiService.getUserProfile("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val userProfile = response.body()!!
                
                userPreferences.saveUserProfile(
                    firstName = userProfile.first_name,
                    lastName = userProfile.last_name,
                    photoUrl = userProfile.foto_perfil
                )
                
                Result.Success(userProfile)
            } else {
                Result.Error(Exception("Error al obtener perfil del usuario: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Obtiene el nombre del usuario desde preferencias.
     */
    fun getUserFirstName(): Flow<String> = userPreferences.userFirstName.map { it ?: "Usuario" }
    
    /**
     * Obtiene categorías con cache inteligente y validación de tiempo.
     * Cache se invalida automáticamente después de 5 minutos.
     */
    suspend fun getCategories(): Result<List<Category>> {
        return try {
            // Verificar cache válido
            val currentTime = System.currentTimeMillis()
            if (cachedCategories != null && (currentTime - categoriesCacheTime) < CACHE_DURATION_MS) {
                return Result.Success(listOf(Category.createAllCategoriesOption()) + cachedCategories!!)
            }
            
            val token = userPreferences.accessToken.first()
            if (token == null) {
                return Result.Error(Exception("Token de acceso no disponible"))
            }
            
            val response = apiService.getCategories("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val categories = response.body()!!
                
                // Actualizar cache con timestamp
                cachedCategories = categories
                categoriesCacheTime = currentTime
                
                val allCategories = listOf(Category.createAllCategoriesOption()) + categories
                Result.Success(allCategories)
            } else {
                Result.Error(Exception("Error al obtener categorías: ${response.code()}"))
            }
        } catch (e: Exception) {
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
                Result.Error(Exception("Error al obtener eventos: ${response.code()}"))
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
                Result.Error(Exception("Error al obtener eventos por categoría: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Obtiene eventos filtrados con cache inteligente y optimización de llamadas.
     * Cache se invalida automáticamente después de 2 minutos.
     * 
     * @param categoryName Nombre de la categoría (null o "Todos" para obtener todos)
     * @param page Número de página
     * @param pageSize Elementos por página
     */
    suspend fun getFilteredEvents(
        categoryName: String? = null,
        page: Int = 1,
        pageSize: Int = 10
    ): Result<Pair<List<Anuncio>, PaginationInfo>> {
        return try {
            // Verificar cache válido para la misma categoría
            val currentTime = System.currentTimeMillis()
            val normalizedCategoryName = if (categoryName == Category.ALL_CATEGORIES_NAME) null else categoryName
            
            cachedEvents?.let { (cachedCategory, events) ->
                if (cachedCategory == normalizedCategoryName && 
                    (currentTime - eventsCacheTime) < EVENTS_CACHE_DURATION_MS) {
                    val paginationInfo = PaginationInfo(
                        count = events.size,
                        hasNext = false,
                        hasPrevious = false,
                        currentPage = page
                    )
                    return Result.Success(Pair(events, paginationInfo))
                }
            }
            
            val token = userPreferences.accessToken.first()
            if (token == null) {
                return Result.Error(Exception("Token de acceso no disponible"))
            }
            
            // Llamada optimizada al API
            val response = apiService.getEventosAndroid(
                token = "Bearer $token",
                categoriaNombre = normalizedCategoryName,
                ordering = "-created_at"
            )
            
            if (response.isSuccessful && response.body() != null) {
                val events = response.body()!!
                
                // Filtrado en cliente solo si es necesario
                val filteredEvents = if (normalizedCategoryName != null) {
                    val correctlyFiltered = events.all { evento ->
                        evento.categoria.nombre.equals(normalizedCategoryName, ignoreCase = true)
                    }
                    
                    if (!correctlyFiltered) {
                        events.filter { evento ->
                            evento.categoria.nombre.equals(normalizedCategoryName, ignoreCase = true)
                        }
                    } else {
                        events
                    }
                } else {
                    events
                }
                
                // Actualizar cache
                cachedEvents = Pair(normalizedCategoryName, filteredEvents)
                eventsCacheTime = currentTime
                
                val paginationInfo = PaginationInfo(
                    count = filteredEvents.size,
                    hasNext = false,
                    hasPrevious = false,
                    currentPage = page
                )
                
                currentPaginationInfo = paginationInfo
                Result.Success(Pair(filteredEvents, paginationInfo))
            } else {
                Result.Error(Exception("Error al obtener eventos: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(Exception("Error de red: ${e.message}", e))
        }
    }
    
    /**
     * Refresca todos los datos invalidando cache de forma inteligente.
     */
    suspend fun refreshAllData(): Result<Boolean> {
        return try {
            // Invalidar todos los caches
            cachedCategories = null
            categoriesCacheTime = 0
            cachedEvents = null
            eventsCacheTime = 0
            currentPaginationInfo = null
            
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Invalida solo el cache de eventos para una categoría específica.
     */
    fun invalidateEventsCache(categoryName: String? = null) {
        val normalizedCategoryName = if (categoryName == Category.ALL_CATEGORIES_NAME) null else categoryName
        cachedEvents?.let { (cachedCategory, _) ->
            if (cachedCategory == normalizedCategoryName) {
                cachedEvents = null
                eventsCacheTime = 0
            }
        }
    }
    
    /**
     * Obtiene la información de paginación actual.
     */
    fun getCurrentPaginationInfo(): PaginationInfo? = currentPaginationInfo
    
    /**
     * Verifica si hay datos en cache válidos.
     */
    fun hasCachedData(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (cachedCategories != null && (currentTime - categoriesCacheTime) < CACHE_DURATION_MS) ||
               (cachedEvents != null && (currentTime - eventsCacheTime) < EVENTS_CACHE_DURATION_MS)
    }
} 