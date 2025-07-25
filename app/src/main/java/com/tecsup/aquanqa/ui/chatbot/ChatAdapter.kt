package com.tecsup.aquanqa.ui.chatbot

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tecsup.aquanqa.databinding.ItemMessageReceivedBinding
import com.tecsup.aquanqa.databinding.ItemMessageSentBinding
import com.tecsup.aquanqa.databinding.ItemSuggestionHeaderBinding
import com.tecsup.aquanqa.databinding.ItemSuggestionQuestionBinding
import java.lang.IllegalArgumentException

/**
 * Adaptador unificado para toda la conversación del chatbot.
 * Utiliza un ListAdapter con un sealed class (ChatItem) para manejar
 * diferentes tipos de vistas: mensajes enviados, recibidos y sugerencias.
 */
class ChatAdapter(
    private val onSuggestionClicked: (String) -> Unit
) : ListAdapter<ChatItem, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    // Constantes para los tipos de vista
    private val VIEW_TYPE_MESSAGE_SENT = 1
    private val VIEW_TYPE_MESSAGE_RECEIVED = 2
    private val VIEW_TYPE_SUGGESTION_HEADER = 3
    private val VIEW_TYPE_SUGGESTION_ITEM = 4

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatItem.MessageItem -> when (item.message.sender) {
                Sender.USER -> VIEW_TYPE_MESSAGE_SENT
                Sender.CHATBOT -> VIEW_TYPE_MESSAGE_RECEIVED
            }
            is ChatItem.SuggestionHeader -> VIEW_TYPE_SUGGESTION_HEADER
            is ChatItem.SuggestionItem -> VIEW_TYPE_SUGGESTION_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_MESSAGE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(inflater, parent, false)
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_MESSAGE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(inflater, parent, false)
                ReceivedMessageViewHolder(binding)
            }
            VIEW_TYPE_SUGGESTION_HEADER -> {
                val binding = ItemSuggestionHeaderBinding.inflate(inflater, parent, false)
                SuggestionHeaderViewHolder(binding)
            }
            VIEW_TYPE_SUGGESTION_ITEM -> {
                val binding = ItemSuggestionQuestionBinding.inflate(inflater, parent, false)
                SuggestionItemViewHolder(binding, onSuggestionClicked)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatItem.MessageItem -> when (holder) {
                is SentMessageViewHolder -> holder.bind(item.message)
                is ReceivedMessageViewHolder -> holder.bind(item.message)
            }
            is ChatItem.SuggestionHeader -> (holder as SuggestionHeaderViewHolder).bind(item)
            is ChatItem.SuggestionItem -> (holder as SuggestionItemViewHolder).bind(item)
        }
    }

    // ViewHolders para cada tipo de ítem
    class SentMessageViewHolder(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.messageText.text = message.text
        }
    }

    class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            if (message.isLoading) {
                binding.messageText.text = "..." // O podrías usar un ValueAnimator para animar los puntos
                // Podrías añadir una animación aquí si quieres
            } else {
                binding.messageText.text = message.text
            }
        }
    }

    class SuggestionHeaderViewHolder(private val binding: ItemSuggestionHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatItem.SuggestionHeader) {
            binding.tvSuggestionHeader.text = item.text
        }
    }

    class SuggestionItemViewHolder(
        private val binding: ItemSuggestionQuestionBinding,
        private val onSuggestionClicked: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatItem.SuggestionItem) {
            binding.tvQuestion.text = item.question.question
            binding.root.setOnClickListener {
                onSuggestionClicked(item.question.question)
            }
        }
    }
}

/**
 * DiffUtil.Callback para el ListAdapter.
 * Compara los ítems para determinar si la lista ha cambiado.
 */
class ChatDiffCallback : DiffUtil.ItemCallback<ChatItem>() {
    override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        // Para los mensajes, el ID es único. Para los demás, la instancia o el contenido.
        return when {
            oldItem is ChatItem.MessageItem && newItem is ChatItem.MessageItem -> oldItem.message.id == newItem.message.id
            oldItem is ChatItem.SuggestionItem && newItem is ChatItem.SuggestionItem -> oldItem.question.id == newItem.question.id
            oldItem is ChatItem.SuggestionHeader && newItem is ChatItem.SuggestionHeader -> oldItem.text == newItem.text
            else -> oldItem == newItem
        }
    }

    override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        return oldItem == newItem
    }
} 