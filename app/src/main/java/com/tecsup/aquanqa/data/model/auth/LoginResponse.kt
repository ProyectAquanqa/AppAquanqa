package com.tecsup.aquanqa.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos que representa la respuesta completa de la API al iniciar sesión.
 */
data class LoginResponse(
    val access: String,
    val refresh: String,
    val user: UserResponse
)

/**
 * Modelo de datos para la información del usuario anidada en la respuesta del login.
 * Los nombres coinciden con el JSON de la API.
 */
data class UserResponse(
    val id: Int,
    val first_name: String,
    val dni: String
) 