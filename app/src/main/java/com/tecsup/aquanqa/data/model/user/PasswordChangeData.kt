package com.tecsup.aquanqa.data.model.user

/**
 * Data class para encapsular los datos de cambio de contraseña.
 * Mejora la organización y tipado de datos.
 */
data class PasswordChangeData(
    val currentPassword: String,
    val newPassword: String
)