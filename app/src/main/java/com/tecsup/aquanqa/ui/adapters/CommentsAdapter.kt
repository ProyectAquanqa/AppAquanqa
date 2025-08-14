package com.tecsup.aquanqa.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.databinding.ItemCommentBinding
import com.tecsup.aquanqa.data.model.content.ComentarioResponse
import com.tecsup.aquanqa.data.model.content.ComentarioUsuario
import com.tecsup.aquanqa.utils.DateUtils
import com.tecsup.aquanqa.utils.ImageLoadingUtils

/**
 * Adapter para el RecyclerView que muestra la lista de comentarios.
 */
class CommentsAdapter(
    private var comentarios: List<ComentarioResponse>,
    private val onOptionsClick: ((View, ComentarioResponse) -> Unit)? = null,
    private val currentUserId: Int? = null
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comentarios[position], onOptionsClick, currentUserId)
    }

    override fun getItemCount(): Int = comentarios.size

    /**
     * Getter para acceder a la lista de comentarios desde fuera.
     */
    fun getComentarios(): List<ComentarioResponse> = comentarios

    /**
     * Actualiza la lista de comentarios.
     */
    fun updateComments(newComentarios: List<ComentarioResponse>) {
        comentarios = newComentarios
        notifyDataSetChanged()
    }

    /**
     * Agrega un nuevo comentario al inicio de la lista.
     */
    fun addComment(comentario: ComentarioResponse) {
        comentarios = listOf(comentario) + comentarios
        notifyItemInserted(0)
    }

    /**
     * Elimina un comentario de la lista.
     */
    fun removeComment(comentarioId: Int) {
        val index = comentarios.indexOfFirst { it.id == comentarioId }
        if (index != -1) {
            comentarios = comentarios.toMutableList().apply { removeAt(index) }
            notifyItemRemoved(index)
        }
    }

    /**
     * ViewHolder para cada comentario.
     */
    class CommentViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            comentario: ComentarioResponse,
            onOptionsClick: ((View, ComentarioResponse) -> Unit)?,
            currentUserId: Int?
        ) {
            // Configurar nombre del usuario
            binding.tvUserName.text = comentario.usuario.fullName

            // Configurar tiempo transcurrido
            binding.tvTimeAgo.text = DateUtils.getTimeAgo(comentario.createdAt)

            // Configurar contenido del comentario
            binding.tvCommentContent.text = comentario.contenido

            // Cargar foto de perfil del usuario
            ImageLoadingUtils.loadProfileImage(
                context = itemView.context,
                imageView = binding.ivUserPhoto,
                imageUrl = comentario.usuario.fotoPerfil,
                useCircleCrop = true
            )

            // Mostrar menú de opciones solo si es el comentario del usuario actual
            val isOwnComment = currentUserId != null && comentario.usuario.id == currentUserId
            binding.ivOptionsMenu.visibility = if (isOwnComment) View.VISIBLE else View.GONE

            if (isOwnComment) {
                binding.ivOptionsMenu.setOnClickListener { anchorView ->
                    onOptionsClick?.invoke(anchorView, comentario)
                }
            }
        }
    }
}
