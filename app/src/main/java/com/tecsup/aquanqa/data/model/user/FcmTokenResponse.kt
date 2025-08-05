package com.tecsup.aquanqa.data.model.user

/**
 * Modelo para la respuesta de tokens FCM del servidor
 */
data class FcmTokenResponse(
    val id: Int,
    val token: String,
    val device_type: String,
    val is_active: Boolean,
    val created_at: String
) 