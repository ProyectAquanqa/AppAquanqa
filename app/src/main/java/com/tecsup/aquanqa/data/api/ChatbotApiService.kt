package com.tecsup.aquanqa.data.api

import com.tecsup.aquanqa.ui.chatbot.model.ChatbotRequest
import com.tecsup.aquanqa.ui.chatbot.model.ChatbotResponse
import com.tecsup.aquanqa.ui.chatbot.model.RecommendedQuestion
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Interfaz de servicio de Retrofit para las interacciones con el chatbot.
 */
interface ChatbotApiService {

    /**
     * Envía un mensaje al chatbot y recibe una respuesta.
     *
     * @param request El cuerpo de la solicitud, que contiene la pregunta y el ID de sesión.
     * @return La respuesta del chatbot.
     */
    @POST("api/chatbot/")
    suspend fun sendMessage(@Body request: ChatbotRequest): ChatbotResponse

    /**
     * Obtiene una lista de preguntas recomendadas para mostrar al usuario.
     *
     * @return Una lista de preguntas recomendadas.
     */
    @GET("api/chatbot/recommended-questions/")
    suspend fun getRecommendedQuestions(): List<RecommendedQuestion>

} 