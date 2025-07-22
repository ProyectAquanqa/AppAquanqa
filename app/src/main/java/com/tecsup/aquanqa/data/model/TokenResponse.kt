package com.tecsup.aquanqa.data.model

/**
 * Modelo para la respuesta de autenticación que devuelve la API.
 * Contiene el token de acceso y el token de refresco JWT.
 */
data class TokenResponse(
    val access: String,
    val refresh: String
) 