package com.tecsup.aquanqa.ui.chatbot

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecsup.aquanqa.databinding.FragmentChatbotBinding

class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatbotViewModel
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(requireActivity())[ChatbotViewModel::class.java]
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupMessageInput()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter { question ->
            viewModel.sendMessage(question)
        }
        binding.messagesRecyclerView.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupMessageInput() {
        binding.sendButton.setOnClickListener { sendMessageAndClearInput() }
        binding.messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessageAndClearInput()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun observeViewModel() {
        viewModel.chatItems.observe(viewLifecycleOwner) { items ->
            messageAdapter.submitList(items) {
                // Desplazarse al final de la lista después de la actualización.
                if (items.isNotEmpty()) {
                    binding.messagesRecyclerView.scrollToPosition(items.size - 1)
                }
            }
        }
    }

    private fun sendMessageAndClearInput() {
        val messageText = binding.messageInput.text.toString().trim()
        if (messageText.isNotEmpty()) {
            viewModel.sendMessage(messageText)
            binding.messageInput.text.clear()
            hideKeyboard()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 