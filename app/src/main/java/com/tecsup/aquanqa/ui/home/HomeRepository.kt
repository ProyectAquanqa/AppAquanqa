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
 * Repositorio para la pantalla Home.
 * Maneja la obtención de datos desde la API y preferencias locales.
 * Incluye soporte para paginación e infinite scroll.
 */
class HomeRepository(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {
    
    private var cachedCategories: List<Category>? = null
    private var currentPaginationInfo: PaginationInfo? = null
    
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
     * Obtiene todas las categorías disponibles.
     * Incluye cache en memoria y la categoría "Todos".
     */
    suspend fun getCategories(): Result<List<Category>> {
        return try {
            cachedCategories?.let { 
                return Result.Success(listOf(Category.createAllCategoriesOption()) + it)
            }
            
            val token = userPreferences.accessToken.first()
            if (token == null) {
                return Result.Error(Exception("Token de acceso no disponible"))
            }
            
            val response = apiService.getCategories("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val categories = response.body()!!
                cachedCategories = categories
                
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
     * Obtiene eventos filtrados con paginación.
     * Usa el endpoint específico de Android con filtrado robusto.
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
            val token = userPreferences.accessToken.first()
            if (token == null) {
                return Result.Error(Exception("Token de acceso no disponible"))
            }
            
            val response = if (categoryName == null || categoryName == Category.ALL_CATEGORIES_NAME) {
                apiService.getEventosAndroid(
                    token = "Bearer $token",
                    categoriaNombre = null,
                    ordering = "-created_at"
                )
            } else {
                apiService.getEventosAndroid(
                    token = "Bearer $token",
                    categoriaNombre = categoryName,
                    ordering = "-created_at"
                )
            }
            
            if (response.isSuccessful && response.body() != null) {
                val events = response.body()!!
                
                if (categoryName != null && categoryName != Category.ALL_CATEGORIES_NAME) {
                    val correctlyFiltered = events.all { evento ->
                        evento.categoria.nombre.equals(categoryName, ignoreCase = true)
                    }
                    
                    if (!correctlyFiltered && events.isNotEmpty()) {
                        val clientFiltered = events.filter { evento ->
                            evento.categoria.nombre.equals(categoryName, ignoreCase = true)
                        }
                        
                        val paginationInfo = PaginationInfo(
                            count = clientFiltered.size,
                            hasNext = false,
                            hasPrevious = false,
                            currentPage = page
                        )
                        
                        return Result.Success(Pair(clientFiltered, paginationInfo))
                    }
                }
                
                val paginationInfo = PaginationInfo(
                    count = events.size,
                    hasNext = false,
                    hasPrevious = false,
                    currentPage = page
                )
                
                currentPaginationInfo = paginationInfo
                Result.Success(Pair(events, paginationInfo))
            } else {
                Result.Error(Exception("Error al obtener eventos: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(Exception("Error de red: ${e.message}", e))
        }
    }
    
    /**
     * Refresca todos los datos eliminando cache.
     */
    suspend fun refreshAllData(): Result<Boolean> {
        return try {
            cachedCategories = null
            currentPaginationInfo = null
            
            val categoriesResult = getCategories()
            if (categoriesResult is Result.Error) {
                return Result.Error(Exception("Error al refrescar categorías: ${categoriesResult.exception.message}"))
            }
            
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Obtiene la información de paginación actual.
     */
    fun getCurrentPaginationInfo(): PaginationInfo? = currentPaginationInfo
} 