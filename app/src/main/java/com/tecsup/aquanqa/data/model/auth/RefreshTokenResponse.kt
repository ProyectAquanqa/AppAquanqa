package com.tecsup.aquanqa.data.model.auth

/**
 * Modelo para la respuesta del refresh token
 * Incluye tanto el nuevo access token como el nuevo refresh token (cuando está habilitada la rotación)
 */
data class RefreshTokenResponse(
    val access: String,
    val refresh: String? = null // Opcional para mantener compatibilidad, pero requerido con ROTATE_REFRESH_TOKENS
) 