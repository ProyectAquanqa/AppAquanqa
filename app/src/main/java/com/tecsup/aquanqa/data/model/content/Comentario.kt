package com.tecsup.aquanqa.data.model.content

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Modelo de datos para representar la información del usuario que comenta.
 */
data class ComentarioUsuario(
    val id: Int,
    val username: String,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("foto_perfil")
    val fotoPerfil: String?
) : Serializable

/**
 * Modelo de datos para un comentario.
 */
data class Comentario(
    val id: Int,
    val contenido: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    val usuario: ComentarioUsuario
) : Serializable

/**
 * Modelo para las respuestas del API de comentarios.
 */
data class ComentarioResponse(
    val id: Int,
    val contenido: String,
    @SerializedName("created_at")
    val createdAt: String,
    val usuario: ComentarioUsuario
)

/**
 * Modelo para crear un nuevo comentario.
 */
data class NuevoComentarioRequest(
    @SerializedName("evento_id")
    val eventoId: Int,
    val contenido: String
)

/**
 * Respuesta del toggle like.
 */
data class LikeResponse(
    val liked: Boolean,
    @SerializedName("likes_count")
    val likesCount: Int,
    val message: String
)
