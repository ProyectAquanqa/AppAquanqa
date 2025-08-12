package com.tecsup.aquanqa.data.repository

import com.tecsup.aquanqa.data.api.ChatbotApiService
import com.tecsup.aquanqa.data.model.chatbot.ChatbotRequest
import com.tecsup.aquanqa.data.model.chatbot.ChatbotResponse
import com.tecsup.aquanqa.data.model.chatbot.RecommendedQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Repositorio para gestionar las interacciones con el chatbot.
 * Es el intermediario entre el ViewModel y el servicio de la API del chatbot.
 */
class ChatbotRepository(private val chatbotApiService: ChatbotApiService) {

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
                val response = chatbotApiService.sendMessage(request)

                if (response.isSuccessful) {
                    response.body()?.let { apiResponse ->
                        when (apiResponse.status) {
                            "success" -> {
                                apiResponse.data?.let { chatbotData ->
                                    Result.success(chatbotData)
                                } ?: Result.failure(Exception("Los datos del chatbot están vacíos."))
                            }
                            "error" -> {
                                Result.failure(Exception(apiResponse.error ?: "Error desconocido del chatbot"))
                            }
                            else -> {
                                Result.failure(Exception("Estado de respuesta desconocido: ${apiResponse.status}"))
                            }
                        }
                    } ?: Result.failure(Exception("La respuesta del chatbot está vacía."))
                } else {
                    Result.failure(IOException("No se pudo procesar tu consulta. Intenta nuevamente"))
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
                android.util.Log.d("ChatbotRepository", "Iniciando llamada a getFrequentQuestions()")
                val response = chatbotApiService.getFrequentQuestions()
                
                android.util.Log.d("ChatbotRepository", "Respuesta recibida - Código: ${response.code()}, Exitosa: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    response.body()?.let { apiResponse ->
                        android.util.Log.d("ChatbotRepository", "Status de la API: ${apiResponse.status}")
                        
                        when (apiResponse.status) {
                            "success" -> {
                                apiResponse.data?.let { questionsData ->
                                    android.util.Log.d("ChatbotRepository", "Preguntas recibidas: ${questionsData.recommendedQuestions.size}")
                                    questionsData.recommendedQuestions.forEach { question ->
                                        android.util.Log.d("ChatbotRepository", "Pregunta ID: ${question.id}, Texto: ${question.question}")
                                    }
                                    Result.success(questionsData.recommendedQuestions)
                                } ?: run {
                                    android.util.Log.e("ChatbotRepository", "Los datos de preguntas frecuentes están vacíos")
                                    Result.failure(Exception("Los datos de preguntas frecuentes están vacíos."))
                                }
                            }
                            "error" -> {
                                android.util.Log.e("ChatbotRepository", "Error de la API: ${apiResponse.error}")
                                Result.failure(Exception(apiResponse.error ?: "Error desconocido al obtener preguntas"))
                            }
                            else -> {
                                android.util.Log.e("ChatbotRepository", "Estado desconocido: ${apiResponse.status}")
                                Result.failure(Exception("Estado de respuesta desconocido: ${apiResponse.status}"))
                            }
                        }
                    } ?: run {
                        android.util.Log.e("ChatbotRepository", "La respuesta del cuerpo está vacía")
                        Result.failure(Exception("La respuesta de preguntas frecuentes está vacía."))
                    }
                } else {
                    android.util.Log.e("ChatbotRepository", "Error del servidor: ${response.code()} - ${response.message()}")
                    android.util.Log.e("ChatbotRepository", "Cuerpo del error: ${response.errorBody()?.string()}")
                    Result.failure(IOException("No se pudieron cargar las preguntas frecuentes. Intenta más tarde"))
                }
            } catch (e: IOException) {
                android.util.Log.e("ChatbotRepository", "IOException en getFrequentQuestions", e)
                Result.failure(e)
            } catch (e: Exception) {
                android.util.Log.e("ChatbotRepository", "Exception en getFrequentQuestions", e)
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
            RecommendedQuestion(-2, "¿Cuáles son los horarios de trabajo?"),
            RecommendedQuestion(-3, "¿Dónde puedo encontrar más información?"),
            RecommendedQuestion(-4, "¿Cómo puedo reportar un problema?")
        )
    }
} 