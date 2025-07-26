package com.tecsup.aquanqa.ui.chatbot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tecsup.aquanqa.ui.chatbot.model.RecommendedQuestion
import java.util.UUID

/**
 * Singleton que mantiene la conversación del chatbot en memoria durante toda la ejecución de la app.
 * La conversación persiste aunque el usuario navegue entre fragmentos, pero se borra automáticamente
 * cuando se cierra completamente la aplicación (no usa almacenamiento persistente).
 */
object ChatSessionManager {
    
    private val _chatItems = MutableLiveData<List<ChatItem>>()
    val chatItems: LiveData<List<ChatItem>> = _chatItems
    
    private val currentMessages = mutableListOf<ChatItem>()
    private var isInitialized = false
    
    /**
     * Inicializa la sesión de chat con el mensaje de bienvenida y preguntas frecuentes.
     * Solo se ejecuta una vez durante la vida de la aplicación.
     */
    fun initializeSession(repository: ChatbotRepository) {
        if (!isInitialized) {
            // Mensaje de bienvenida
            currentMessages.add(ChatItem.MessageItem(
                Message(UUID.randomUUID().toString(), "¡Hola! Soy AquaBot, tu asistente virtual. ¿En qué puedo ayudarte hoy?", Sender.CHATBOT)
            ))
            
            // Cargar preguntas frecuentes iniciales
            loadInitialFrequentQuestions(repository)
            isInitialized = true
        } else {
            // Si ya está inicializada, solo emitir el estado actual
            _chatItems.postValue(currentMessages.toList())
        }
    }
    
    /**
     * Carga las preguntas más frecuentes al inicio de la conversación.
     */
    private fun loadInitialFrequentQuestions(repository: ChatbotRepository) {
        // Nota: Esto se ejecuta de forma síncrona usando las preguntas por defecto
        // para evitar problemas de concurrencia en el singleton
        val defaultQuestions = listOf(
            RecommendedQuestion(-1, "¿Cómo puedo contactar con soporte?"),
            RecommendedQuestion(-2, "¿Cuáles son los horarios de atención?"),
            RecommendedQuestion(-3, "¿Dónde puedo encontrar más información?"),
            RecommendedQuestion(-4, "¿Cómo puedo reportar un problema?")
        )
        
        if (defaultQuestions.isNotEmpty()) {
            currentMessages.add(ChatItem.SuggestionHeader("Preguntas frecuentes:"))
            defaultQuestions.forEach { question ->
                currentMessages.add(ChatItem.SuggestionItem(question))
            }
        }
        
        _chatItems.postValue(currentMessages.toList())
    }
    
    /**
     * Añade un mensaje del usuario a la conversación.
     */
    fun addUserMessage(messageText: String): String {
        val messageId = UUID.randomUUID().toString()
        val userMessage = ChatItem.MessageItem(Message(messageId, messageText, Sender.USER))
        currentMessages.add(userMessage)
        _chatItems.postValue(currentMessages.toList())
        return messageId
    }
    
    /**
     * Añade un indicador de "escribiendo..." del bot.
     */
    fun addLoadingIndicator(): String {
        val loadingId = UUID.randomUUID().toString()
        val loadingIndicator = ChatItem.MessageItem(Message(loadingId, "...", Sender.CHATBOT, isLoading = true))
        currentMessages.add(loadingIndicator)
        _chatItems.postValue(currentMessages.toList())
        return loadingId
    }
    
    /**
     * Remueve el indicador de "escribiendo..." por su ID.
     */
    fun removeLoadingIndicator(loadingId: String) {
        currentMessages.removeAll { item ->
            item is ChatItem.MessageItem && item.message.id == loadingId && item.message.isLoading
        }
        _chatItems.postValue(currentMessages.toList())
    }
    
    /**
     * Añade una respuesta del bot a la conversación.
     */
    fun addBotMessage(responseText: String) {
        val botMessage = ChatItem.MessageItem(
            Message(UUID.randomUUID().toString(), responseText, Sender.CHATBOT)
        )
        currentMessages.add(botMessage)
        _chatItems.postValue(currentMessages.toList())
    }
    
    /**
     * Añade preguntas sugeridas a la conversación.
     */
    fun addSuggestions(suggestions: List<RecommendedQuestion>, headerText: String = "Quizás quieras preguntar:") {
        if (suggestions.isNotEmpty()) {
            currentMessages.add(ChatItem.SuggestionHeader(headerText))
            suggestions.forEach { question ->
                currentMessages.add(ChatItem.SuggestionItem(question))
            }
            _chatItems.postValue(currentMessages.toList())
        }
    }
    
    /**
     * Obtiene el estado actual de la conversación.
     */
    fun getCurrentMessages(): List<ChatItem> {
        return currentMessages.toList()
    }
    
    /**
     * Limpia la conversación (solo para casos especiales, normalmente no se usa).
     */
    fun clearSession() {
        currentMessages.clear()
        isInitialized = false
        _chatItems.postValue(emptyList())
    }
}