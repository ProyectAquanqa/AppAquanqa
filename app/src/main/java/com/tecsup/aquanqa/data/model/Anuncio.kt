package com.tecsup.aquanqa.data.model

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Modelo de datos para representar la información del autor de un anuncio.
 * `SerializedName` se usa para mapear los nombres de la API a las propiedades de Kotlin.
 */
data class Autor(
    val id: Int,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("foto_perfil")
    val fotoPerfil: String?
)

/**
 * Modelo de datos principal para un anuncio.
 */
data class Anuncio(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val fecha: Date,
    val imagen: String?,
    val autor: Autor,
    val categoria: Category,
    val publicado: Boolean = true,
    @SerializedName("is_pinned")
    val isPinned: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) 