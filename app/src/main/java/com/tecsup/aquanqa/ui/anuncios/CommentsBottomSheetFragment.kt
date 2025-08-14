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
        private const val MENU_DELETE_ID = 1001

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
            onOptionsClick = { anchor, comentario ->
                showCommentOptions(anchor, comentario)
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
                            // Actualizar el adapter con el ID del usuario (sin foto de perfil)
                            val currentComments = commentsAdapter.getComentarios()
                            commentsAdapter = CommentsAdapter(
                                currentComments,
                                onOptionsClick = { anchor, comentario ->
                                    showCommentOptions(anchor, comentario)
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
        binding.btnSendComment.isEnabled = false
        
        // Usar CommentManager para crear el comentario y actualizar contadores automáticamente
        com.tecsup.aquanqa.utils.CommentManager.createComment(
            context = requireContext(),
            eventoId = anuncio.id,
            content = content,
            lifecycleScope = lifecycleScope
        ) { success, newCount ->
            binding.btnSendComment.isEnabled = true
            
            if (success) {
                // Limpiar campo de texto
                binding.etComment.text?.clear()
                
                // Recargar comentarios para mostrar el nuevo
                loadComments()
                
               
            }
            // Los errores ya se manejan en CommentManager
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

    private fun showCommentOptions(anchor: View, comentario: ComentarioResponse) {
        // Popup limpio usando widget Material Light (fondo blanco y sin contenedor rosado)
        val popup = androidx.appcompat.widget.PopupMenu(
            requireContext(),
            anchor,
            android.view.Gravity.END,
            0,
            androidx.appcompat.R.style.ThemeOverlay_AppCompat_Light
        )

        popup.menu.add(0, MENU_DELETE_ID, 0, "Eliminar")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_DELETE_ID -> {
                    confirmAndDeleteComment(comentario)
                    true
                }
                else -> false
            }
        }

        popup.show()
        
        // Ajustar fondo a gris claro y compactar, sin agregar contenedores extra
        try {
            val popupField = androidx.appcompat.widget.PopupMenu::class.java.getDeclaredField("mPopup")
            popupField.isAccessible = true
            val menuPopupHelper = popupField.get(popup)
            val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
            val getListView = classPopupHelper.getMethod("getListView")
            val listView = getListView.invoke(menuPopupHelper) as? android.widget.ListView
            listView?.apply {
                setBackgroundColor(android.graphics.Color.parseColor("#F2F2F2"))
                //setBackgroundColor(ContextCompat.getColor(context, R.color.text_hint))
                setPadding(12, 8, 12, 8)
                divider = null
                dividerHeight = 0
            }
        } catch (_: Exception) { }
    }

    private fun confirmAndDeleteComment(comentario: ComentarioResponse) {
        // Diálogo más compacto y moderno
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar comentario")
            .setMessage("¿Eliminar este comentario?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                dialog.dismiss()
                deleteComment(comentario)
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deleteComment(comentario: ComentarioResponse) {
        // Usar CommentManager para eliminar el comentario y actualizar contadores automáticamente
        com.tecsup.aquanqa.utils.CommentManager.deleteComment(
            context = requireContext(),
            eventoId = anuncio.id,
            comentarioId = comentario.id,
            lifecycleScope = lifecycleScope
        ) { success, newCount ->
            if (success) {
                // Remover comentario de la lista local
                commentsAdapter.removeComment(comentario.id)
                
                // Si la lista queda vacía, mostrar estado vacío
                if (commentsAdapter.getComentarios().isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.rvComments.visibility = View.GONE
                }
                
                // El contador se actualiza automáticamente via CommentManager
                Toast.makeText(requireContext(), "Comentario eliminado", Toast.LENGTH_SHORT).show()
            }
            // Los errores ya se manejan en CommentManager
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar callbacks para evitar memory leaks
        com.tecsup.aquanqa.utils.CommentManager.clearCallbacks(anuncio.id)
        _binding = null
    }
}
