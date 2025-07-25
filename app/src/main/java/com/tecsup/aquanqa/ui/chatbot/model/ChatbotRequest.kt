package com.tecsup.aquanqa.ui.chatbot.model

/**
 * Modelo de datos para la solicitud enviada al endpoint del chatbot.
 * Solo contiene la pregunta del usuario.
 *
 * @property question El texto de la pregunta del usuario.
 */
data class ChatbotRequest(
    val question: String
) 