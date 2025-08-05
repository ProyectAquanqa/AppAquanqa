package com.tecsup.aquanqa.data.api

import com.tecsup.aquanqa.data.model.chatbot.ApiResponse
import com.tecsup.aquanqa.data.model.chatbot.ChatbotRequest
import com.tecsup.aquanqa.data.model.chatbot.ChatbotResponse
import com.tecsup.aquanqa.data.model.chatbot.RecommendedQuestion
import com.tecsup.aquanqa.data.model.chatbot.RecommendedQuestionsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Interfaz de Retrofit para los servicios relacionados con el Chatbot.
 */
interface ChatbotApiService {

    /**
     * Envía una pregunta al chatbot y recibe una respuesta.

     * @param request El objeto que contiene la pregunta del usuario y el ID de sesión.
     * @return Un objeto [ApiResponse] con la respuesta del chatbot envuelta.
     */
    @POST("api/chatbot/query/")
    suspend fun sendMessage(@Body request: ChatbotRequest): Response<ApiResponse<ChatbotResponse>>

    // Obtiene una lista de las preguntas más frecuentes para mostrar al inicio del chat.

    @GET("api/chatbot-knowledge/frequent_questions/")
    suspend fun getFrequentQuestions(): Response<ApiResponse<RecommendedQuestionsResponse>>
} 