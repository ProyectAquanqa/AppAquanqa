package com.tecsup.aquanqa.ui.chatbot.model

import com.google.gson.annotations.SerializedName

/**
 * Wrapper para las respuestas del backend que incluye status y data
 */
data class ApiResponse<T>(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("data")
    val data: T?,
    
    @SerializedName("error")
    val error: String?
)

/**
 * Modelo de datos para la respuesta recibida del endpoint del chatbot.
 * Esta clase encapsula la respuesta del servidor, que ahora incluye
 * más contexto sobre la coincidencia encontrada.
 *
 * @property answer El texto de la respuesta principal del chatbot.
 * @property matchQuestion La pregunta original de la base de datos que coincidió con la consulta del usuario. Puede ser nulo si no se encontró una buena coincidencia.
 * @property score El puntaje de similitud (confianza) de la coincidencia, en una escala de 0 a 1.
 * @property recommendedQuestions Una lista de preguntas relacionadas que el usuario podría querer hacer a continuación.
 */
data class ChatbotResponse(
    @SerializedName("answer")
    val answer: String,

    @SerializedName("match_question")
    val matchQuestion: String?,

    @SerializedName("score")
    val score: Double,

    @SerializedName("recommended_questions")
    val recommendedQuestions: List<RecommendedQuestion> = emptyList()
)

/**
 * Modelo para la respuesta de preguntas recomendadas del backend
 */
data class RecommendedQuestionsResponse(
    @SerializedName("frequent_questions")
    val recommendedQuestions: List<RecommendedQuestion>,
    
    @SerializedName("total")
    val total: Int
) 