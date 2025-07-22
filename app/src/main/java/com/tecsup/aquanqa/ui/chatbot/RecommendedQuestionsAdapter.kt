package com.tecsup.aquanqa.ui.chatbot

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tecsup.aquanqa.databinding.ItemRecommendedQuestionBinding
import com.tecsup.aquanqa.ui.chatbot.model.RecommendedQuestion

class RecommendedQuestionsAdapter(
    private val onQuestionClicked: (String) -> Unit
) : ListAdapter<RecommendedQuestion, RecommendedQuestionsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecommendedQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemRecommendedQuestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onQuestionClicked(getItem(adapterPosition).question)
            }
        }

        fun bind(item: RecommendedQuestion) {
            binding.questionTextView.text = item.question
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecommendedQuestion>() {
        override fun areItemsTheSame(oldItem: RecommendedQuestion, newItem: RecommendedQuestion) =
            oldItem.question == newItem.question

        override fun areContentsTheSame(oldItem: RecommendedQuestion, newItem: RecommendedQuestion) =
            oldItem == newItem
    }
} 