package com.tecsup.aquanqa.ui.anuncios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.databinding.FragmentCommentsBottomSheetBinding
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.SessionManager
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.tecsup.aquanqa.data.model.content.Anuncio
import com.tecsup.aquanqa.data.model.content.ComentarioResponse
import com.tecsup.aquanqa.data.model.content.NuevoComentarioRequest
import com.tecsup.aquanqa.data.model.user.UserProfile
import com.tecsup.aquanqa.ui.adapters.CommentsAdapter
import com.tecsup.aquanqa.utils.ImageLoadingUtils
import kotlinx.coroutines.launch

/**
 * Bottom sheet fragment para mostrar y gestionar comentarios de un evento.
 */
class CommentsBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentCommentsBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var anuncio: Anuncio
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var userPreferences: UserPreferences
    private var userProfile: UserProfile? = null

    companion object {
        private const val ARG_ANUNCIO = "arg_anuncio"

        fun newInstance(anuncio: Anuncio): CommentsBottomSheetFragment {
            val fragment = CommentsBottomSheetFragment()
            val args = Bundle()
            args.putSerializable(ARG_ANUNCIO, anuncio)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        anuncio = arguments?.getSerializable(ARG_ANUNCIO) as Anuncio
        userPreferences = UserPreferences(requireContext())
        sessionManager = SessionManager(requireContext(), userPreferences)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupRecyclerView()
        setupCommentInput()
        loadUserProfile()
        loadComments()
    }

    override fun onStart() {
        super.onStart()
        // Configurar el comportamiento del bottom sheet
        val behavior = BottomSheetBehavior.from(requireView().parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.peekHeight = 600
    }



    private fun setupRecyclerView() {
        commentsAdapter = CommentsAdapter(
            emptyList(),
            onOptionsClick = { comentario ->
                showCommentOptions(comentario)
            },
            currentUserId = userProfile?.id
        )

        binding.rvComments.apply {
            adapter = commentsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupCommentInput() {
        // Habilitar/deshabilitar botón de envío según el texto
        binding.etComment.doAfterTextChanged { text ->
            binding.btnSendComment.isEnabled = !text.isNullOrBlank()
        }

        // Configurar botón de envío
        binding.btnSendComment.setOnClickListener {
            val commentText = binding.etComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                sendComment(commentText)
            }
        }
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getValidAccessToken()
                if (token != null) {
                    val response = ApiClient.apiService.getUserProfile("Bearer $token")
                    if (response.isSuccessful) {
                        userProfile = response.body()
                        userProfile?.let { profile ->
                            // Cargar foto de perfil del usuario actual
                            ImageLoadingUtils.loadProfileImage(
                                context = requireContext(),
                                imageView = binding.ivUserPhoto,
                                imageUrl = profile.foto_perfil,
                                useCircleCrop = true
                            )
                            // Actualizar el adapter con el ID del usuario
                            val currentComments = commentsAdapter.getComentarios()
                            commentsAdapter = CommentsAdapter(
                                currentComments,
                                onOptionsClick = { comentario ->
                                    showCommentOptions(comentario)
                                },
                                currentUserId = profile.id
                            )
                            binding.rvComments.adapter = commentsAdapter
                        }
                    }
                }
            } catch (e: Exception) {
                // Error silencioso al cargar perfil
            }
        }
    }

    private fun loadComments() {
        showLoadingState()
        
        lifecycleScope.launch {
            try {
                val token = sessionManager.getValidAccessToken()
                if (token != null) {
                    val response = ApiClient.apiService.getComentarios("Bearer $token", anuncio.id)
                    
                    if (response.isSuccessful) {
                        val comentarios = response.body() ?: emptyList()
                        updateCommentsUI(comentarios)
                    } else {
                        showError("Error al cargar comentarios")
                    }
                } else {
                    showError("Error de autenticación")
                }
            } catch (e: Exception) {
                showError("Error de conexión")
            }
        }
    }

    private fun sendComment(content: String) {
        lifecycleScope.launch {
            try {
                binding.btnSendComment.isEnabled = false
                
                val token = sessionManager.getValidAccessToken()
                if (token != null) {
                    val request = NuevoComentarioRequest(anuncio.id, content)
                    val response = ApiClient.apiService.crearComentario("Bearer $token", request)
                    
                    if (response.isSuccessful) {
                        val nuevoComentario = response.body()
                        if (nuevoComentario != null) {
                            // Agregar comentario a la lista
                            commentsAdapter.addComment(nuevoComentario)
                            

                            // Limpiar campo de texto
                            binding.etComment.text?.clear()
                            
                            // Hacer scroll al nuevo comentario
                            binding.rvComments.smoothScrollToPosition(0)
                            
                            // Ocultar estado vacío si estaba visible
                            binding.emptyState.visibility = View.GONE
                            binding.rvComments.visibility = View.VISIBLE

                        }
                    } else {
                        showError("Error al enviar comentario")
                    }
                } else {
                    showError("Error de autenticación")
                }
            } catch (e: Exception) {
                showError("Error de conexión")
            } finally {
                binding.btnSendComment.isEnabled = true
            }
        }
    }

    private fun updateCommentsUI(comentarios: List<ComentarioResponse>) {
        if (comentarios.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvComments.visibility = View.GONE
            binding.loadingState.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvComments.visibility = View.VISIBLE
            binding.loadingState.visibility = View.GONE
            commentsAdapter.updateComments(comentarios)
        }


    }

    private fun showLoadingState() {
        binding.loadingState.visibility = View.VISIBLE
        binding.rvComments.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.loadingState.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        
        // Mostrar estado vacío en caso de error
        binding.emptyState.visibility = View.VISIBLE
        binding.rvComments.visibility = View.GONE
    }

    private fun showCommentOptions(comentario: ComentarioResponse) {
        // TODO: Implementar menú de opciones (editar/eliminar comentario)
        Toast.makeText(requireContext(), "Opciones para comentario #${comentario.id}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
