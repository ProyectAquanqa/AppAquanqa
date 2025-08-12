package com.tecsup.aquanqa.ui.lunch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.data.model.content.Almuerzo
import com.tecsup.aquanqa.data.repository.LunchRepository
import com.tecsup.aquanqa.data.Result as DataResult
import kotlinx.coroutines.launch

/**
 * ViewModel optimizado para gestión de almuerzos.
 * 
 * Maneja estados de UI de manera clara y eficiente con mejor UX.
 */
class LunchViewModel(
    private val repository: LunchRepository
) : ViewModel() {

    // Estados de UI consolidados
    private val _uiState = MutableLiveData<LunchUiState>(LunchUiState.Idle)
    val uiState: LiveData<LunchUiState> = _uiState

    // Lista de almuerzos
    private val _almuerzos = MutableLiveData<List<Almuerzo>>(emptyList())
    val almuerzos: LiveData<List<Almuerzo>> = _almuerzos

    init {
        //  Cargar datos inmediatamente al crear el ViewModel
        loadAlmuerzos()
    }

    /**
     * Carga almuerzos con estado de UI optimizado.
     * 
     * @param forceRefresh Si true, ignora cache
     */
    fun loadAlmuerzos(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = LunchUiState.Loading
            
            when (val result = repository.getAlmuerzos(forceRefresh)) {
                is DataResult.Success -> {
                    val almuerzosList = result.data
                    _almuerzos.value = almuerzosList
                    
                    _uiState.value = if (almuerzosList.isEmpty()) {
                        LunchUiState.Empty
                    } else {
                        LunchUiState.Success
                    }
                }
                is DataResult.Error -> {
                    _uiState.value = LunchUiState.Error(result.exception.message ?: "Error desconocido")
                    _almuerzos.value = emptyList()
                }
                else -> {
                    _uiState.value = LunchUiState.Error("Estado no reconocido")
                    _almuerzos.value = emptyList()
                }
            }
        }
    }

    /**
     * Refresca datos desde la API.
     */
    fun refreshAlmuerzos() {
        loadAlmuerzos(forceRefresh = true)
    }

    /**
     * Maneja evento cuando la app vuelve del background.
     * Solo refresca si ya hay datos cargados para evitar doble carga inicial.
     */
    fun onAppResumed() {
        //  Solo refrescar si ya hay datos, evitar doble carga inicial
        if (_almuerzos.value?.isNotEmpty() == true) {
            refreshAlmuerzos()
        }
    }

    /**
     * Busca nuevo contenido limpiando cache.
     */
    fun checkForNewContent() {
        viewModelScope.launch {
            repository.clearCache()
            loadAlmuerzos(forceRefresh = true)
        }
    }

    /**
     * Limpia errores para permitir retry.
     */
    fun clearError() {
        if (_uiState.value is LunchUiState.Error) {
            _uiState.value = LunchUiState.Idle
        }
    }

    /**
     * Obtiene almuerzo por ID de manera eficiente.
     */
    fun getAlmuerzoById(almuerzoId: Int): Almuerzo? {
        return _almuerzos.value?.find { it.id == almuerzoId }
    }

    /**
     * Estados de UI consolidados para mejor manejo.
     */
    sealed class LunchUiState {
        object Idle : LunchUiState()
        object Loading : LunchUiState()
        object Success : LunchUiState()
        object Empty : LunchUiState()
        data class Error(val message: String) : LunchUiState()
    }
}