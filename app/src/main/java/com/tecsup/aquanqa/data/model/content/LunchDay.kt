package com.tecsup.aquanqa.data.model.content

import com.google.gson.annotations.SerializedName
import com.tecsup.aquanqa.utils.DateUtils

/**
 * Modelo de datos limpio y optimizado para almuerzos.
 * 
 * Representa un menú diario del comedor con toda la información necesaria
 * para mostrar en la UI y permitir pedidos.
 */
data class Almuerzo(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("fecha")
    val fecha: String,
    
    @SerializedName("entrada")
    val entrada: String,
    
    @SerializedName("plato_fondo")
    val platoFondo: String,
    
    @SerializedName("refresco")
    val refresco: String,
    
    @SerializedName("es_feriado")
    val esFeriado: Boolean,
    
    @SerializedName("link")
    val link: String?,
    
    @SerializedName("active")
    val active: Boolean,
    
    @SerializedName("dieta")
    val dieta: String?,
    
    @SerializedName("nombre_dia")
    val nombreDia: String,
    
    @SerializedName("fecha_formateada")
    val fechaFormateadaBackend: String?,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at") 
    val updatedAt: String
) {
    
    /**
     * Verifica si el almuerzo está disponible para pedidos.
     */
    val isAvailable: Boolean
        get() = active && !esFeriado
    
    /**
     * Verifica si tiene menú de dieta disponible.
     */
    val hasDietMenu: Boolean
        get() = !dieta.isNullOrBlank()
    
    /**
     * Verifica si tiene link de pedido disponible.
     */
    val hasOrderLink: Boolean
        get() = !link.isNullOrBlank()
    
    /**
     * Mensaje user-friendly del estado del almuerzo.
     */
    val statusMessage: String
        get() = when {
            esFeriado -> "Día feriado - No hay servicio"
            !active -> "Menú no disponible"
            !hasOrderLink -> "Pedidos no habilitados"
            else -> "Disponible para pedidos"
        }
    
    /**
     * Fecha formateada para mostrar en la UI como "Lunes 18 de agosto".
     * Prioriza el formato del backend, fallback a cálculo local.
     */
    val fechaFormateada: String
        get() = fechaFormateadaBackend?.takeIf { it.isNotBlank() } 
            ?: DateUtils.formatLunchDateFromISO(fecha)
}