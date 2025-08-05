package com.tecsup.aquanqa.data.model.user

/**
 * Modelo de datos que representa el perfil de un usuario.
 *
 * Los nombres de los campos  están en snake_case para coincidir
 * directamente con las claves del JSON devuelto por la API de Django,
 * eliminando la necesidad de anotaciones @SerializedName.
 */
data class UserProfile(
    val id: Int,
    val username: String,
    val first_name: String,
    val last_name: String,
    val email: String?,
    val groups: List<String>?,
    val foto_perfil: String?,
    val firma: String?
) 