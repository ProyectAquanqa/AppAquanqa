package com.tecsup.aquanqa.ui.chatbot

import com.tecsup.aquanqa.ui.chatbot.model.RecommendedQuestion
import java.util.UUID

/**
 * Represents a message in the chatbot conversation
 * @property id Unique identifier for the message
 * @property text Content of the message
 * @property sender Who sent the message (user or chatbot)
 * @property timestamp When the message was sent
 */
data class Message(
    val id: String,
    val text: String,
    val sender: Sender,
    val isLoading: Boolean = false // Nuevo campo para el indicador de "escribiendo..."
)

/**
 * Enum to represent the sender of a message
 */
enum class Sender {
    USER,
    CHATBOT
}

/**
 * NUEVO: Un tipo sellado para representar diferentes tipos de ítems en el chat.
 * Esto nos permite tener mensajes normales, un encabezado para las sugerencias,
 * y las propias sugerencias, todo en una sola lista para el RecyclerView.
 */
sealed class ChatItem {
    data class MessageItem(val message: Message) : ChatItem()
    data class SuggestionHeader(val text: String) : ChatItem()
    data class SuggestionItem(val question: RecommendedQuestion) : ChatItem()
} 