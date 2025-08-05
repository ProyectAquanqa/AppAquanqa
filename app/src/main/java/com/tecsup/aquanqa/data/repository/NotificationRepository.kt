package com.tecsup.aquanqa.ui.notifications

import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.api.ApiService
import com.tecsup.aquanqa.data.model.content.Notification
import com.tecsup.aquanqa.data.model.content.NotificationGroup
import com.tecsup.aquanqa.data.model.content.EventoNotification
import com.tecsup.aquanqa.data.model.content.AutorNotification
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repositorio para la gestión de notificaciones.
 *
 * Se encarga de la comunicación con la API para obtener el historial de notificaciones,
 * marcarlas como leídas y agruparlas por fecha para su correcta visualización.
 * Este repositorio centraliza toda la lógica de datos relacionada con las notificaciones.
 */
class NotificationRepository(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {

    /**
     * Obtiene el historial de notificaciones del usuario desde la API.
     *
     * Realiza una petición GET al endpoint `api/notifications/`, incluyendo el token
     * de autenticación del usuario. Maneja tanto respuestas exitosas como errores de red
     * o de la API, devolviendo un objeto `Result` que encapsula el estado de la operación.
     *
     * @return `Result.Success(List<Notification>)` si la petición es exitosa.
     * @return `Result.Error(Exception)` si ocurre algún error.
     */
    suspend fun getNotifications(): Result<List<Notification>> {
        return try {
            val token = userPreferences.accessToken.first()
            if (token == null) {
                return Result.Error(Exception("Token de acceso no disponible"))
            }

            val response = apiService.getNotifications("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(Exception("Error al obtener notificaciones: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Marca una notificación específica como leída en el servidor.
     *
     * Envía una petición PATCH al endpoint `api/notifications/{id}/` con el cuerpo
     * `{"leida": true}` para actualizar el estado de la notificación.
     *
     * @param notificationId El ID de la notificación a marcar como leída.
     * @return `Result.Success(true)` si la operación fue exitosa.
     * @return `Result.Error(Exception)` si ocurre algún error.
     */
    suspend fun markNotificationAsRead(notificationId: String): Result<Boolean> {
        return try {
            val token = userPreferences.accessToken.first()
            if (token == null) {
                return Result.Error(Exception("Token de acceso no disponible"))
            }

            val response = apiService.markNotificationAsRead(
                "Bearer $token",
                notificationId,
                mapOf("leida" to true)
            )

            if (response.isSuccessful) {
                Result.Success(true)
            } else {
                Result.Error(Exception("Error al marcar notificación como leída"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Agrupa una lista de notificaciones por fecha para la UI.
     *
     * Crea etiquetas como "Hoy", "Ayer" y fechas específicas (ej: "25 de julio")
     * para organizar las notificaciones de manera cronológica en el RecyclerView.
     *
     * @param notifications La lista de notificaciones sin agrupar.
     * @return Una lista de `NotificationGroup`, donde cada grupo contiene una etiqueta
     *         de fecha y las notificaciones correspondientes a esa fecha.
     */
    fun groupNotificationsByDate(notifications: List<Notification>): List<NotificationGroup> {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.timeInMillis

        val grouped = notifications
            .sortedByDescending { it.timestampMillis }
            .groupBy { notification ->
                when {
                    isSameDay(notification.timestampMillis, today) -> "Hoy"
                    isSameDay(notification.timestampMillis, yesterday) -> "Ayer"
                    else -> formatDateLabel(notification.timestampMillis)
                }
            }

        return grouped.map { (dateLabel, notificationList) ->
            NotificationGroup(dateLabel, notificationList)
        }.sortedWith(compareBy { group ->
            when (group.dateLabel) {
                "Hoy" -> 0
                "Ayer" -> 1
                else -> 2
            }
        })
    }

    /**
     * Comprueba si dos timestamps (en milisegundos) pertenecen al mismo día.
     */
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Formatea un timestamp a una etiqueta de fecha legible (ej: "25 de julio").
     */
    private fun formatDateLabel(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("d 'de' MMMM", Locale("es", "ES"))
        return dateFormat.format(Date(timestamp))
    }
}