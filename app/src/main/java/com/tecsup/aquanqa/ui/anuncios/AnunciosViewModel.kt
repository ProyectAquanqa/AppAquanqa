package com.tecsup.aquanqa.ui.anuncios

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.model.Anuncio
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de Anuncios.
 *
 * Sigue el patrón MVVM para separar la lógica de negocio de la UI. Se encarga de:
 * - Solicitar los datos al `AnunciosRepository`.
 * - Exponer el estado de la UI (carga, éxito, error) a través de LiveData.
 * - Mantener los datos de la lista de anuncios, sobreviviendo a cambios de configuración.
 */
class AnunciosViewModel : ViewModel() {

    private val repository = AnunciosRepository()

    // LiveData para exponer la lista de anuncios a la vista.
    private val _anuncios = MutableLiveData<List<Anuncio>>()
    val anuncios: LiveData<List<Anuncio>> = _anuncios

    // LiveData para manejar el estado de carga (mostrar/ocultar un spinner).
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData para comunicar errores a la vista.
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        // Cargar los anuncios tan pronto como el ViewModel se crea.
        cargarAnuncios()
    }

    /**
     * Carga la lista de anuncios desde el repositorio.
     *
     * Utiliza `viewModelScope` para lanzar una corrutina segura, que se cancelará
     * automáticamente si el ViewModel es destruido. Actualiza los LiveData
     * correspondientes para reflejar el estado de la operación.
     */
    fun cargarAnuncios() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.getAnuncios()) {
                is Result.Success -> {
                    _anuncios.value = result.data
                    _error.value = "" // Limpiar errores previos
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Ocurrió un error desconocido"
                }
                is Result.Loading -> {
                    // El loading ya se maneja manualmente arriba y abajo
                }
            }
            _isLoading.value = false
        }
    }
} 