package com.tecsup.aquanqa.ui.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.preferences.UserPreferences

/**
 * Factory para crear instancias de NotificationViewModel.
 * Se encarga de construir el ViewModel con todas sus dependencias (Repository, ApiService, etc.),
 * manteniendo el Fragment limpio y desacoplado de la lógica de inyección de dependencias.
 */
class NotificationViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            // Construir las dependencias aquí para mantener el Fragment limpio
            val userPreferences = UserPreferences(context.applicationContext)
            val apiService = ApiClient.getClient(context.applicationContext).apiService
            val repository = NotificationRepository(apiService, userPreferences)
            
            return NotificationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}