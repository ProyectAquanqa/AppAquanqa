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
 * ViewModel refactorizado para anuncios con cache híbrido inteligente y paginación.
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
    
    // Estados para paginación
    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore
    
    private val _allAnuncios = mutableListOf<Anuncio>()
    private var currentPage = 1
    private var hasMorePages = true
    private var isLoadingPage = false
    private val pageSize = 10

    init {
        // Cargar los anuncios tan pronto como el ViewModel se crea.
        cargarAnuncios()
    }

    /**
     * Carga anuncios con cache híbrido inteligente y paginación simulada.
     * Ahora con persistencia que sobrevive al cierre de la app.
     */
    fun cargarAnuncios(forceRefresh: Boolean = false, isLoadingMore: Boolean = false) {
        if (isLoadingPage) return // Evitar llamadas múltiples simultáneas
        
        viewModelScope.launch {
            isLoadingPage = true
            
            if (isLoadingMore) {
                _isLoadingMore.value = true
            } else {
                _isLoading.value = true
                // Reiniciar paginación para nueva carga o refresh
                currentPage = 1
                hasMorePages = true
                _allAnuncios.clear()
            }
            
            when (val result = repository.getAnuncios(forceRefresh)) {
                is DataResult.Success -> {
                    val allServerData = result.data
                    
                    // Simular paginación dividiendo los datos del servidor
                    val startIndex = (currentPage - 1) * pageSize
                    val endIndex = minOf(startIndex + pageSize, allServerData.size)
                    
                    if (startIndex < allServerData.size) {
                        val pageData = allServerData.subList(startIndex, endIndex)
                        
                        if (isLoadingMore) {
                            // Agregar nuevos anuncios a la lista existente
                            _allAnuncios.addAll(pageData)
                        } else {
                            // Primera carga o refresh completo
                            _allAnuncios.clear()
                            _allAnuncios.addAll(pageData)
                        }
                        
                        // Actualizar estado de paginación
                        hasMorePages = endIndex < allServerData.size
                        if (hasMorePages) currentPage++
                        
                        // Emitir lista completa actualizada
                        _anuncios.value = _allAnuncios.toList()
                        _error.value = "" // Limpiar errores previos
                    } else {
                        // No hay más datos
                        hasMorePages = false
                    }
                }
                is DataResult.Error -> {
                    if (isLoadingMore) {
                        // Si falla la carga de más elementos, mantener los existentes
                        android.util.Log.e("AnunciosViewModel", "Error loading more anuncios: ${result.exception.message}")
                    } else {
                        _error.value = result.exception.message ?: "Ocurrió un error desconocido"
                    }
                }
                is DataResult.Loading -> {
                    // El loading ya se maneja manualmente arriba y abajo
                }
            }
            
            _isLoading.value = false
            _isLoadingMore.value = false
            isLoadingPage = false
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
    
    /**
     * Carga la siguiente página de anuncios para infinite scroll
     */
    fun loadMoreAnuncios() {
        if (!hasMorePages || isLoadingPage) return
        cargarAnuncios(forceRefresh = false, isLoadingMore = true)
    }
    
    /**
     * Verifica si se pueden cargar más anuncios
     */
    fun canLoadMore(): Boolean = hasMorePages && !isLoadingPage
} 