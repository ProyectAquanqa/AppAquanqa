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
    
    init {
        // Carga optimizada paralela con refresh inteligente
        loadDataOptimized()
    }
    
    /**
     * Refresca datos automáticamente cuando la app vuelve del background.
     * SIEMPRE intenta obtener datos frescos si hay internet.
     */
    fun onAppResumed() {
        viewModelScope.launch {
            // SIEMPRE intentar refresh para detectar contenido nuevo
            refreshData(forceRefresh = true)
        }
    }
    
    /**
     * Verifica si hay nuevos eventos disponibles y los carga.
     * Útil para detectar contenido nuevo después de agregar eventos.
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
     * ✅ Carga inicial de datos con cache inteligente
     */
    private fun loadDataOptimized() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // ✅ Cargar datos en paralelo
            coroutineScope {
                val userNameJob = async { loadUserName() }
                val categoriesJob = async { loadCategories() }
                
                // ✅ Esperar resultados
                userNameJob.await()
                val categoriesResult = categoriesJob.await()
                
                // ✅ Seleccionar categoría por defecto y cargar eventos
                if (categoriesResult is Result.Success && categoriesResult.data.isNotEmpty()) {
                    val defaultCategory = categoriesResult.data.find { it.isAllCategoriesOption() } 
                        ?: categoriesResult.data.first()
                    
                    _selectedCategory.value = defaultCategory
                    loadEventsForCategory(defaultCategory.nombre)
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * ✅ Carga nombre de usuario con fallback
     */
    private suspend fun loadUserName() {
        try {
            val firstName = repository.getUserFirstName().first()
            _userFirstName.value = firstName
            
            // ✅ Si no hay nombre válido, intentar actualizar desde API
            if (firstName == "Usuario") {
                repository.getUserProfile()
                _userFirstName.value = repository.getUserFirstName().first()
            }
        } catch (e: Exception) {
            _userFirstName.value = "Usuario"
        }
    }
    
    /**
     * ✅ Carga categorías con cache inteligente
     */
    private suspend fun loadCategories(forceRefresh: Boolean = false): Result<List<Category>> {
        _categoriesState.value = Result.Loading
        val result = repository.getCategories(forceRefresh)
        _categoriesState.value = result
        return result
    }
    
    /**
     * ✅ Carga eventos por categoría con manejo inteligente de cache
     */
    private fun loadEventsForCategory(categoryName: String?, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _eventsState.value = Result.Loading
            
            // ✅ Invalidar cache específico antes de cambiar categoría 
            if (forceRefresh) {
                repository.invalidateCache(categoryName = categoryName, invalidateEvents = true)
            }
            
            val eventsResult = repository.getFilteredEvents(
                categoryName = categoryName,
                page = 1,
                pageSize = 30,
                forceRefresh = forceRefresh
            )
            
            when (eventsResult) {
                is Result.Success -> {
                    _eventsState.value = Result.Success(eventsResult.data.first)
                }
                is Result.Error -> {
                    _eventsState.value = Result.Error(eventsResult.exception)
                }
                is Result.Loading -> {
                    _eventsState.value = Result.Loading
                }

                else -> { Log.w("Eventos", "Error de conexion")}
            }
        }
    }
    
    /**
     * ✅ Selección de categoría con cache invalidation para datos frescos
     */
    fun onCategorySelected(category: Category) {
        if (_selectedCategory.value?.id != category.id) {
            _selectedCategory.value = category
            // ✅ IMPORTANTE: Forzar refresh al cambiar categoría para evitar cache incorrecto
            loadEventsForCategory(category.nombre, forceRefresh = true)
        }
    }
    
    /**
     * ✅ Refresca todos los datos con invalidación de cache
     */
    fun refreshData(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // ✅ Smart refresh del repositorio
            repository.refreshAllData()
            
            // ✅ Recargar categorías
            loadCategories(forceRefresh)
            
            // ✅ Recargar eventos de la categoría actual
            val currentCategory = _selectedCategory.value
            loadEventsForCategory(currentCategory?.nombre, forceRefresh)
            
            _isLoading.value = false
        }
    }
    
    /**
     * ✅ Obtiene estadísticas del cache para debugging
     */
    fun getCacheStats() = repository.getCacheStats()
    
    /**
     * ✅ Invalida cache específico por categoría
     */
    fun invalidateCache(categoryName: String? = null) {
        viewModelScope.launch {
            repository.invalidateCache(categoryName = categoryName, invalidateEvents = true)
        }
    }
}

/**
 * Factory para crear instancias de HomeViewModel con dependencias manuales.
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