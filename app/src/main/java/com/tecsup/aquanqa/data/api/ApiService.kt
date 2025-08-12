package com.tecsup.aquanqa.data.api

import com.tecsup.aquanqa.data.model.content.Anuncio
import com.tecsup.aquanqa.data.model.content.Category
import com.tecsup.aquanqa.data.model.user.FcmTokenResponse
import com.tecsup.aquanqa.data.model.auth.LoginRequest
import com.tecsup.aquanqa.data.model.auth.LoginResponse
import com.tecsup.aquanqa.data.model.common.PaginatedResponse
import com.tecsup.aquanqa.data.model.auth.RefreshTokenRequest
import com.tecsup.aquanqa.data.model.auth.RefreshTokenResponse
import com.tecsup.aquanqa.data.model.user.UserProfile
import com.tecsup.aquanqa.data.model.chatbot.ChatbotRequest
import com.tecsup.aquanqa.data.model.chatbot.ChatbotResponse
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
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interfaz que define todos los endpoints de la API de la aplicacion
 * Esta interfaz utiliza Retrofit para realizar peticiones HTTP a la API del backend de Django.
 */
interface ApiService {
    /**Endpoint para iniciar sesión y obtener tokens JWT.
     * loginRequest Datos de inicio de sesión (DNI y contraseña)
     *  Response<LoginResponse> Respuesta con tokens de acceso y refresco */

    @POST("api/mobile/auth/login/")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
    
    /**
     * Endpoint para refrescar el token de acceso cuando expira.
     * @param refreshToken Objeto con el token de refresco
     * @return Response<RefreshTokenResponse> Nuevo token de acceso
     */
    @POST("api/mobile/auth/refresh/")
    suspend fun refreshToken(@Body refreshToken: RefreshTokenRequest): Response<RefreshTokenResponse>


    /**
     * Endpoint para obtener el perfil completo de un usuario autenticado
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @return Response<UserProfile> Datos completos del perfil del usuario
     */
    @GET("api/mobile/auth/profile/")
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
    @PATCH("api/mobile/auth/profile/")
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
    @PATCH("api/mobile/auth/profile/")
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
    @GET("api/mobile/categorias/")
    suspend fun getCategories(@Header("Authorization") token: String): Response<List<Category>>

    /**
     * Endpoint para buscar categorías por nombre.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param search Término de búsqueda para filtrar categorías por nombre
     * @return Response<List<Category>> Lista de categorías que coinciden con la búsqueda
     */
    @GET("api/mobile/categorias/")
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
    @GET("api/mobile/feed/eventos/")
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
    @GET("api/mobile/eventos/")
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
    @GET("api/mobile/eventos/")
    suspend fun getFilteredEvents(
        @Header("Authorization") token: String,
        @Query("categoria__nombre") categoriaNombre: String? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<Anuncio>>

    /**
     * Endpoint específico para Android con filtrado robusto por categoría.

     */
    @GET("api/mobile/eventos/")
    suspend fun getEventosAndroid(
        @Header("Authorization") token: String,
        @Query("categoria__nombre") categoriaNombre: String? = null,
        @Query("ordering") ordering: String? = "-created_at"
    ): Response<List<Anuncio>>

    /**
     * Endpoint heredado para obtener eventos por categoría.

     * @param categoriaNombre Nombre de la categoría a filtrar
     * @return Response<List<Anuncio>> Lista de eventos de la categoría especificada
     * @deprecated Usar getEventsByCategory o getFilteredEvents en su lugar
     */
    @GET("api/mobile/eventos/")
    suspend fun getEventosPorCategoria(
        @Query("categoria_nombre") categoriaNombre: String
    ): Response<List<Anuncio>>

    /**
     * Endpoint para obtener el detalle de un evento por ID.
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param id ID del evento
     */
    @GET("api/mobile/eventos/{id}/")
    suspend fun getEventoById(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Anuncio>

    // ================= CHATBOT =================

    /**
     * Endpoint para enviar consultas al sistema de chatbot.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param request Objeto que contiene la pregunta del usuario
     * @return Response<ChatbotResponse> Respuesta del chatbot con la respuesta, 
     *         puntaje de confianza y preguntas recomendadas
     */
    @POST("api/mobile/chatbot/query/")
    suspend fun postChatbotQuery(
        @Header("Authorization") token: String,
        @Body request: ChatbotRequest
    ): Response<ChatbotResponse>

    // ================= NOTIFICACIONES =================

    /**
     * Endpoint para obtener el historial de notificaciones del usuario.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param page Número de página (opcional, por defecto 1)
     * @param pageSize Cantidad de elementos por página (opcional, por defecto 20)
     * @return Response<List<Notification>> Lista de notificaciones
     */
    @GET("api/mobile/notifications/")
    suspend fun getNotifications(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<List<com.tecsup.aquanqa.data.model.content.Notification>>

    /**
     * Endpoint para marcar una notificación como leída.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param notificationId ID de la notificación a marcar como leída
     * @return Response<Unit> Respuesta de confirmación
     */
    @PATCH("api/mobile/notifications/{id}/")
    suspend fun markNotificationAsRead(
        @Header("Authorization") token: String,
        @Path("id") notificationId: String,
        @Body readStatus: Map<String, Boolean>
    ): Response<Unit>

    /**
     * Endpoint para registrar un token FCM de dispositivo.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param fcmTokenRequest Objeto que contiene el token FCM del dispositivo
     * @return Response<Unit> Respuesta de confirmación del registro
     */
    @POST("api/mobile/fcm-token/")
    suspend fun registerFcmToken(
        @Header("Authorization") token: String,
        @Body fcmTokenRequest: Map<String, String>
    ): Response<Unit>

    /**
     * Endpoint para obtener los tokens FCM del usuario.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @return Response<List<FcmTokenResponse>> Lista de tokens del usuario
     */
    @GET("api/mobile/fcm-token/")
    suspend fun getFcmTokens(
        @Header("Authorization") token: String
    ): Response<List<FcmTokenResponse>>

    /**
     * Endpoint para actualizar un token FCM específico.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param tokenId ID del token a actualizar
     * @param updateData Datos a actualizar (ej: is_active: false)
     * @return Response<Unit> Respuesta de confirmación
     */
    @PATCH("api/mobile/fcm-token/{id}/")
    suspend fun updateFcmToken(
        @Header("Authorization") token: String,
        @Path("id") tokenId: Int,
        @Body updateData: Map<String, Any>
    ): Response<Unit>

    // ================= ALMUERZOS =================
    
    /**
     * Endpoint para obtener la lista de almuerzos filtrada por días no feriados.
     * 
     * Obtiene todos los menús de almuerzo disponibles, excluyendo automáticamente
     * los días marcados como feriados. Los resultados se ordenan por fecha ascendente.
     * 
     * @param token Token de autenticación en formato "Bearer {token}"
     * @param esFeriado Filtro para excluir feriados (siempre false para días laborables)
     * @param active Filtro para obtener solo almuerzos activos (por defecto: true)
     * @param ordering Campo por el cual ordenar los resultados (por defecto: fecha)
     * @return Response<List<Almuerzo>> Lista de almuerzos disponibles
     */
    @GET("api/mobile/almuerzos/")
    suspend fun getAlmuerzos(
        @Header("Authorization") token: String,
        @Query("es_feriado") esFeriado: Boolean = false,
        @Query("active") active: Boolean = true,
        @Query("ordering") ordering: String = "fecha"
    ): Response<List<com.tecsup.aquanqa.data.model.content.Almuerzo>>
} 