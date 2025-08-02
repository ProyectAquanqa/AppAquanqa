package com.tecsup.aquanqa.ui.lunch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.data.Result
import kotlinx.coroutines.launch

/**
 * ViewModel para el fragment de almuerzos del comedor de Tecsup.
 * 
 * Implementa el patrón MVVM manejando la lógica de negocio y el estado de la UI.
 * Se comunica con el LunchRepository para obtener los datos desde la API,
 * filtrando automáticamente los días feriados para mostrar solo días laborables.
 * 
 * Estados manejados:
 * - Loading: Mientras se cargan los datos
 * - Success: Datos cargados exitosamente
 * - Error: Error al cargar los datos
 * - Empty: No hay almuerzos disponibles
 * 
 * @property repository Repositorio para acceder a los datos de almuerzos
 */
class LunchViewModel(
    private val repository: LunchRepository
) : ViewModel() {

    // LiveData para la lista de almuerzos
    private val _almuerzos = MutableLiveData<List<Almuerzo>>()
    val almuerzos: LiveData<List<Almuerzo>> = _almuerzos

    // LiveData para el estado de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData para errores
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // LiveData para estado vacío
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    init {
        // Cargar almuerzos al inicializar el ViewModel
        loadAlmuerzos()
    }

    /**
     * Carga la lista de almuerzos desde la API.
     * 
     * Maneja todos los estados posibles: loading, success, error y empty.
     * Los datos se filtran automáticamente para excluir días feriados.
     */
    fun loadAlmuerzos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            when (val result = repository.getAlmuerzos()) {
                is Result.Success -> {
                    val almuerzosList = result.data
                    _almuerzos.value = almuerzosList
                    _isEmpty.value = almuerzosList.isEmpty()
                    
                    // Log para debugging
                    println("LunchViewModel: Cargados ${almuerzosList.size} almuerzos")
                }
                is Result.Error -> {
                    _error.value = "Error al cargar los almuerzos: ${result.exception.message}"
                    _almuerzos.value = emptyList()
                    _isEmpty.value = true
                    
                    // Log del error
                    println("LunchViewModel Error: ${result.exception.message}")
                }
                else -> {
                    _error.value = "Estado no manejado en la carga de almuerzos"
                    _almuerzos.value = emptyList()
                    _isEmpty.value = true
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Refresca la lista de almuerzos desde la API.
     * 
     * Útil para implementar pull-to-refresh en la UI.
     */
    fun refreshAlmuerzos() {
        loadAlmuerzos()
    }

    /**
     * Limpia el mensaje de error actual.
     * 
     * Útil para dismissar mensajes de error en la UI.
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Obtiene un almuerzo específico por su ID.
     * 
     * @param almuerzoId ID del almuerzo a buscar
     * @return El almuerzo encontrado o null si no existe
     */
    fun getAlmuerzoById(almuerzoId: Int): Almuerzo? {
        return _almuerzos.value?.find { it.id == almuerzoId }
    }

    /**
     * Verifica si hay almuerzos disponibles.
     * 
     * @return true si hay al menos un almuerzo disponible, false en caso contrario
     */
    fun hasAlmuerzos(): Boolean {
        return _almuerzos.value?.isNotEmpty() == true
    }
}