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

    init {
        // Inicializar la sesión de chat (solo se ejecuta una vez por app)
        ChatSessionManager.initializeSession(repository)
        
        // Observar los cambios en la conversación desde el SessionManager
        ChatSessionManager.chatItems.observeForever { items ->
            _chatUiState.postValue(ChatUiState.Success(items))
        }
    }



    fun sendMessage(userMessageText: String) {
        if (userMessageText.isBlank()) return

        // 1. Añadir el mensaje del usuario usando el SessionManager
        ChatSessionManager.addUserMessage(userMessageText)
        
        // 2. Añadir indicador de "escribiendo..." usando el SessionManager
        val loadingId = ChatSessionManager.addLoadingIndicator()

        // 3. Lanzar la corrutina para obtener la respuesta del bot
        viewModelScope.launch {
            val result = repository.postQuery(userMessageText)
            
            // Eliminar el indicador de "escribiendo..."
            ChatSessionManager.removeLoadingIndicator(loadingId)

            result.fold(
                onSuccess = { response ->
                    // Añadir la respuesta del bot usando el SessionManager
                    ChatSessionManager.addBotMessage(response.answer)
                    
                    // SIEMPRE mostrar preguntas sugeridas
                    addSuggestionsAfterResponse(response)
                },
                onFailure = {
                    // Manejar el error usando el SessionManager
                    ChatSessionManager.addBotMessage("Lo siento, tuve un problema para conectarme. Por favor, inténtalo de nuevo.")
                    
                    // Incluso en caso de error, mostrar preguntas frecuentes para que el usuario pueda continuar
                    addFallbackSuggestions()
                }
            )
        }
    }

    /**
     * Añade preguntas sugeridas después de cada respuesta del bot.
     * SIEMPRE muestra preguntas, usando la siguiente lógica de prioridad:
     * 1. Si hay recommendedQuestions específicas → mostrar esas
     * 2. Si NO hay recommendedQuestions → obtener las 4 preguntas más frecuentes de la base de datos
     * 3. Si falla todo → mostrar preguntas por defecto
     */
    private fun addSuggestionsAfterResponse(response: com.tecsup.aquanqa.ui.chatbot.model.ChatbotResponse) {
        viewModelScope.launch {
            val suggestionsToShow = if (response.recommendedQuestions.isNotEmpty()) {
                // Caso 1: Hay preguntas específicas recomendadas
                response.recommendedQuestions
            } else {
                // Caso 2: No hay preguntas específicas, obtener las más frecuentes
                repository.getFrequentQuestionsWithFallback().take(4)
            }

            // Añadir las sugerencias al chat usando el SessionManager
            ChatSessionManager.addSuggestions(suggestionsToShow)
        }
    }

    /**
     * Añade preguntas de respaldo cuando hay un error en la API principal.
     * Garantiza que el usuario siempre tenga opciones para continuar la conversación.
     */
    private fun addFallbackSuggestions() {
        viewModelScope.launch {
            val fallbackQuestions = repository.getFrequentQuestionsWithFallback().take(4)
            
            // Añadir las preguntas de respaldo usando el SessionManager
            ChatSessionManager.addSuggestions(fallbackQuestions)
        }
    }
} 
