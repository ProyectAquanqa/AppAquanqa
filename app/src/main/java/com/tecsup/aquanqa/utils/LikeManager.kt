package com.tecsup.aquanqa.utils

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.button.MaterialButton
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.data.SessionManager
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.model.content.Anuncio
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.launch

/**
 * Gestor reutilizable para la funcionalidad de likes.
 * 
 * Esta clase maneja toda la lógica de likes de manera centralizada,
 * evitando duplicación de código entre diferentes fragments/adapters.
 */
object LikeManager {

    /**
     * Configura un botón de like con toda la funcionalidad necesaria.
     * 
     * @param context Contexto para acceder a recursos y preferencias
     * @param button Botón que funcionará como like/unlike
     * @param anuncio Evento al que se aplicará el like
     * @param lifecycleScope Scope para manejar la corrutina
     * @param onLikeChanged Callback que se ejecuta cuando cambia el estado del like
     */
    fun setupLikeButton(
        context: Context,
        button: MaterialButton,
        anuncio: Anuncio,
        lifecycleScope: LifecycleCoroutineScope,
        onLikeChanged: ((Boolean, Int) -> Unit)? = null
    ) {
        // Configurar estado inicial
        updateLikeButtonUI(button, anuncio.userHasLiked, anuncio.likesCount)
        
        // Configurar click listener
        button.setOnClickListener {
            toggleLike(context, anuncio, lifecycleScope) { liked, count ->
                // Actualizar UI del botón inmediatamente
                updateLikeButtonUI(button, liked, count)
                // Llamar callback para actualizar otros elementos si es necesario
                onLikeChanged?.invoke(liked, count)
            }
        }
    }

    /**
     * Actualiza la interfaz visual del botón de like.
     * 
     * @param button Botón a actualizar
     * @param userHasLiked Si el usuario ya dio like
     * @param likesCount Número total de likes
     */
    private fun updateLikeButtonUI(
        button: MaterialButton,
        userHasLiked: Boolean,
        likesCount: Int
    ) {
        button.apply {
            if (userHasLiked) {
                // Estado: YA DIO LIKE (rojo/lleno)
                setIconResource(R.drawable.ic_like_filled)
                setIconTintResource(R.color.error_color)
                text = if (likesCount > 0) likesCount.toString() else ""
            } else {
                // Estado: NO HA DADO LIKE (gris/outline)
                setIconResource(R.drawable.ic_like_outline)
                setIconTintResource(R.color.text_secondary)
                text = if (likesCount > 0) likesCount.toString() else ""
            }
        }
    }

    /**
     * Realiza el toggle de like llamando a la API.
     * 
     * @param context Contexto para acceder a SessionManager
     * @param anuncio Evento al que aplicar el toggle
     * @param lifecycleScope Scope para la corrutina
     * @param onResult Callback con el resultado (liked: Boolean, count: Int)
     */
    private fun toggleLike(
        context: Context,
        anuncio: Anuncio,
        lifecycleScope: LifecycleCoroutineScope,
        onResult: (Boolean, Int) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val userPreferences = UserPreferences(context)
                val sessionManager = SessionManager(context, userPreferences)
                val token = sessionManager.getValidAccessToken()
                
                if (token != null) {
                    val response = ApiClient.apiService.toggleLike("Bearer $token", anuncio.id)
                    
                    if (response.isSuccessful) {
                        val likeResponse = response.body()
                        if (likeResponse != null) {
                            // Actualizar estado del anuncio
                            updateAnuncioLikeState(anuncio, likeResponse.liked, likeResponse.likesCount)
                            
                            // Ejecutar callback con el nuevo estado
                            onResult(likeResponse.liked, likeResponse.likesCount)

                        }
                    } else {
                        Toast.makeText(context, "Error al procesar like", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Error de autenticación", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Actualiza el estado interno del objeto Anuncio.
     * Nota: Como es un data class inmutable, no podemos modificarlo directamente,
     * pero podemos mantener el estado para futuras referencias.
     */
    private fun updateAnuncioLikeState(anuncio: Anuncio, liked: Boolean, count: Int) {
        // En un caso real, podrías querer actualizar un cache o state manager
        // Por ahora, el estado se mantiene en memoria durante la sesión
    }

    /**
     * Configura un botón de comentarios con navegación al modal.
     * 
     * @param button Botón de comentarios
     * @param anuncio Evento para mostrar comentarios
     * @param comentariosCount Número actual de comentarios
     * @param onCommentClick Callback para abrir modal de comentarios
     */
    fun setupCommentButton(
        button: MaterialButton,
        anuncio: Anuncio,
        comentariosCount: Int,
        onCommentClick: (Anuncio) -> Unit
    ) {
        // Delegar al CommentManager para manejo completo de comentarios
        CommentManager.setupCommentButton(button, anuncio, onCommentClick)
    }
}
