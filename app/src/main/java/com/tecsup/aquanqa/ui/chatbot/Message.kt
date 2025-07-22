package com.tecsup.aquanqa.ui.chatbot

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
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Enum to represent the sender of a message
 */
enum class Sender {
    USER,
    CHATBOT
} 