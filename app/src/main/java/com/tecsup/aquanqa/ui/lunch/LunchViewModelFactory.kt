package com.tecsup.aquanqa.ui.lunch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias del LunchViewModel.
 * 
 * Se encarga de construir el ViewModel con todas sus dependencias (Repository, ApiService, etc.),
 * siguiendo el patrón de inyección de dependencias manual utilizado en la aplicación.
 * 
 * @property repository Repositorio de almuerzos que se inyectará al ViewModel
 */
class LunchViewModelFactory(
    private val repository: LunchRepository
) : ViewModelProvider.Factory {

    /**
     * Crea una nueva instancia del ViewModel solicitado.
     * 
     * @param modelClass Clase del ViewModel a crear
     * @return Instancia del ViewModel con sus dependencias inyectadas
     * @throws IllegalArgumentException si se solicita un ViewModel no soportado
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LunchViewModel::class.java)) {
            return LunchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}