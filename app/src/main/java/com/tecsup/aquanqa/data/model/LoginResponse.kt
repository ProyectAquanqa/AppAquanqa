package com.tecsup.aquanqa.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos que representa la respuesta completa de la API al iniciar sesión.
 */
data class LoginResponse(
    @SerializedName("access")
    val access: String,

    @SerializedName("refresh")
    val refresh: String,

    @SerializedName("user")
    val user: UserResponse
)

/**
 * Modelo de datos para la información del usuario anidada en la respuesta del login.
 */
data class UserResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("first_name")
    val firstName: String,

    @SerializedName("dni")
    val dni: String
) 