package com.tecsup.aquanqa.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.model.Notification
import com.tecsup.aquanqa.data.model.NotificationGroup
import com.tecsup.aquanqa.data.model.NotificationItem

import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel para la pantalla de notificaciones.
 * Maneja la lógica de presentación y el estado de las notificaciones.
 */
class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _notificationsState = MutableLiveData<Result<List<NotificationItem>>>()
    val notificationsState: LiveData<Result<List<NotificationItem>>> = _notificationsState
    
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing
    
    init {
        android.util.Log.d("NotificationViewModel", "ViewModel inicializado, cargando notificaciones")
        loadNotifications()
    }
    
    /**
     * Carga las notificaciones desde el repositorio.
     * Agrupa las notificaciones por fecha y las convierte en items para el adapter.
     */
    fun loadNotifications() {
        android.util.Log.d("NotificationViewModel", "Iniciando carga de notificaciones")
        viewModelScope.launch {
            _notificationsState.value = Result.Loading
            
            when (val result = repository.getNotifications()) {
                is Result.Success -> {
                    android.util.Log.d("NotificationViewModel", "Repositorio devolvió ${result.data.size} notificaciones")
                    val groupedNotifications = repository.groupNotificationsByDate(result.data)
                    android.util.Log.d("NotificationViewModel", "Agrupadas en ${groupedNotifications.size} grupos")
                    val notificationItems = convertToNotificationItems(groupedNotifications)
                    android.util.Log.d("NotificationViewModel", "Convertidas a ${notificationItems.size} items")
                    _notificationsState.value = Result.Success(notificationItems)
                }
                is Result.Error -> {
                    android.util.Log.e("NotificationViewModel", "Error: ${result.exception.message}")
                    _notificationsState.value = result
                }
                is Result.Loading -> {
                    _notificationsState.value = result
                }
            }
        }
    }
    
    /**
     * Refresca las notificaciones.
     * Usado para pull-to-refresh.
     */
    fun refreshNotifications() {
        viewModelScope.launch {
            _isRefreshing.value = true
            
            when (val result = repository.getNotifications()) {
                is Result.Success -> {
                    val groupedNotifications = repository.groupNotificationsByDate(result.data)
                    val notificationItems = convertToNotificationItems(groupedNotifications)
                    _notificationsState.value = Result.Success(notificationItems)
                }
                is Result.Error -> {
                    _notificationsState.value = result
                }
                is Result.Loading -> {
                    // No cambiar el estado durante refresh
                }
            }
            
            _isRefreshing.value = false
        }
    }
    
    /**
     * Convierte los grupos de notificaciones en items para el RecyclerView.
     * Intercala headers de fecha con items de notificación.
     */
    private fun convertToNotificationItems(groups: List<NotificationGroup>): List<NotificationItem> {
        val items = mutableListOf<NotificationItem>()
        
        groups.forEach { group ->
            // Agregar header de fecha
            items.add(NotificationItem.DateHeader(group.dateLabel))
            
            // Agregar notificaciones del grupo
            group.notifications.forEach { notification ->
                items.add(NotificationItem.NotificationData(notification))
            }
        }
        
        return items
    }
    
    /**
     * Maneja el click en una notificación.
     * Marca la notificación como leída y puede implementar navegación específica.
     */
    fun onNotificationClicked(notification: Notification) {
        // Marcar como leída si no está leída
        if (!notification.isRead) {
            markNotificationAsRead(notification.id)
        }
        
        // TODO: Implementar navegación específica según el tipo de notificación
        // Por ejemplo:
        // - evento -> navegar al evento específico
        // - general -> mostrar detalles de la notificación
    }
    
    /**
     * Marca una notificación como leída.
     */
    private fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            when (repository.markNotificationAsRead(notificationId)) {
                is Result.Success -> {
                    // Recargar notificaciones para reflejar el cambio
                    loadNotifications()
                }
                is Result.Error -> {
                    android.util.Log.e("NotificationViewModel", "Error marcando notificación como leída")
                }
                is Result.Loading -> {
                    // No hacer nada durante loading
                }
            }
        }
    }
}