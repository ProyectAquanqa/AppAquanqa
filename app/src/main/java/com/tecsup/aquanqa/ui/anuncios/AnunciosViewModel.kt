package com.tecsup.aquanqa.ui.anuncios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.data.model.content.Anuncio
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.tecsup.aquanqa.data.repository.AnunciosRepository
import com.tecsup.aquanqa.data.Result as DataResult
import kotlinx.coroutines.launch

/**
 * ViewModel refactorizado para anuncios con cache híbrido inteligente.
 * Ahora con persistencia que sobrevive al cierre de la app.
 */
class AnunciosViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val repository = AnunciosRepository(userPreferences)

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
     * Carga anuncios con cache híbrido inteligente.
     * Ahora con persistencia que sobrevive al cierre de la app.
     * 
     * @param forceRefresh Forzar actualización desde API
     */
    fun cargarAnuncios(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.getAnuncios(forceRefresh)) {
                is DataResult.Success -> {
                    _anuncios.value = result.data
                    _error.value = "" // Limpiar errores previos
                }
                is DataResult.Error -> {
                    _error.value = result.exception.message ?: "Ocurrió un error desconocido"
                }
                is DataResult.Loading -> {
                    // El loading ya se maneja manualmente arriba y abajo
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Refresca anuncios forzando llamada a la API.
     * Útil para pull-to-refresh.
     */
    fun refreshAnuncios() {
        cargarAnuncios(forceRefresh = true)
    }

    /**
     * Limpia el cache de anuncios y recarga.
     */
    fun clearCacheAndReload() {
        viewModelScope.launch {
            repository.clearCache()
            cargarAnuncios(forceRefresh = true)
        }
    }

    /**
     * Método llamado cuando la app vuelve del background.
     * Refresca anuncios para detectar contenido nuevo inmediatamente.
     */
    fun onAppResumed() {
        refreshAnuncios()
    }
} 