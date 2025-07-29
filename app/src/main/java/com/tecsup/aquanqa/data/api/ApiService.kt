package com.tecsup.aquanqa.data.api

import com.tecsup.aquanqa.data.model.Anuncio
import com.tecsup.aquanqa.data.model.Category
import com.tecsup.aquanqa.data.model.LoginRequest
import com.tecsup.aquanqa.data.model.LoginResponse
import com.tecsup.aquanqa.data.model.PaginatedResponse
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
 * Interfaz que define todos los endpoints de la API de Aquanqa.
 * 
 * Esta interfaz utiliza Retrofit para realizar peticiones HTTP a la API
 * del backend de Django. Incluye endpoints para autenticación, perfil de usuario,
 * gestión de eventos, categorías y chatbot.
 */
interface ApiService {
    
    // ================= AUTENTICACIÓN =================
    
    /**
     * Endpoint para iniciar sesión y obtener tokens JWT.
     * 
     * @param loginRequest Datos de inicio de sesión (DNI y contraseña)
     * @return Response<LoginResponse> Respuesta con tokens de acceso y refresco
     */
    @POST("api/token/")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
    
    /**
     * Endpoint para refrescar el token de acceso cuando expira.
     * 
     * @param refreshToken Mapa con la clave "refresh" y el token de refresco
     * @return Response<LoginResponse> Nuevo token de acceso
     */
    @POST("api/token/refresh/")
    suspend fun refreshToken(@Body refreshToken: Map<String, String>): Response<LoginResponse>
    
    // ================= PERFIL DE USUARIO =================
    
    /**
     * Endpoint para obtener el perfil completo del usuario autenticado.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @return Response<UserProfile> Datos completos del perfil del usuario
     */
    @GET("api/profile/")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<UserProfile>

    /**
     * Endpoint para actualizar archivos del perfil del usuario (foto y firma).
     * Utiliza el método PATCH para permitir actualizaciones parciales.
     *
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param fotoPerfil Archivo de imagen para la foto de perfil (opcional)
     * @param firma Archivo de imagen para la firma (opcional)
     * @return Response<UserProfile> Respuesta con los datos actualizados del perfil
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
     * Utiliza el método PATCH para permitir actualizaciones parciales.
     *
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param textData Mapa con los campos de texto a actualizar
     * @return Response<UserProfile> Respuesta con los datos actualizados del perfil
     */
    @PATCH("api/profile/")
    suspend fun updateProfileTextData(
        @Header("Authorization") token: String,
        @Body textData: Map<String, String>
    ): Response<UserProfile>

    // ================= CATEGORÍAS =================

    /**
     * Endpoint para obtener todas las categorías de eventos disponibles.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @return Response<List<Category>> Lista de todas las categorías
     */
    @GET("api/categorias/")
    suspend fun getCategories(@Header("Authorization") token: String): Response<List<Category>>

    /**
     * Endpoint para buscar categorías por nombre.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param search Término de búsqueda para filtrar categorías por nombre
     * @return Response<List<Category>> Lista de categorías que coinciden con la búsqueda
     */
    @GET("api/categorias/")
    suspend fun searchCategories(
        @Header("Authorization") token: String,
        @Query("search") search: String
    ): Response<List<Category>>

    // ================= EVENTOS =================

    /**
     * Endpoint para obtener eventos paginados del feed público.
     * Soporta infinite scroll y lazy loading.
     * 
     * @param page Número de página (empezando desde 1)
     * @param pageSize Cantidad de elementos por página (opcional, por defecto 10)
     * @return Response<PaginatedResponse<Anuncio>> Respuesta paginada con eventos
     */
    @GET("api/feed/eventos/")
    suspend fun getAllEvents(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): Response<PaginatedResponse<Anuncio>>

    /**
     * Endpoint para obtener eventos filtrados por categoría con paginación.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param categoriaNombre Nombre de la categoría para filtrar eventos
     * @param page Número de página (empezando desde 1)
     * @param pageSize Cantidad de elementos por página (opcional, por defecto 10)
     * @return Response<PaginatedResponse<Anuncio>> Respuesta paginada con eventos filtrados
     */
    @GET("api/eventos/")
    suspend fun getEventsByCategory(
        @Header("Authorization") token: String,
        @Query("categoria__nombre") categoriaNombre: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): Response<PaginatedResponse<Anuncio>>

    /**
     * Endpoint mejorado para obtener eventos con múltiples opciones de filtrado.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param categoriaNombre Nombre de la categoría (opcional)
     * @param search Término de búsqueda en título y descripción (opcional)
     * @param ordering Campo por el cual ordenar (ej: "-fecha", "titulo") (opcional)
     * @param limit Número máximo de resultados a retornar (opcional)
     * @return Response<List<Anuncio>> Lista de eventos filtrados
     */
    @GET("api/eventos/")
    suspend fun getFilteredEvents(
        @Header("Authorization") token: String,
        @Query("categoria__nombre") categoriaNombre: String? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<Anuncio>>

    /**
     * Endpoint específico para Android con filtrado robusto por categoría.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param categoriaNombre Nombre de la categoría para filtrar (opcional)
     * @param ordering Campo por el cual ordenar (opcional, por defecto "-created_at")
     * @return Response<List<Anuncio>> Lista de eventos filtrados correctamente
     */
    @GET("api/eventos/")
    suspend fun getEventosAndroid(
        @Header("Authorization") token: String,
        @Query("categoria__nombre") categoriaNombre: String? = null,
        @Query("ordering") ordering: String? = "-created_at"
    ): Response<List<Anuncio>>

    /**
     * Endpoint heredado para obtener eventos por categoría.
     * Mantenido por compatibilidad con código existente.
     * 
     * @param categoriaNombre Nombre de la categoría a filtrar
     * @return Response<List<Anuncio>> Lista de eventos de la categoría especificada
     * @deprecated Usar getEventsByCategory o getFilteredEvents en su lugar
     */
    @GET("api/eventos/")
    suspend fun getEventosPorCategoria(
        @Query("categoria_nombre") categoriaNombre: String
    ): Response<List<Anuncio>>

    // ================= CHATBOT =================

    /**
     * Endpoint para enviar consultas al sistema de chatbot.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param request Objeto que contiene la pregunta del usuario
     * @return Response<ChatbotResponse> Respuesta del chatbot con la respuesta, 
     *         puntaje de confianza y preguntas recomendadas
     */
    @POST("api/chatbot/query/")
    suspend fun postChatbotQuery(
        @Header("Authorization") token: String,
        @Body request: ChatbotRequest
    ): Response<ChatbotResponse>
} 