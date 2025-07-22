package com.tecsup.aquanqa.ui.chatbot.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos para la solicitud enviada al endpoint del chatbot.
 *
 * @property question El texto de la pregunta del usuario.
 * @property sessionId El ID de sesión opcional para mantener el contexto de la conversación.
 */
data class ChatbotRequest(
    @SerializedName("question")
    val question: String,

    @SerializedName("session_id")
    val sessionId: String? = null
) 