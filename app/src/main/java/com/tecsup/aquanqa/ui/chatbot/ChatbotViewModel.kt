package com.tecsup.aquanqa.ui.chatbot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.ui.chatbot.model.RecommendedQuestion
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ChatUiState {
    object Loading : ChatUiState()
    data class Success(val items: List<ChatItem>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

class ChatbotViewModel(private val repository: ChatbotRepository) : ViewModel() {

    private val _chatUiState = MutableLiveData<ChatUiState>()
    val chatUiState: LiveData<ChatUiState> = _chatUiState

    private val currentMessages = mutableListOf<ChatItem>()

    init {
        loadInitialWelcome()
    }

    private fun loadInitialWelcome() {
        currentMessages.add(ChatItem.MessageItem(
            Message(UUID.randomUUID().toString(), "¡Hola! Soy AquaBot, tu asistente virtual. ¿En qué puedo ayudarte hoy?", Sender.CHATBOT)
        ))
        // Aquí podrías cargar preguntas frecuentes si lo deseas en el futuro
        _chatUiState.value = ChatUiState.Success(currentMessages.toList())
    }

    fun sendMessage(userMessageText: String) {
        if (userMessageText.isBlank()) return

        // 1. Añadir el mensaje del usuario inmediatamente y mostrar estado de carga
        val userMessage = ChatItem.MessageItem(Message(UUID.randomUUID().toString(), userMessageText, Sender.USER))
        currentMessages.add(userMessage)
        
        // Añadir un indicador de que el bot está "escribiendo..."
        val loadingIndicator = ChatItem.MessageItem(Message(UUID.randomUUID().toString(), "...", Sender.CHATBOT, isLoading = true))
        currentMessages.add(loadingIndicator)

        _chatUiState.value = ChatUiState.Success(currentMessages.toList())


        // 2. Lanzar la corrutina para obtener la respuesta del bot
        viewModelScope.launch {
            val result = repository.postQuery(userMessageText)
            
            // Eliminar el indicador de "escribiendo..."
            currentMessages.remove(loadingIndicator)

            result.fold(
                onSuccess = { response ->
                    // Añadir la respuesta del bot
                    currentMessages.add(ChatItem.MessageItem(
                        Message(UUID.randomUUID().toString(), response.answer, Sender.CHATBOT)
                    ))
                    // Si hay preguntas recomendadas, añadirlas
                    if (response.recommendedQuestions.isNotEmpty()) {
                        currentMessages.add(ChatItem.SuggestionHeader("Quizás quieras preguntar:"))
                        response.recommendedQuestions.forEach {
                            currentMessages.add(ChatItem.SuggestionItem(it))
                        }
                    }
                    _chatUiState.postValue(ChatUiState.Success(currentMessages.toList()))
                },
                onFailure = {
                    // Manejar el error
                    currentMessages.add(ChatItem.MessageItem(
                        Message(UUID.randomUUID().toString(), "Lo siento, tuve un problema para conectarme. Por favor, inténtalo de nuevo.", Sender.CHATBOT)
                    ))
                    _chatUiState.postValue(ChatUiState.Success(currentMessages.toList()))
                }
            )
        }
    }
} 
