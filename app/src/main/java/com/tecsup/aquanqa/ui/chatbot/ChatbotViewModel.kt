package com.tecsup.aquanqa.ui.chatbot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.ui.chatbot.model.ChatbotRequest
import com.tecsup.aquanqa.ui.chatbot.model.RecommendedQuestion
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for the chatbot screen
 */
class ChatbotViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    private val _recommendedQuestions = MutableLiveData<List<RecommendedQuestion>>()
    val recommendedQuestions: LiveData<List<RecommendedQuestion>> = _recommendedQuestions

    private var sessionId: String? = null

    // MediatorLiveData para combinar mensajes y preguntas recomendadas
    val chatItems = MediatorLiveData<List<ChatItem>>()

    init {
        // Ejecutar la inicialización solo si el historial está vacío.
        if (_messages.value.isNullOrEmpty()) {
            addBotMessage("¡Hola! Soy Ara, tu asistente virtual de la empresa Aquanqa. ¿En qué puedo ayudarte hoy?")
            fetchRecommendedQuestions()
        }

        chatItems.addSource(_messages) { messages ->
            combineChatItems(messages, _recommendedQuestions.value)
        }
        chatItems.addSource(_recommendedQuestions) { questions ->
            combineChatItems(_messages.value, questions)
        }
    }

    private fun combineChatItems(messages: List<Message>?, questions: List<RecommendedQuestion>?) {
        val combinedList = mutableListOf<ChatItem>()
        combinedList.add(ChatItem.HeaderItem)
        messages?.map { ChatItem.MessageItem(it) }?.let { combinedList.addAll(it) }

        if (!questions.isNullOrEmpty()) {
            combinedList.add(ChatItem.RecommendedQuestionsItem(questions))
        }

        chatItems.value = combinedList
    }

    /**
     * Updates the current message being composed
     */
    fun setCurrentMessage(message: String) {
        // This function is no longer needed as the message is sent directly.
    }

    /**
     * Envía un mensaje del usuario al chatbot y obtiene una respuesta de la API.
     *
     * @param messageText El texto del mensaje enviado por el usuario.
     */
    fun sendMessage(messageText: String) {
        if (messageText.isBlank()) return
        
        addUserMessage(messageText)
        getBotResponse(messageText)
    }

    private fun fetchRecommendedQuestions() {
        viewModelScope.launch {
            try {
                val questions = ApiClient.chatbotApiService.getRecommendedQuestions()
                _recommendedQuestions.postValue(questions)
            } catch (e: Exception) {
                // Silently fail, no questions will be shown
            }
        }
    }

    /**
     * Agrega un mensaje del usuario a la lista de mensajes de la conversación.
     *
     * @param text El texto del mensaje del usuario.
     */
    private fun addUserMessage(text: String) {
        val currentMessages = _messages.value ?: emptyList()
        val newMessage = Message(UUID.randomUUID().toString(), text, Sender.USER)
        _messages.value = currentMessages + newMessage
    }

    /**
     * Agrega un mensaje del chatbot a la lista de mensajes de la conversación.
     *
     * @param text El texto del mensaje del chatbot.
     */
    private fun addBotMessage(text: String) {
        val currentMessages = _messages.value ?: emptyList()
        val newMessage = Message(UUID.randomUUID().toString(), text, Sender.CHATBOT)
        _messages.value = currentMessages + newMessage
    }

    /**
     * Realiza una llamada a la API del chatbot para obtener una respuesta basada en el mensaje del usuario.
     * Actualiza la lista de mensajes con la respuesta o un mensaje de error.
     *
     * @param userMessage El mensaje del usuario para el que se busca una respuesta.
     */
    private fun getBotResponse(userMessage: String) {
        viewModelScope.launch {
            try {
                val request = ChatbotRequest(question = userMessage, sessionId = sessionId)
                val response = ApiClient.chatbotApiService.sendMessage(request)
                sessionId = response.sessionId
                addBotMessage(response.answer)

                // Volver a cargar las preguntas recomendadas después de cada respuesta.
                fetchRecommendedQuestions()
            } catch (e: Exception) {
                addBotMessage("Lo siento, ocurrió un error. Por favor, inténtalo de nuevo más tarde.")
            }
        }
    }
} 