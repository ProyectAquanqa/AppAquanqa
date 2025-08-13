package com.tecsup.aquanqa.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.model.content.Anuncio
import com.tecsup.aquanqa.data.model.content.Category
import com.tecsup.aquanqa.data.model.common.PaginationInfo
import com.tecsup.aquanqa.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class HomeViewModel(
    private val repository: com.tecsup.aquanqa.data.repository.HomeRepository
) : ViewModel() {

    // Estados combinados para mejor performance
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _categoriesState = MutableLiveData<Result<List<Category>>>()
    val categoriesState: LiveData<Result<List<Category>>> = _categoriesState
    
    private val _eventsState = MutableLiveData<Result<List<Anuncio>>>()
    val eventsState: LiveData<Result<List<Anuncio>>> = _eventsState
    
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()
    
    private val _userFirstName = MutableLiveData("Usuario")
    val userFirstName: LiveData<String> = _userFirstName
    
    val currentDateSpanish: String get() = DateUtils.getCurrentDateInSpanish()
    
    // Estados para paginación
    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore
    
    private val _allEvents = mutableListOf<Anuncio>()
    private var currentPage = 1
    private var hasMorePages = true
    private var isLoadingPage = false
    private val pageSize = 10
    
    init {
        // Carga optimizada paralela con refresh inteligente
        loadDataOptimized()
    }
    
    /**
     * Refresca datos de manera inteligente cuando la app vuelve del background.
     * Solo hace refresh si es realmente necesario para evitar modo offline.
     */
    fun onAppResumed() {
        viewModelScope.launch {
            // INTELIGENTE: Solo refresh si no hay datos o si han pasado más de 5 minutos
            val shouldRefresh = repository.shouldRefreshOnResume()
            
            if (shouldRefresh) {
                // Solo hacer refresh suave (no forzado) para mantener datos cached si falla la API
                refreshData(forceRefresh = false)
            } else {
                // Verificar si hay datos cached válidos y mostrarlos inmediatamente
                if (_eventsState.value !is Result.Success || _categoriesState.value !is Result.Success) {
                    loadDataOptimized()
                }
            }
        }
    }
    
    /**
     * Verifica si hay nuevos eventos disponibles y los carga.
     * Útil para detectar contenido nuevo después de agregar eventos
     */
    fun checkForNewEvents() {
        viewModelScope.launch {
            // Invalidar cache para forzar detección de nuevo contenido
            repository.invalidateCache(invalidateEvents = true)
            
            // Recargar eventos de la categoría actual
            val currentCategory = _selectedCategory.value
            loadEventsForCategory(currentCategory?.nombre, forceRefresh = true)
        }
    }
    
    /**
     *  Carga inicial de datos con cache inteligente y mejor manejo de categorías
     */
    private fun loadDataOptimized() {
        viewModelScope.launch {
            _isLoading.value = true
            
            //  Cargar datos en paralelo con mejor manejo de errores
            coroutineScope {
                val userNameJob = async { loadUserName() }
                val categoriesJob = async { loadCategories() }
                
                //  Esperar resultados de usuario (no bloquea si falla)
                userNameJob.await()
                
                //  Manejar categorías de manera inteligente
                val categoriesResult = categoriesJob.await()
                
                //  Seleccionar categoría "Todos" por defecto SIEMPRE
                if (categoriesResult is Result.Success && categoriesResult.data.isNotEmpty()) {
                    // Buscar específicamente la categoría "Todos" que se crea en el repositorio
                    val defaultCategory = categoriesResult.data.find { category ->
                        category.nombre == "Todos" || category.id == -1
                    } ?: categoriesResult.data.first() // Fallback al primer elemento si no encuentra "Todos"
                    
                    _selectedCategory.value = defaultCategory
                    // IMPORTANTE: Cargar eventos para "Todos" (que internamente maneja null)
                    val categoryNameForApi = if (defaultCategory.nombre == "Todos") null else defaultCategory.nombre
                    loadEventsForCategory(categoryNameForApi)
                } else {
                    // Si las categorías fallan, cargar eventos sin filtro (modo "Todos")
                    loadEventsForCategory(null)
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     *  Carga nombre de usuario con fallback
     */
    private suspend fun loadUserName() {
        try {
            val firstName = repository.getUserFirstName().first()
            _userFirstName.value = firstName
            
            //  Si no hay nombre válido, intentar actualizar desde API
            if (firstName == "Usuario") {
                repository.getUserProfile()
                _userFirstName.value = repository.getUserFirstName().first()
            }
        } catch (e: Exception) {
            _userFirstName.value = "Usuario"
        }
    }
    
    /**
     *  Carga categorías con cache inteligente
     */
    private suspend fun loadCategories(forceRefresh: Boolean = false): Result<List<Category>> {
        _categoriesState.value = Result.Loading
        val result = repository.getCategories(forceRefresh)
        _categoriesState.value = result
        
        // SWR: Si cargamos desde cache (forceRefresh=false) y fue exitoso, revalidar silenciosamente en background
        if (!forceRefresh && result is Result.Success) {
            revalidateCategoriesInBackground()
        }
        return result
    }
    
    /**
     *  Carga eventos por categoría con paginación incremental
     */
    private fun loadEventsForCategory(categoryName: String?, forceRefresh: Boolean = false, isLoadingMore: Boolean = false) {
        if (isLoadingPage) return // Evitar llamadas múltiples simultáneas
        
        viewModelScope.launch {
            isLoadingPage = true
            
            if (isLoadingMore) {
                _isLoadingMore.value = true
            } else {
                _eventsState.value = Result.Loading
                // Reiniciar paginación para nueva categoría o refresh
                currentPage = 1
                hasMorePages = true
                _allEvents.clear()
            }
            
            //  Invalidar cache específico antes de cambiar categoría 
            if (forceRefresh) {
                repository.invalidateCache(categoryName = categoryName, invalidateEvents = true)
            }
            
            val eventsResult = repository.getFilteredEvents(
                categoryName = categoryName,
                page = currentPage,
                pageSize = pageSize,
                forceRefresh = forceRefresh
            )
            
            when (eventsResult) {
                is Result.Success -> {
                    val (newEvents, paginationInfo) = eventsResult.data
                    
                    if (isLoadingMore) {
                        // Agregar nuevos eventos a la lista existente
                        _allEvents.addAll(newEvents)
                    } else {
                        // Primera carga o refresh completo
                        _allEvents.clear()
                        _allEvents.addAll(newEvents)
                    }
                    
                    // Actualizar estado de paginación
                    hasMorePages = paginationInfo.hasNext
                    if (hasMorePages) currentPage++
                    
                    // Emitir lista completa actualizada
                    _eventsState.value = Result.Success(_allEvents.toList())

                    // SWR: Si la carga no fue forzada (probable cache), revalidar en background
                    if (!forceRefresh && !isLoadingMore) {
                        revalidateEventsInBackground(categoryName)
                    }
                }
                is Result.Error -> {
                    if (isLoadingMore) {
                        // Si falla la carga de más elementos, mantener los existentes
                        Log.e("HomeViewModel", "Error loading more events: ${eventsResult.exception.message}")
                    } else {
                        _eventsState.value = Result.Error(eventsResult.exception)
                    }
                }
                is Result.Loading -> {
                    if (!isLoadingMore) {
                        _eventsState.value = Result.Loading
                    }
                }
                else -> { Log.w("Eventos", "Error de conexion")}
            }
            
            _isLoadingMore.value = false
            isLoadingPage = false
        }
    }

    /**
     * Revalida categorías silenciosamente en background (stale-while-revalidate)
     */
    private fun revalidateCategoriesInBackground() {
        viewModelScope.launch {
            try {
                val fresh = repository.getCategories(forceRefresh = true)
                if (fresh is Result.Success) {
                    val current = (_categoriesState.value as? Result.Success)?.data
                    if (current == null || current != fresh.data) {
                        _categoriesState.value = fresh
                    }
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * Revalida eventos silenciosamente en background (stale-while-revalidate)
     */
    private fun revalidateEventsInBackground(categoryName: String?) {
        viewModelScope.launch {
            try {
                val fresh = repository.getFilteredEvents(
                    categoryName = categoryName,
                    page = 1,
                    pageSize = pageSize,
                    forceRefresh = true
                )
                if (fresh is Result.Success) {
                    val (freshEvents, paginationInfo) = fresh.data
                    val current = (_eventsState.value as? Result.Success)?.data
                    if (current == null || current != freshEvents) {
                        _allEvents.clear()
                        _allEvents.addAll(freshEvents)
                        hasMorePages = paginationInfo.hasNext
                        currentPage = if (hasMorePages) 2 else 1
                        _eventsState.value = Result.Success(_allEvents.toList())
                    }
                }
            } catch (_: Exception) { }
        }
    }
    
    /**
     *  Selección de categoría con manejo inteligente de "Todos" y cache
     */
    fun onCategorySelected(category: Category) {
        if (_selectedCategory.value?.id != category.id) {
            _selectedCategory.value = category
            
            // IMPORTANTE: Convertir "Todos" a null para la API
            val categoryNameForApi = if (category.nombre == "Todos") null else category.nombre
            
            // Solo forzar refresh si realmente es necesario (evita modo offline)
            viewModelScope.launch {
                val forceRefresh = repository.shouldRefreshForCategory(categoryNameForApi)
                loadEventsForCategory(categoryNameForApi, forceRefresh = forceRefresh)
            }
        }
    }
    
    /**
     *  Refresca datos de manera inteligente basado en el tipo de refresh
     */
    fun refreshData(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                if (forceRefresh) {
                    //  Refresh completo: invalidar cache y recargar todo
                    repository.refreshAllData()
                    
                    //  Recargar categorías
                    loadCategories(forceRefresh = true)
                    
                    //  Recargar eventos de la categoría actual
                    val currentCategory = _selectedCategory.value
                    val categoryNameForApi = if (currentCategory?.nombre == "Todos") null else currentCategory?.nombre
                    loadEventsForCategory(categoryNameForApi, forceRefresh = true)
                } else {
                    //  Refresh suave: usar cache si está disponible, solo actualizar si es necesario
                    //  Intentar cargar categorías desde cache primero
                    val categoriesResult = loadCategories(forceRefresh = false)
                    
                    //  Solo recargar eventos si no tenemos datos válidos
                    if (_eventsState.value !is Result.Success) {
                        val currentCategory = _selectedCategory.value
                        val categoryNameForApi = if (currentCategory?.nombre == "Todos") null else currentCategory?.nombre
                        loadEventsForCategory(categoryNameForApi, forceRefresh = false)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error during refresh: ${e.message}")
                // En caso de error, intentar cargar datos cached
                if (_eventsState.value !is Result.Success || _categoriesState.value !is Result.Success) {
                    loadDataOptimized()
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     *  Obtiene estadísticas del cache para debugging
     */
    fun getCacheStats() = repository.getCacheStats()
    
    /**
     *  Invalida cache específico por categoría
     */
    fun invalidateCache(categoryName: String? = null) {
        viewModelScope.launch {
            repository.invalidateCache(categoryName = categoryName, invalidateEvents = true)
        }
    }
    
    /**
     * Carga la siguiente página de eventos para infinite scroll
     */
    fun loadMoreEvents() {
        if (!hasMorePages || isLoadingPage) return
        
        val currentCategory = _selectedCategory.value
        loadEventsForCategory(currentCategory?.nombre, forceRefresh = false, isLoadingMore = true)
    }
    
    /**
     * Verifica si se pueden cargar más eventos
     */
    fun canLoadMore(): Boolean = hasMorePages && !isLoadingPage
}

/**
 * Factory para crear instancias de HomeViewModel con dependencias manuales
 */
class HomeViewModelFactory(
    private val repository: com.tecsup.aquanqa.data.repository.HomeRepository
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}