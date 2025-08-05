package com.tecsup.aquanqa.data.model.chatbot

import com.google.gson.annotations.SerializedName

/**
 * Modelo para la respuesta de preguntas recomendadas del backend
 */
data class RecommendedQuestionsResponse(
    @SerializedName("frequent_questions")
    val recommendedQuestions: List<RecommendedQuestion>,
    
    @SerializedName("total")
    val total: Int
)