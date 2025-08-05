package com.tecsup.aquanqa.ui.chatbot

import com.tecsup.aquanqa.data.repository.ChatbotRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de ChatbotViewModel con sus dependencias.
 * Esto es crucial para la inyección de dependencias y para asegurar que el ViewModel
 * reciba el ChatbotRepository que necesita para funcionar.
 */
class ChatbotViewModelFactory(private val repository: ChatbotRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatbotViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatbotViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 