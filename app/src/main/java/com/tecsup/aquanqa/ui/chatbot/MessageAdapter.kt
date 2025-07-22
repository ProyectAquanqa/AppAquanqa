package com.tecsup.aquanqa.ui.chatbot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.databinding.ItemRecommendedQuestionsListBinding
import com.tecsup.aquanqa.ui.chatbot.model.RecommendedQuestion

sealed class ChatItem {
    data class MessageItem(val message: Message) : ChatItem()
    data class RecommendedQuestionsItem(val questions: List<RecommendedQuestion>) : ChatItem()
    object HeaderItem : ChatItem()
}

class MessageAdapter(
    private val onQuestionClicked: (String) -> Unit
) : ListAdapter<ChatItem, RecyclerView.ViewHolder>(ChatItemDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_RECOMMENDED_QUESTIONS = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatItem.HeaderItem -> VIEW_TYPE_HEADER
            is ChatItem.MessageItem -> when (item.message.sender) {
                Sender.USER -> VIEW_TYPE_SENT
                Sender.CHATBOT -> VIEW_TYPE_RECEIVED
            }
            is ChatItem.RecommendedQuestionsItem -> VIEW_TYPE_RECOMMENDED_QUESTIONS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_chatbot_header, parent, false))
            VIEW_TYPE_SENT -> SentMessageViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false))
            VIEW_TYPE_RECEIVED -> ReceivedMessageViewHolder(inflater.inflate(R.layout.item_message_received, parent, false))
            VIEW_TYPE_RECOMMENDED_QUESTIONS -> {
                val binding = ItemRecommendedQuestionsListBinding.inflate(inflater, parent, false)
                RecommendedQuestionsViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatItem.MessageItem -> {
                when (holder) {
                    is SentMessageViewHolder -> holder.bind(item.message)
                    is ReceivedMessageViewHolder -> holder.bind(item.message)
                }
            }
            is ChatItem.RecommendedQuestionsItem -> (holder as RecommendedQuestionsViewHolder).bind(item)
            is ChatItem.HeaderItem -> { /* No-op */ }
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        fun bind(message: Message) {
            messageText.text = message.text
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        fun bind(message: Message) {
            messageText.text = message.text
        }
    }

    inner class RecommendedQuestionsViewHolder(private val binding: ItemRecommendedQuestionsListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatItem.RecommendedQuestionsItem) {
            val questionsAdapter = RecommendedQuestionsAdapter(onQuestionClicked)
            binding.recommendedQuestionsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                adapter = questionsAdapter
            }
            questionsAdapter.submitList(item.questions)
        }
    }

    class ChatItemDiffCallback : DiffUtil.ItemCallback<ChatItem>() {
        override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            return when {
                oldItem is ChatItem.MessageItem && newItem is ChatItem.MessageItem ->
                    oldItem.message.id == newItem.message.id
                oldItem is ChatItem.RecommendedQuestionsItem && newItem is ChatItem.RecommendedQuestionsItem ->
                    // Sólo puede haber una lista de preguntas a la vez
                    true
                else -> oldItem::class == newItem::class
            }
        }

        override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            return oldItem == newItem
        }
    }
} 