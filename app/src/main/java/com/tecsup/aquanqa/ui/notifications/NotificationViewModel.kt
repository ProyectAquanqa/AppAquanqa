package com.tecsup.aquanqa.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.model.content.Notification
import com.tecsup.aquanqa.data.model.content.NotificationGroup
import com.tecsup.aquanqa.data.model.content.NotificationItem

import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel para la pantalla de notificaciones.
 * Maneja la lógica de presentación y el estado de las notificaciones con paginación.
 */
class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _notificationsState = MutableLiveData<Result<List<NotificationItem>>>()
    val notificationsState: LiveData<Result<List<NotificationItem>>> = _notificationsState
    
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing
    
    // Estados para paginación
    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore
    
    private val _allNotificationItems = mutableListOf<NotificationItem>()
    private var currentPage = 1
    private var hasMorePages = true
    private var isLoadingPage = false
    private val pageSize = 15
    
    init {
        android.util.Log.d("NotificationViewModel", "ViewModel inicializado, cargando notificaciones")
        loadNotifications()
    }
    
    /**
     * Carga las notificaciones desde el repositorio con paginación simulada.
     * Agrupa las notificaciones por fecha y las convierte en items para el adapter.
     */
    fun loadNotifications(isLoadingMore: Boolean = false) {
        if (isLoadingPage) return // Evitar llamadas múltiples simultáneas
        
        android.util.Log.d("NotificationViewModel", "Iniciando carga de notificaciones (loadMore: $isLoadingMore)")
        viewModelScope.launch {
            isLoadingPage = true
            
            if (isLoadingMore) {
                _isLoadingMore.value = true
            } else {
                _notificationsState.value = Result.Loading
                // Reiniciar paginación para nueva carga o refresh
                currentPage = 1
                hasMorePages = true
                _allNotificationItems.clear()
            }
            
            when (val result = repository.getNotifications()) {
                is Result.Success -> {
                    android.util.Log.d("NotificationViewModel", "Repositorio devolvió ${result.data.size} notificaciones")
                    
                    // Agrupar y convertir todas las notificaciones del servidor
                    val groupedNotifications = repository.groupNotificationsByDate(result.data)
                    android.util.Log.d("NotificationViewModel", "Agrupadas en ${groupedNotifications.size} grupos")
                    val allNotificationItems = convertToNotificationItems(groupedNotifications)
                    
                    // Simular paginación dividiendo los datos del servidor
                    val startIndex = (currentPage - 1) * pageSize
                    val endIndex = minOf(startIndex + pageSize, allNotificationItems.size)
                    
                    if (startIndex < allNotificationItems.size) {
                        val pageData = allNotificationItems.subList(startIndex, endIndex)
                        
                        if (isLoadingMore) {
                            // Agregar nuevas notificaciones a la lista existente
                            _allNotificationItems.addAll(pageData)
                        } else {
                            // Primera carga o refresh completo
                            _allNotificationItems.clear()
                            _allNotificationItems.addAll(pageData)
                        }
                        
                        // Actualizar estado de paginación
                        hasMorePages = endIndex < allNotificationItems.size
                        if (hasMorePages) currentPage++
                        
                        // Emitir lista completa actualizada
                        android.util.Log.d("NotificationViewModel", "Mostrando ${_allNotificationItems.size} items")
                        _notificationsState.value = Result.Success(_allNotificationItems.toList())
                    } else {
                        // No hay más datos
                        hasMorePages = false
                    }
                }
                is Result.Error -> {
                    android.util.Log.e("NotificationViewModel", "Error: ${result.exception.message}")
                    if (isLoadingMore) {
                        // Si falla la carga de más elementos, mantener los existentes
                        android.util.Log.e("NotificationViewModel", "Error loading more notifications: ${result.exception.message}")
                    } else {
                        _notificationsState.value = result
                    }
                }
                is Result.Loading -> {
                    if (!isLoadingMore) {
                        _notificationsState.value = result
                    }
                }
            }
            
            _isLoadingMore.value = false
            isLoadingPage = false
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
    
    /**
     * Carga la siguiente página de notificaciones para infinite scroll
     */
    fun loadMoreNotifications() {
        if (!hasMorePages || isLoadingPage) return
        loadNotifications(isLoadingMore = true)
    }
    
    /**
     * Verifica si se pueden cargar más notificaciones
     */
    fun canLoadMore(): Boolean = hasMorePages && !isLoadingPage
}