package com.tecsup.aquanqa.ui.chatbot.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos para una pregunta recomendada.
 *
 * @property question El texto de la pregunta.
 */
data class RecommendedQuestion(
    @SerializedName("question")
    val question: String
) 