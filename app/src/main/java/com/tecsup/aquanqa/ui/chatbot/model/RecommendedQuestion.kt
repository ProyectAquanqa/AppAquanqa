package com.tecsup.aquanqa.ui.chatbot.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos para una pregunta sugerida, ya sea frecuente o contextual.
 *
 * @property id El identificador único de la pregunta.
 * @property question El texto de la pregunta.
 */
data class RecommendedQuestion(
    @SerializedName("id")
    val id: Int,
    @SerializedName("question")
    val question: String
) 