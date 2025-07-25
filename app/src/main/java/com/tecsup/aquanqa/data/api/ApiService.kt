package com.tecsup.aquanqa.data.api

import com.tecsup.aquanqa.data.model.Anuncio
import com.tecsup.aquanqa.data.model.LoginRequest
import com.tecsup.aquanqa.data.model.LoginResponse
import com.tecsup.aquanqa.data.model.UserProfile
import com.tecsup.aquanqa.ui.chatbot.model.ChatbotRequest
import com.tecsup.aquanqa.ui.chatbot.model.ChatbotResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query

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
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
    
    /**
     * Endpoint para refrescar el token de acceso
     * @param refreshToken Token de refresco
     * @return Nuevo token de acceso
     */
    @POST("api/token/refresh/")
    suspend fun refreshToken(@Body refreshToken: Map<String, String>): Response<LoginResponse>
    
    /**
     * Endpoint para obtener el perfil del usuario autenticado
     * @param token Token de autenticación (Bearer token)
     * @return Datos del perfil del usuario
     */
    @GET("api/profile/")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<UserProfile>

    /**
     * Endpoint unificado para actualizar el perfil del usuario (foto, firma, etc.).
     * Usa el método PATCH para permitir actualizaciones parciales.
     *
     * @param token Token de autenticación (Bearer token).
     * @param fotoPerfil Archivo de imagen para la foto de perfil (opcional).
     * @param firma Archivo de imagen para la firma (opcional).
     * @return Respuesta con los datos actualizados del perfil.
     */
    @Multipart
    @PATCH("api/profile/")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Part fotoPerfil: MultipartBody.Part?,
        @Part firma: MultipartBody.Part?
    ): Response<UserProfile>

    /**
     * Endpoint para actualizar datos de texto del perfil (email, contraseña, etc.).
     * Usa el método PATCH para permitir actualizaciones parciales.
     *
     * @param token Token de autenticación (Bearer token).
     * @param textData Un mapa con los campos de texto a actualizar.
     * @return Respuesta con los datos actualizados del perfil.
     */
    @PATCH("api/profile/")
    suspend fun updateProfileTextData(
        @Header("Authorization") token: String,
        @Body textData: Map<String, String>
    ): Response<UserProfile>

    /**
     * Endpoint para obtener la lista de eventos filtrados por categoría.
     * @param categoriaNombre Nombre de la categoría a filtrar (e.g., "Anuncios")
     * @return Lista de eventos que coinciden con la categoría
     */
    @GET("api/eventos/")
    suspend fun getEventosPorCategoria(
        @Query("categoria_nombre") categoriaNombre: String
    ): Response<List<Anuncio>>

    /**
     * Endpoint para enviar una consulta al chatbot.
     * @param request La pregunta del usuario.
     * @return La respuesta del chatbot con la respuesta, puntaje y preguntas recomendadas.
     */
    @POST("api/chatbot/query/")
    suspend fun postChatbotQuery(@Body request: ChatbotRequest): Response<ChatbotResponse>
} 