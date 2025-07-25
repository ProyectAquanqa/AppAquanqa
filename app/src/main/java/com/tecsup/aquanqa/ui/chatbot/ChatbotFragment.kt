package com.tecsup.aquanqa.ui.chatbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.databinding.FragmentChatbotBinding

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
        val apiService = ApiClient.apiService
        val repository = ChatbotRepository(apiService)
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
                    }
                }
                is ChatUiState.Error -> {
                    // Opcional: Mostrar un Snackbar o Toast con state.message
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

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar la referencia al binding para evitar fugas de memoria.
        binding.rvMessages.adapter = null
        _binding = null
    }
} 