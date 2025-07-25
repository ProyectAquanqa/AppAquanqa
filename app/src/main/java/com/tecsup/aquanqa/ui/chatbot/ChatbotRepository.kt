package com.tecsup.aquanqa.ui.chatbot

import com.tecsup.aquanqa.data.api.ApiService
import com.tecsup.aquanqa.ui.chatbot.model.ChatbotRequest
import com.tecsup.aquanqa.ui.chatbot.model.ChatbotResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

/**
 * Repositorio para gestionar las interacciones con el chatbot.
 * Es el intermediario entre el ViewModel y el servicio de la API del chatbot.
 */
class ChatbotRepository(private val apiService: ApiService) {

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
} 