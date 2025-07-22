package com.tecsup.aquanqa.ui.chatbot.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos para la respuesta recibida del endpoint del chatbot.
 *
 * @property answer El texto de la respuesta del chatbot.
 * @property sessionId El ID de sesión, devuelto por la API para mantener el contexto.
 */
data class ChatbotResponse(
    @SerializedName("answer")
    val answer: String,

    @SerializedName("session_id")
    val sessionId: String
) 