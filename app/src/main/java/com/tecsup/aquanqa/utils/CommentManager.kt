package com.tecsup.aquanqa.utils

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.button.MaterialButton
import com.tecsup.aquanqa.data.SessionManager
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.model.content.Anuncio
import com.tecsup.aquanqa.data.model.content.NuevoComentarioRequest
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.launch

/**
 * Gestor reutilizable para la funcionalidad de comentarios.
 * 
 * Esta clase maneja toda la lógica de comentarios de manera centralizada,
 * incluyendo la actualización automática de contadores.
 */
object CommentManager {

    // Mapa para mantener los callbacks de actualización de comentarios por evento
    private val commentUpdateCallbacks = mutableMapOf<Int, MutableList<(Int) -> Unit>>()

    /**
     * Configura un botón de comentarios con toda la funcionalidad necesaria.
     * 
     * @param button Botón de comentarios
     * @param anuncio Evento para mostrar comentarios
     * @param onCommentClick Callback para abrir modal de comentarios
     */
    fun setupCommentButton(
        button: MaterialButton,
        anuncio: Anuncio,
        onCommentClick: (Anuncio) -> Unit
    ) {
        // Inicializar contador local con el valor actual
        localCommentCounts[anuncio.id] = anuncio.comentariosCount
        
        // Actualizar texto con contador inicial
        updateCommentButtonUI(button, anuncio.comentariosCount)
        
        // Registrar callback para actualizaciones automáticas
        registerCommentUpdateCallback(anuncio.id) { newCount ->
            updateCommentButtonUI(button, newCount)
        }
        
        // Configurar click para abrir modal
        button.setOnClickListener {
            onCommentClick(anuncio)
        }
    }

    /**
     * Actualiza la interfaz visual del botón de comentarios.
     * 
     * @param button Botón a actualizar
     * @param comentariosCount Número total de comentarios
     */
    private fun updateCommentButtonUI(
        button: MaterialButton,
        comentariosCount: Int
    ) {
        button.text = if (comentariosCount > 0) {
            comentariosCount.toString()
        } else {
            ""
        }
    }

    /**
     * Registra un callback para recibir actualizaciones de comentarios.
     * 
     * @param eventoId ID del evento
     * @param callback Función que se ejecuta cuando cambia el contador
     */
    fun registerCommentUpdateCallback(eventoId: Int, callback: (Int) -> Unit) {
        val callbacks = commentUpdateCallbacks.getOrPut(eventoId) { mutableListOf() }
        callbacks.add(callback)
    }

    /**
     * Notifica a todos los callbacks registrados sobre un cambio en el contador.
     * 
     * @param eventoId ID del evento
     * @param newCount Nuevo número de comentarios
     */
    fun notifyCommentCountChanged(eventoId: Int, newCount: Int) {
        commentUpdateCallbacks[eventoId]?.forEach { callback ->
            callback(newCount)
        }
    }

    /**
     * Crea un nuevo comentario y actualiza automáticamente los contadores.
     * 
     * @param context Contexto para acceder a SessionManager
     * @param eventoId ID del evento
     * @param content Contenido del comentario
     * @param lifecycleScope Scope para la corrutina
     * @param onResult Callback con el resultado (success: Boolean, newCount: Int)
     */
    fun createComment(
        context: Context,
        eventoId: Int,
        content: String,
        lifecycleScope: LifecycleCoroutineScope,
        onResult: (Boolean, Int) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val userPreferences = UserPreferences(context)
                val sessionManager = SessionManager(context, userPreferences)
                val token = sessionManager.getValidAccessToken()
                
                if (token != null) {
                    val request = NuevoComentarioRequest(eventoId, content)
                    val response = ApiClient.apiService.crearComentario("Bearer $token", request)
                    
                    if (response.isSuccessful) {
                        val nuevoComentario = response.body()
                        if (nuevoComentario != null) {
                            // Incrementar contador localmente (más eficiente)
                            updateCommentCountLocally(eventoId, increment = true)
                            val newCount = localCommentCounts[eventoId] ?: 1
                            
                            // Ejecutar callback con el resultado
                            onResult(true, newCount)
                        } else {
                            onResult(false, 0)
                        }
                    } else {
                        Toast.makeText(context, "Error al enviar comentario", Toast.LENGTH_SHORT).show()
                        onResult(false, 0)
                    }
                } else {
                    Toast.makeText(context, "Error de autenticación", Toast.LENGTH_SHORT).show()
                    onResult(false, 0)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                onResult(false, 0)
            }
        }
    }

    /**
     * Elimina un comentario y actualiza automáticamente los contadores.
     * 
     * @param context Contexto para acceder a SessionManager
     * @param eventoId ID del evento
     * @param comentarioId ID del comentario a eliminar
     * @param lifecycleScope Scope para la corrutina
     * @param onResult Callback con el resultado (success: Boolean, newCount: Int)
     */
    fun deleteComment(
        context: Context,
        eventoId: Int,
        comentarioId: Int,
        lifecycleScope: LifecycleCoroutineScope,
        onResult: (Boolean, Int) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val userPreferences = UserPreferences(context)
                val sessionManager = SessionManager(context, userPreferences)
                val token = sessionManager.getValidAccessToken()
                
                if (token != null) {
                    val response = ApiClient.apiService.eliminarComentario("Bearer $token", comentarioId)
                    
                    if (response.isSuccessful) {
                        // Decrementar contador localmente (más eficiente)
                        updateCommentCountLocally(eventoId, increment = false)
                        val newCount = localCommentCounts[eventoId] ?: 0
                        
                        // Ejecutar callback con el resultado
                        onResult(true, newCount)
                    } else {
                        Toast.makeText(context, "No se pudo eliminar el comentario", Toast.LENGTH_SHORT).show()
                        onResult(false, 0)
                    }
                } else {
                    Toast.makeText(context, "Error de autenticación", Toast.LENGTH_SHORT).show()
                    onResult(false, 0)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                onResult(false, 0)
            }
        }
    }



    /**
     * Versión simplificada para incrementar/decrementar contador localmente.
     * Más eficiente que hacer llamadas adicionales a la API.
     */
    fun updateCommentCountLocally(eventoId: Int, increment: Boolean) {
        // Mantener un cache local simple de contadores
        val currentCount = localCommentCounts[eventoId] ?: 0
        val newCount = if (increment) currentCount + 1 else maxOf(0, currentCount - 1)
        localCommentCounts[eventoId] = newCount
        
        // Notificar cambios
        notifyCommentCountChanged(eventoId, newCount)
    }

    // Cache local simple para contadores
    private val localCommentCounts = mutableMapOf<Int, Int>()

    /**
     * Limpia los callbacks registrados para un evento específico.
     * Útil para evitar memory leaks cuando se destruye una vista.
     * 
     * @param eventoId ID del evento
     */
    fun clearCallbacks(eventoId: Int) {
        commentUpdateCallbacks.remove(eventoId)
        localCommentCounts.remove(eventoId)
    }

    /**
     * Limpia todos los callbacks registrados.
     * Útil para limpiar memoria cuando sea necesario.
     */
    fun clearAllCallbacks() {
        commentUpdateCallbacks.clear()
        localCommentCounts.clear()
    }
}