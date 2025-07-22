package com.tecsup.aquanqa.data.api

import com.tecsup.aquanqa.data.model.LoginRequest
import com.tecsup.aquanqa.data.model.TokenResponse
import com.tecsup.aquanqa.data.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Interfaz que define los endpoints de la API
 */
interface ApiService {
    
    /**
     * Endpoint para iniciar sesión y obtener tokens JWT
     * @param loginRequest Datos de inicio de sesión (DNI y contraseña)
     * @return Respuesta con tokens de acceso y refresco
     */
    @POST("api/token/")
    suspend fun login(@Body loginRequest: LoginRequest): Response<TokenResponse>
    
    /**
     * Endpoint para refrescar el token de acceso
     * @param refreshToken Token de refresco
     * @return Nuevo token de acceso
     */
    @POST("api/token/refresh/")
    suspend fun refreshToken(@Body refreshToken: Map<String, String>): Response<TokenResponse>
    
    /**
     * Endpoint para obtener el perfil del usuario autenticado
     * @param token Token de autenticación
     * @return Datos del perfil del usuario
     */
    @GET("api/profile/me/")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<UserProfile>
} 