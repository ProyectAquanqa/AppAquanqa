package com.tecsup.aquanqa.data.api

import com.tecsup.aquanqa.ui.chatbot.model.ChatbotRequest
import com.tecsup.aquanqa.ui.chatbot.model.ChatbotResponse
import com.tecsup.aquanqa.ui.chatbot.model.RecommendedQuestion
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Interfaz de Retrofit para los servicios relacionados con el Chatbot.
 */
interface ChatbotApiService {

    /**
     * Envía una pregunta al chatbot y recibe una respuesta.
     * La respuesta ahora incluye la respuesta principal y una lista de preguntas
     * recomendadas contextualmente.
     *
     * @param request El objeto que contiene la pregunta del usuario y el ID de sesión.
     * @return Un objeto [ChatbotResponse] con la respuesta y sugerencias.
     */
    @POST("api/chatbot/")
    suspend fun sendMessage(@Body request: ChatbotRequest): ChatbotResponse

    /**
     * Obtiene una lista de las preguntas más frecuentes para mostrar al inicio del chat.
     * Reemplaza al antiguo endpoint de preguntas recomendadas.
     *
     * @return Una lista de objetos [RecommendedQuestion].
     */
    @GET("api/chatbot/frequent-questions/")
    suspend fun getFrequentQuestions(): List<RecommendedQuestion>
} 