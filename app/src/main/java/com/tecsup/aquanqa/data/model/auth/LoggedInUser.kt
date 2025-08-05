package com.tecsup.aquanqa.data.model.auth

// Clase de datos que captura información del usuario para los usuarios que iniciaron sesión recuperada de LoginRepository

data class LoggedInUser(
    val userId: String,
    val displayName: String,
    val token: String
)