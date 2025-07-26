package com.tecsup.aquanqa.ui.chatbot

import com.tecsup.aquanqa.data.api.ApiService
import com.tecsup.aquanqa.data.api.ChatbotApiService
import com.tecsup.aquanqa.ui.chatbot.model.ChatbotRequest
import com.tecsup.aquanqa.ui.chatbot.model.ChatbotResponse
import com.tecsup.aquanqa.ui.chatbot.model.RecommendedQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

/**
 * Repositorio para gestionar las interacciones con el chatbot.
 * Es el intermediario entre el ViewModel y el servicio de la API del chatbot.
 */
class ChatbotRepository(private val apiService: ApiService, private val chatbotApiService: ChatbotApiService) {

    /**
     * Envía una pregunta al backend del chatbot y devuelve la respuesta.
     *
     * @param question La pregunta del usuario.
     * @return Un objeto Result que contiene la ChatbotResponse si la llamada es exitosa,
     *         o una excepción si falla.
     */
    suspend fun postQuery(question: String): Result<ChatbotResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = ChatbotRequest(question = question)
                val response = apiService.postChatbotQuery(request)

                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("La respuesta del chatbot está vacía."))
                } else {
                    Result.failure(IOException("Error en la respuesta del servidor: ${response.code()}"))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Obtiene las preguntas más frecuentes desde la API.
     * Estas se usan como respaldo cuando no hay preguntas específicas recomendadas.
     *
     * @return Un objeto Result que contiene la lista de preguntas frecuentes si la llamada es exitosa,
     *         o una excepción si falla.
     */
    suspend fun getFrequentQuestions(): Result<List<RecommendedQuestion>> {
        return withContext(Dispatchers.IO) {
            try {
                val questions = chatbotApiService.getFrequentQuestions()
                Result.success(questions)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Obtiene las preguntas más frecuentes con respaldo a preguntas por defecto.
     * Garantiza que siempre se retornen preguntas, incluso si la API falla.
     *
     * @return Lista de preguntas frecuentes o preguntas por defecto si falla la API.
     */
    suspend fun getFrequentQuestionsWithFallback(): List<RecommendedQuestion> {
        return getFrequentQuestions().getOrElse {
            // Preguntas por defecto en caso de que falle la API
            getDefaultQuestions()
        }
    }

    /**
     * Preguntas por defecto para usar cuando falla todo lo demás.
     * Estas están hardcodeadas y siempre estarán disponibles.
     */
    private fun getDefaultQuestions(): List<RecommendedQuestion> {
        return listOf(
            RecommendedQuestion(-1, "¿Cómo puedo contactar con soporte?"),
            RecommendedQuestion(-2, "¿Cuáles son los horarios de atención?"),
            RecommendedQuestion(-3, "¿Dónde puedo encontrar más información?"),
            RecommendedQuestion(-4, "¿Cómo puedo reportar un problema?")
        )
    }
} 