package com.tecsup.aquanqa.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tecsup.aquanqa.data.model.chatbot.RecommendedQuestion
import com.tecsup.aquanqa.data.model.chatbot.Message
import com.tecsup.aquanqa.data.model.chatbot.ChatItem
import com.tecsup.aquanqa.data.model.chatbot.Sender
import com.tecsup.aquanqa.data.repository.ChatbotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Singleton que mantiene la conversación del chatbot en memoria durante toda la ejecución de la app.
 * La conversación persiste aunque el usuario navegue entre fragmentos, pero se borra automáticamente
 * cuando se cierra completamente la aplicación (no usa almacenamiento persistente).
 * 
 * OPTIMIZACIÓN DE MEMORIA:
 * - Mantiene un máximo de MAX_MESSAGES_IN_MEMORY elementos en memoria
 * - Cuando se alcanza el límite, conserva el mensaje de bienvenida y los mensajes más recientes
 * - Esto previene problemas de memoria en conversaciones largas
 */
object ChatSessionManager {
    
    // Configuración de límites de memoria
    private const val MAX_MESSAGES_IN_MEMORY = 50
    private const val MESSAGES_TO_KEEP_WHEN_TRIMMING = 30
    
    // Scope de corrutinas para el ChatSessionManager
    private val sessionScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _chatItems = MutableLiveData<List<ChatItem>>()
    val chatItems: LiveData<List<ChatItem>> = _chatItems
    
    private val currentMessages = mutableListOf<ChatItem>()
    private var isInitialized = false
    private var welcomeMessageId: String? = null // Para identificar el mensaje de bienvenida
    
    /**
     * Inicializa la sesión de chat con el mensaje de bienvenida y preguntas frecuentes.
     * Solo se ejecuta una vez durante la vida de la aplicación.
     */
    fun initializeSession(repository: ChatbotRepository) {
        if (!isInitialized) {
            // Mensaje de bienvenida
            welcomeMessageId = UUID.randomUUID().toString()
            currentMessages.add(ChatItem.MessageItem(
                Message(welcomeMessageId!!, "¡Hola! Soy AquaBot, tu asistente virtual. ¿En qué puedo ayudarte hoy?", Sender.CHATBOT)
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
     * Intenta obtener las preguntas reales de la API y usa preguntas por defecto como fallback.
     */
    private fun loadInitialFrequentQuestions(repository: ChatbotRepository) {
        android.util.Log.d("ChatSessionManager", "Iniciando carga de preguntas frecuentes")
        
        // Emitir el estado actual primero (con solo el mensaje de bienvenida)
        _chatItems.postValue(currentMessages.toList())
        
        // Cargar preguntas frecuentes de forma asíncrona
        sessionScope.launch {
            android.util.Log.d("ChatSessionManager", "Llamando a repository.getFrequentQuestionsWithFallback()")
            
            try {
                val allFrequentQuestions = repository.getFrequentQuestionsWithFallback()
                android.util.Log.d("ChatSessionManager", "Preguntas obtenidas total: ${allFrequentQuestions.size}")
                
                // Limitar a solo las 4 más frecuentes
                val top4Questions = allFrequentQuestions.take(4)
                android.util.Log.d("ChatSessionManager", "Mostrando las ${top4Questions.size} preguntas más frecuentes")
                
                if (top4Questions.isNotEmpty()) {
                    // Agregar solo las 4 preguntas más frecuentes
                    currentMessages.add(ChatItem.SuggestionHeader("Preguntas frecuentes:"))
                    top4Questions.forEach { question ->
                        android.util.Log.d("ChatSessionManager", "Agregando pregunta: ${question.question}")
                        currentMessages.add(ChatItem.SuggestionItem(question))
                    }
                    
                    // Emitir el estado actualizado
                    _chatItems.postValue(currentMessages.toList())
                    android.util.Log.d("ChatSessionManager", "Preguntas frecuentes cargadas exitosamente")
                } else {
                    android.util.Log.w("ChatSessionManager", "No se obtuvieron preguntas frecuentes")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ChatSessionManager", "Error cargando preguntas frecuentes", e)
                
                // Fallback: usar preguntas por defecto
                val defaultQuestions = getDefaultQuestions()
                currentMessages.add(ChatItem.SuggestionHeader("Preguntas frecuentes:"))
                defaultQuestions.forEach { question ->
                    currentMessages.add(ChatItem.SuggestionItem(question))
                }
                
                _chatItems.postValue(currentMessages.toList())
                android.util.Log.d("ChatSessionManager", "Usando preguntas por defecto como fallback")
            }
        }
    }
    
    /**
     * Preguntas por defecto para usar como fallback cuando falla la API.
     */
    private fun getDefaultQuestions(): List<RecommendedQuestion> {
        return listOf(
            RecommendedQuestion(-1, "¿Cómo puedo contactar con soporte?"),
            RecommendedQuestion(-2, "¿Cuáles son los horarios de atención?"),
            RecommendedQuestion(-3, "¿Dónde puedo encontrar más información?"),
            RecommendedQuestion(-4, "¿Cómo puedo reportar un problema?")
        )
    }
    
    /**
     * Añade un mensaje del usuario a la conversación.
     */
    fun addUserMessage(messageText: String): String {
        val messageId = UUID.randomUUID().toString()
        val userMessage = ChatItem.MessageItem(Message(messageId, messageText, Sender.USER))
        currentMessages.add(userMessage)
        
        // Verificar si necesitamos optimizar memoria
        trimMessagesIfNeeded()
        
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
        
        // No necesitamos trimear aquí porque el loading indicator es temporal
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
        
        // Verificar si necesitamos optimizar memoria
        trimMessagesIfNeeded()
        
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
            
            // Verificar si necesitamos optimizar memoria después de añadir sugerencias
            trimMessagesIfNeeded()
            
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
     * Optimiza el uso de memoria eliminando mensajes antiguos cuando se alcanza el límite.
     * Mantiene siempre:
     * El mensaje de bienvenida
     * Los últimos MESSAGES_TO_KEEP_WHEN_TRIMMING mensajes
     *
     * Esta función se llama automáticamente cuando se añaden nuevos mensajes.
     */
    private fun trimMessagesIfNeeded() {
        if (currentMessages.size <= MAX_MESSAGES_IN_MEMORY) {
            return // No necesitamos optimizar aún
        }
        
        // Buscar el mensaje de bienvenida
        val welcomeMessage = currentMessages.find { item ->
            item is ChatItem.MessageItem && item.message.id == welcomeMessageId
        }
        
        // Obtener los mensajes más recientes (excluyendo el de bienvenida si está presente)
        val recentMessages = currentMessages
            .filter { item -> 
                // Excluir el mensaje de bienvenida del conteo de mensajes recientes
                !(item is ChatItem.MessageItem && item.message.id == welcomeMessageId)
            }
            .takeLast(MESSAGES_TO_KEEP_WHEN_TRIMMING)
        
        // Reconstruir la lista optimizada
        currentMessages.clear()
        
        // Añadir mensaje de bienvenida si existe
        welcomeMessage?.let { currentMessages.add(it) }
        
        // Si tenemos mensajes recientes, añadir un separador visual
        if (recentMessages.isNotEmpty() && welcomeMessage != null) {
            currentMessages.add(ChatItem.SuggestionHeader("--- Conversación anterior ---"))
        }
        
        // Añadir mensajes recientes
        currentMessages.addAll(recentMessages)
        
        // Log para debugging (opcional, puedes removerlo en producción)
        android.util.Log.d("ChatSessionManager", 
            "Memoria optimizada: ${currentMessages.size} mensajes mantenidos de un total que excedía $MAX_MESSAGES_IN_MEMORY")
    }
    
    /**
     * Obtiene estadísticas de uso de memoria del chat.
     * Útil para debugging y monitoreo.
     */
    fun getMemoryStats(): ChatMemoryStats {
        val messageCount = currentMessages.count { it is ChatItem.MessageItem }
        val suggestionCount = currentMessages.count { it is ChatItem.SuggestionItem }
        val headerCount = currentMessages.count { it is ChatItem.SuggestionHeader }
        
        return ChatMemoryStats(
            totalItems = currentMessages.size,
            messageItems = messageCount,
            suggestionItems = suggestionCount,
            headerItems = headerCount,
            isNearLimit = currentMessages.size > (MAX_MESSAGES_IN_MEMORY * 0.8).toInt()
        )
    }
    
    /**
     * Limpia la conversación (solo para casos especiales, normalmente no se usa).
     */
    fun clearSession() {
        currentMessages.clear()
        isInitialized = false
        welcomeMessageId = null
        _chatItems.postValue(emptyList())
    }
}

/**
 * Clase de datos para estadísticas de memoria del chat.
 */
data class ChatMemoryStats(
    val totalItems: Int,
    val messageItems: Int,
    val suggestionItems: Int,
    val headerItems: Int,
    val isNearLimit: Boolean
)