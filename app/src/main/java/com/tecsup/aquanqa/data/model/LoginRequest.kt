package com.tecsup.aquanqa.data.model

/**
 * Modelo para la solicitud de login que se enviará a la API.
 *  en la API de Django, el campo 'dni' se usa como 'username' para autenticación.
 */
data class LoginRequest(
    val username: String, // DNI del usuario
    val password: String
) 