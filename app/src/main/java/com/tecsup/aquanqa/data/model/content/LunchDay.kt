package com.tecsup.aquanqa.data.model.content

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos que representa un almuerzo del comedor de Tecsup.
 * 
 * Esta clase mapea directamente con la respuesta de la API de almuerzos,
 * incluyendo todos los campos necesarios para mostrar el menú del día
 * y permitir la navegación al link de pedidos.
 * 
 * @property id Identificador único del almuerzo
 * @property fecha Fecha del almuerzo en formato YYYY-MM-DD  
 * @property entrada Plato de entrada del menú
 * @property platoFondo Plato principal del menú
 * @property refresco Bebida incluida en el menú
 * @property esFeriado Indica si ese día es feriado (no se debe mostrar)
 * @property link URL para realizar el pedido del almuerzo
 * @property nombreDia Nombre del día de la semana en español (calculado por la API)
 * @property createdAt Fecha de creación del registro (auditoría)
 * @property updatedAt Fecha de última actualización (auditoría)
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
    
    @SerializedName("nombre_dia")
    val nombreDia: String,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at") 
    val updatedAt: String
)