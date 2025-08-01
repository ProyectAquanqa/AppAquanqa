package com.tecsup.aquanqa.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.model.Anuncio
import com.tecsup.aquanqa.data.model.Category
import com.tecsup.aquanqa.data.model.PaginationInfo
import com.tecsup.aquanqa.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class HomeViewModel(
    private val repository: HomeRepository
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
        // Carga optimizada paralela
        loadDataOptimized()
    }
    
    /**
     * Carga optimizada de datos en paralelo para mejor performance
     */
    private fun loadDataOptimized() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                coroutineScope {
                    // Cargar datos en paralelo para velocidad máxima
                    val userNameDeferred = async { loadUserName() }
                    val categoriesDeferred = async { loadCategories() }
                    
                    // Esperar ambas operaciones
                    userNameDeferred.await()
                    val categoriesResult = categoriesDeferred.await()
                    
                    // Solo cargar eventos si las categorías se cargaron correctamente
                    if (categoriesResult is Result.Success && categoriesResult.data.isNotEmpty()) {
                        val defaultCategory = categoriesResult.data.find { it.isAllCategoriesOption() } 
                            ?: categoriesResult.data.first()
                        
                        _selectedCategory.value = defaultCategory
                        loadEventsForCategory(defaultCategory.nombre)
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Carga rápida del nombre de usuario con cache
     */
    private suspend fun loadUserName() {
        try {
            val firstName = repository.getUserFirstName().first()
            _userFirstName.value = firstName
            
            // Solo hacer llamada al API si no tenemos nombre válido
            if (firstName == "Usuario") {
                repository.getUserProfile()
                // Reintenta obtener el nombre actualizado
                val updatedName = repository.getUserFirstName().first()
                _userFirstName.value = updatedName
            }
        } catch (e: Exception) {
            // Manejar error silenciosamente para no bloquear la UI
            _userFirstName.value = "Usuario"
        }
    }
    
    /**
     * Carga optimizada de categorías con mejor manejo de errores
     */
    private suspend fun loadCategories(): Result<List<Category>> {
        _categoriesState.value = Result.Loading
        return try {
            val result = repository.getCategories()
            _categoriesState.value = result
            result
        } catch (e: Exception) {
            val errorResult = Result.Error(e)
            _categoriesState.value = errorResult
            errorResult
        }
    }
    
    /**
     * Carga optimizada de eventos con mejor manejo de errores y cache
     */
    private fun loadEventsForCategory(categoryName: String?) {
        viewModelScope.launch {
            try {
                _eventsState.value = Result.Loading
                
                val eventsResult = repository.getFilteredEvents(
                    categoryName = categoryName,
                    page = 1,
                    pageSize = 30 // Más elementos para menos llamadas
                )
                
                when (eventsResult) {
                    is Result.Success -> {
                        val events = eventsResult.data.first
                        _eventsState.value = Result.Success(events)
                    }
                    is Result.Error -> {
                        _eventsState.value = eventsResult
                    }
                    is Result.Loading -> {
                        _eventsState.value = eventsResult
                    }
                }
            } catch (e: Exception) {
                _eventsState.value = Result.Error(e)
            }
        }
    }
    
    /**
     * Selección optimizada de categoría con prevención de cargas duplicadas
     */
    fun onCategorySelected(category: Category) {
        if (_selectedCategory.value?.id != category.id) {
            _selectedCategory.value = category
            loadEventsForCategory(category.nombre)
        }
    }
    
    /**
     * Refresh optimizado que conserva datos válidos durante la carga
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                // Refrescar cache del repositorio
                repository.refreshAllData()
                
                // Recargar solo categorías, los eventos se recargarán automáticamente
                val categoriesResult = loadCategories()
                
                if (categoriesResult is Result.Success && categoriesResult.data.isNotEmpty()) {
                    // Mantener categoría seleccionada si sigue existiendo
                    val currentCategory = _selectedCategory.value
                    val updatedCategory = if (currentCategory != null) {
                        categoriesResult.data.find { it.id == currentCategory.id }
                            ?: categoriesResult.data.find { it.isAllCategoriesOption() }
                            ?: categoriesResult.data.first()
                    } else {
                        categoriesResult.data.find { it.isAllCategoriesOption() }
                            ?: categoriesResult.data.first()
                    }
                    
                    _selectedCategory.value = updatedCategory
                    loadEventsForCategory(updatedCategory.nombre)
                }
            } catch (e: Exception) {
                // Error silencioso para no interrumpir la experiencia del usuario
            }
        }
    }
}