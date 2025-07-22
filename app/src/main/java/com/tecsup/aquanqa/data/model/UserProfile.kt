package com.tecsup.aquanqa.data.model

/**
 * Modelo para el perfil de usuario que se obtiene de la API.
 */
data class UserProfile(
    val foto_perfil: String?,
    val firma: String?,
    val created_by: String?,
    val updated_by: String?
) 