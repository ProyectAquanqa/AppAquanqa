package com.tecsup.aquanqa.data.model.chatbot

/**
 * Modelo de datos para la solicitud enviada al endpoint del chatbot.
 * Solo contiene la pregunta del usuario.
 *
 * @property question El texto de la pregunta del usuario.
 */
data class ChatbotRequest(
    val question: String
) 