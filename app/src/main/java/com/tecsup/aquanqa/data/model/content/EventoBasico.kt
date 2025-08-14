package com.tecsup.aquanqa.data.model.content

/**
 * Modelo simplificado de evento para fallback cuando no hay internet.
 * Contiene solo datos esenciales sin URLs de imágenes.
 */
data class EventoBasico(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val fecha: String, // Como string para evitar problemas de serialización
    val autorNombre: String,
    val categoriaNombre: String,
    val createdAt: String? = null,
    val isPinned: Boolean = false // Agregar propiedad isPinned
) {
    /**
     * Convierte EventoBasico a Anuncio completo.
     * IMPORTANTE: imagen = null para forzar imagen por defecto en Glide.
     */
    fun toAnuncio(): Anuncio {
        return Anuncio(
            id = id,
            titulo = titulo,
            descripcion = descripcion,
            fecha = java.util.Date(), // Fecha actual como fallback
            imagen = null, // IMPORTANTE: null para imagen por defecto
            autor = Autor(
                id = 0,
                fullName = autorNombre,
                fotoPerfil = null
            ),
            categoria = Category(
                id = 0,
                nombre = categoriaNombre,
                descripcion = null,
                created_at = null,
                updated_at = null
            ),
            publicado = true,
            isPinned = isPinned, // IMPORTANTE: usar el valor real de isPinned
            createdAt = createdAt,
            updatedAt = null
        )
    }
}

/**
 * Extensión para convertir Anuncio a EventoBasico.
 */
fun Anuncio.toEventoBasico(): EventoBasico {
    return EventoBasico(
        id = id,
        titulo = titulo,
        descripcion = descripcion,
        fecha = createdAt ?: "",
        autorNombre = autor.fullName,
        categoriaNombre = categoria.nombre,
        createdAt = createdAt,
        isPinned = isPinned // IMPORTANTE: preservar el valor de isPinned
    )
}