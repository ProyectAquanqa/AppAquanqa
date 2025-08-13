package com.tecsup.aquanqa.data.model.content

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

/**
 * Modelo de datos para representar la información del autor de un anuncio.
 * SerializedName para mapear los nombres de la API a las propiedades de Kotlin.
 */
data class Autor(
    val id: Int,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("foto_perfil")
    val fotoPerfil: String?
) : Serializable

//Modelo de datos principal para un anuncio.

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
    val updatedAt: String? = null,
    
    // Campos para likes
    @SerializedName("likes_count")
    val likesCount: Int = 0,
    @SerializedName("user_has_liked")
    val userHasLiked: Boolean = false,
    
    // Campos para comentarios
    @SerializedName("comentarios_count")
    val comentariosCount: Int = 0,
    @SerializedName("comentarios_recientes")
    val comentariosRecientes: List<Comentario> = emptyList()
) : Serializable 