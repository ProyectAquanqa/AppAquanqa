package com.tecsup.aquanqa.data.model.content

import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Enum para representar los diferentes tipos de notificaciones disponibles en la aplicación.
 */
enum class NotificationType {
    @SerializedName("new_post")
    NEW_POST,
    
    @SerializedName("comment")
    COMMENT,
    
    @SerializedName("ad_update")
    AD_UPDATE,
    
    @SerializedName("community_event")
    COMMUNITY_EVENT
}

/**
 * Modelo de datos principal para una notificación.
 * Representa la información básica de una notificación recibida por el usuario.
 */
data class Notification(
    val id: String,
    @SerializedName("titulo")
    val title: String,
    @SerializedName("mensaje")
    val message: String,
    @SerializedName("fecha_creacion")
    val timestamp: String, // ISO 8601 format from Django
    @SerializedName("leida")
    val isRead: Boolean = false,
    @SerializedName("tipo")
    val type: String = "general", // Backend sends string type
    @SerializedName("evento")
    val evento: EventoNotification? = null
) {
    // Propiedades calculadas para compatibilidad con la interfaz de usuario
    val authorName: String
        get() = evento?.autor?.fullName ?: "Sistema"
    
    val authorImageUrl: String?
        get() = evento?.autor?.fotoPerfil
    
    val timestampMillis: Long
        get() = try {
            // Usa el parseador moderno de Java 8 para máxima compatibilidad
            Instant.parse(timestamp).toEpochMilli()
        } catch (e: DateTimeParseException) {
            // Fallback por si el formato de fecha es inesperado
            System.currentTimeMillis()
        }
}

/**
 * Modelo del evento asociado a una notificación.
 */
data class EventoNotification(
    val id: Int,
    val titulo: String,
    val autor: AutorNotification
)

/**
 * Modelo  del autor para notificaciones.
 */
data class AutorNotification(
    val id: Int,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("foto_perfil")
    val fotoPerfil: String?
)

/**
 * Modelo de datos para agrupar notificaciones por fecha.
 * Utilizado para organizar las notificaciones en la UI por día.
 */
data class NotificationGroup(
    val dateLabel: String,
    val notifications: List<Notification>
)

/**
 * Sealed class para representar los diferentes tipos de items que pueden aparecer
 * en el RecyclerView del adapter de notificaciones.
 */
sealed class NotificationItem {
    // Representa un header de fecha en la lista de notificaciones.
    data class DateHeader(val date: String) : NotificationItem()
    // Representa una notificación individual en la lista.
    data class NotificationData(val notification: Notification) : NotificationItem()
}