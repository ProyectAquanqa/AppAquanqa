package com.tecsup.aquanqa.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.model.Anuncio
import com.tecsup.aquanqa.data.model.Category
import com.tecsup.aquanqa.data.model.PaginationInfo
import com.tecsup.aquanqa.data.model.UserProfile
import com.tecsup.aquanqa.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel() {

    private val _categoriesState = MutableLiveData<Result<List<Category>>>()
    val categoriesState: LiveData<Result<List<Category>>> = _categoriesState
    
    private val _eventsState = MutableLiveData<Result<List<Anuncio>>>()
    val eventsState: LiveData<Result<List<Anuncio>>> = _eventsState
    
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()
    
    val userFirstName: LiveData<String> = liveData {
        repository.getUserFirstName().collect { emit(it) }
    }
    
    val currentDateSpanish: String get() = DateUtils.getCurrentDateInSpanish()
    
    init {
        loadAllData()
    }
    
    private fun loadAllData() {
        viewModelScope.launch {
            _categoriesState.value = Result.Loading
            val categoriesResult = repository.getCategories()
            _categoriesState.value = categoriesResult
            
            if (categoriesResult is Result.Success && categoriesResult.data.isNotEmpty()) {
                val todosCategory = categoriesResult.data.find { it.isAllCategoriesOption() } 
                    ?: categoriesResult.data.first()
                
                _selectedCategory.value = todosCategory
                loadEventsForCategory(todosCategory.nombre)
            }
        }
    }
    
    private fun loadEventsForCategory(categoryName: String?) {
        viewModelScope.launch {
            _eventsState.value = Result.Loading
            val eventsResult = repository.getFilteredEvents(
                categoryName = categoryName,
                page = 1,
                pageSize = 20
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
        }
    }
    
    fun onCategorySelected(category: Category) {
        if (_selectedCategory.value?.id != category.id) {
            _selectedCategory.value = category
            loadEventsForCategory(category.nombre)
        }
    }
    
    fun refreshData() {
        loadAllData()
    }
}