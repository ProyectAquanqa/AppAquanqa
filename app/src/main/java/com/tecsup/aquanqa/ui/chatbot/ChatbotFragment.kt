package com.tecsup.aquanqa.ui.chatbot

import com.tecsup.aquanqa.data.repository.ChatbotRepository

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.databinding.FragmentChatbotBinding
import com.tecsup.aquanqa.ui.adapters.ChatAdapter
import com.tecsup.aquanqa.BuildConfig
import com.tecsup.aquanqa.utils.ValidationResult

/**
 * Fragmento que representa la pantalla del chatbot.
 * Su responsabilidad es simple: observar la lista de 'ChatItems' del ViewModel
 * y enviarla al 'ChatAdapter' para que se dibuje en la pantalla.
 */
class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatbotViewModel
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Configuración de la inyección de dependencias manual
        val chatbotApiService = ApiClient.chatbotApiService
        val repository = ChatbotRepository(chatbotApiService)
        val factory = ChatbotViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ChatbotViewModel::class.java]
        
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // El adapter ahora maneja todos los tipos de ítems.
        chatAdapter = ChatAdapter { questionText ->
            viewModel.sendMessage(questionText)
        }

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
    }

        // Observar el nuevo estado de la UI (ChatUiState).
        viewModel.chatUiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ChatUiState.Loading -> {
                    // Opcional: Podrías mostrar un ProgressBar general aquí
                }
                is ChatUiState.Success -> {
                    // Ocultar cualquier ProgressBar general
                    chatAdapter.submitList(state.items) {
                        // Desplazarse al final para ver los mensajes más recientes.
                        binding.rvMessages.scrollToPosition(state.items.size - 1)
                        
                        // Log de estadísticas de memoria (solo para debugging)
                        logMemoryStatsIfNeeded()
                    }
                }
                is ChatUiState.Error -> {
                    // Opcional: Mostrar un Snackbar o Toast con state.message
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                is ChatUiState.ValidationError -> {
                    // Mostrar error de validación al usuario
                    Snackbar.make(binding.root, state.error, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
                        .show()
                }
            }
        }

        binding.sendButton.setOnClickListener {
            val messageText = binding.messageInput.text.toString()
            if (messageText.isNotBlank()) {
            viewModel.sendMessage(messageText)
                binding.messageInput.text?.clear()
            }
        }
    }

    /**
     * Log de estadísticas de memoria para debugging.
     * Solo se ejecuta en builds de debug para no afectar performance en producción.
     */
    private fun logMemoryStatsIfNeeded() {
        if (com.tecsup.aquanqa.BuildConfig.DEBUG) {
            val stats = viewModel.getMemoryStats()
            if (stats.isNearLimit) {
                android.util.Log.d("ChatbotFragment", 
                    "Memoria del chat cerca del límite: ${stats.totalItems} items " +
                    "(${stats.messageItems} mensajes, ${stats.suggestionItems} sugerencias)")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar la referencia al binding para evitar fugas de memoria.
        binding.rvMessages.adapter = null
        _binding = null
    }
} 