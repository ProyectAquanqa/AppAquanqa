package com.tecsup.aquanqa.ui.event

import android.app.Activity
import android.graphics.Color
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.repository.AnunciosRepository
import com.tecsup.aquanqa.databinding.FragmentEventDetailBinding
import com.tecsup.aquanqa.ui.base.BaseFragment
import com.tecsup.aquanqa.ui.anuncios.CommentsBottomSheetFragment
import com.tecsup.aquanqa.utils.LikeManager
import com.github.chrisbanes.photoview.PhotoView
import androidx.lifecycle.lifecycleScope

class EventDetailFragment : BaseFragment<FragmentEventDetailBinding>() {

    private val viewModel: EventDetailViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = requireContext().applicationContext
                val repo = AnunciosRepository(
                    com.tecsup.aquanqa.data.preferences.UserPreferences(appContext),
                    com.tecsup.aquanqa.data.cache.CacheManager()
                )
                @Suppress("UNCHECKED_CAST")
                return EventDetailViewModel(repo) as T
            }
        }
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentEventDetailBinding {
        return FragmentEventDetailBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        super.setupUI()
        // Obtener eventoId de argumentos de navegación o, como fallback, del Intent (FCM)
        val args = arguments
        val fromArgsId = args?.getInt("eventoId", 0) ?: 0
        android.util.Log.d("EventDetailFragment", "Args keys: ${args?.keySet()?.joinToString()} - eventoId: $fromArgsId")

        var resolvedEventoId = fromArgsId
        if (resolvedEventoId <= 0) {
            val extras = requireActivity().intent?.extras
            val fromIntentStr = extras?.getString("evento_id")
            val fromIntentInt = extras?.getInt("evento_id", 0) ?: 0
            resolvedEventoId = when {
                !fromIntentStr.isNullOrBlank() -> fromIntentStr.toIntOrNull() ?: 0
                fromIntentInt > 0 -> fromIntentInt
                else -> 0
            }
            android.util.Log.d("EventDetailFragment", "Resolved from Intent extras: $resolvedEventoId (from_notification=${extras?.getBoolean("from_notification", false)})")
        }

        if (resolvedEventoId <= 0) {
            android.util.Log.e("EventDetailFragment", "eventoId inválido o ausente. No se puede cargar el evento.")
            showErrorUi("No se encontró el evento a mostrar")
            return
        }

        android.util.Log.d("EventDetailFragment", "Cargando eventoId=$resolvedEventoId")
        viewModel.loadEvento(resolvedEventoId)
    }

    override fun setupObservers() {
        super.setupObservers()
        viewModel.eventoState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> showLoading()
                is Result.Success -> {
                    val evento = result.data
                    android.util.Log.d("EventDetailFragment", "=== DATOS RECIBIDOS ===")
                    android.util.Log.d("EventDetailFragment", "ID: ${evento.id}")
                    android.util.Log.d("EventDetailFragment", "Título: '${evento.titulo}'")
                    android.util.Log.d("EventDetailFragment", "Descripción (${evento.descripcion?.length ?: 0} chars): '${evento.descripcion}'")
                    android.util.Log.d("EventDetailFragment", "Imagen URL: '${evento.imagen}'")
                    android.util.Log.d("EventDetailFragment", "Autor: ${evento.autor?.fullName ?: "null"}")
                    
                    showContent()
                    
                    // Binding directo al include usando el binding generado
                    val itemBinding = binding.itemAnuncio
                    
                    // Autor
                    val authorName = evento.autor?.fullName ?: "Sistema"
                    itemBinding.tvAuthorName.text = authorName
                    android.util.Log.d("EventDetailFragment", "Autor seteado: '$authorName'")
                    
                    // Título
                    itemBinding.tvAnnouncementTitle.text = evento.titulo ?: "Sin título"
                    android.util.Log.d("EventDetailFragment", "Título seteado: '${evento.titulo ?: "Sin título"}'")
                    
                    // Descripción
                    itemBinding.tvAnnouncementDescription.text = evento.descripcion ?: "Sin descripción"
                    android.util.Log.d("EventDetailFragment", "Descripción seteada: '${evento.descripcion ?: "Sin descripción"}'")
                    
                    // Fecha de publicación
                    val dateLabel = try {
                        val sdf = java.text.SimpleDateFormat("dd 'de' MMMM", java.util.Locale("es", "ES"))
                        "Publicado: ${sdf.format(evento.fecha)}"
                    } catch (e: Exception) {
                        android.util.Log.w("EventDetailFragment", "Error parseando fecha: ${e.message}")
                        "Publicado"
                    }
                    itemBinding.tvPublishDate.text = dateLabel
                    android.util.Log.d("EventDetailFragment", "Fecha seteada: '$dateLabel'")
                    
                    // Foto del autor (circular)
                    com.tecsup.aquanqa.utils.ImageLoadingUtils.loadProfileImage(
                        context = requireContext(),
                        imageView = itemBinding.ivAuthorPhoto,
                        imageUrl = evento.autor?.fotoPerfil,
                        useCircleCrop = true
                    )
                    
                    // Imagen del evento seleccionado
                    if (evento.imagen.isNullOrBlank()) {
                        itemBinding.ivAnnouncementImage.visibility = android.view.View.GONE
                        android.util.Log.d("EventDetailFragment", "Imagen oculta (URL vacía)")
                    } else {
                        itemBinding.ivAnnouncementImage.visibility = android.view.View.VISIBLE
                        com.tecsup.aquanqa.utils.ImageLoadingUtils.loadAnuncioImage(
                            context = requireContext(),
                            imageView = itemBinding.ivAnnouncementImage,
                            imageUrl = evento.imagen
                        )
                        
                        //  Configurar click para abrir pantalla completa con zoom
                        setupFullScreenZoom(itemBinding.ivAnnouncementImage, evento.imagen!!, evento.titulo ?: "Evento")
                        
                        android.util.Log.d("EventDetailFragment", "Imagen cargada desde: '${evento.imagen}'")
                    }
                    
                    // Asegurar visibilidad de elementos críticos
                    itemBinding.llContent.visibility = android.view.View.VISIBLE
                    itemBinding.tvAnnouncementTitle.visibility = android.view.View.VISIBLE
                    itemBinding.tvAnnouncementDescription.visibility = android.view.View.VISIBLE
                    
                    // Configurar botones de acción con LikeManager
                    setupActionButtons(itemBinding, evento)
                    
                    // Compartir
                    itemBinding.btnShare.setOnClickListener {
                        val title = evento.titulo ?: "Evento"
                        val desc = evento.descripcion ?: ""
                        val imageUrl = evento.imagen
                        val author = evento.autor?.fullName

                        val shareText = buildString {
                            appendLine(title)
                            if (author?.isNotBlank() == true) appendLine("Por: $author")
                            if (desc.isNotBlank()) appendLine().append(desc)
                            if (!imageUrl.isNullOrBlank()) appendLine().append("\nImagen: ").append(imageUrl)
                        }.trim()

                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "${title}")
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        }
                        val chooser = android.content.Intent.createChooser(sendIntent, "Compartir evento")
                        startActivity(chooser)
                    }

                    android.util.Log.d("EventDetailFragment", "=== BINDING COMPLETADO ===")
                }
                is Result.Error -> showErrorUi(result.exception.message ?: "Error cargando evento")
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.contentContainer.visibility = android.view.View.GONE
        binding.errorState.root.visibility = android.view.View.GONE
    }

    private fun showContent() {
        binding.progressBar.visibility = android.view.View.GONE
        binding.contentContainer.visibility = android.view.View.VISIBLE
        binding.errorState.root.visibility = android.view.View.GONE
    }

    private fun showErrorUi(error: String) {
        binding.progressBar.visibility = android.view.View.GONE
        binding.contentContainer.visibility = android.view.View.GONE
        binding.errorState.root.visibility = android.view.View.VISIBLE
        binding.errorState.tvErrorMessage.text = error
    }

    /**
     * Configura el click para abrir pantalla completa con zoom
     * Copiado del AnunciosAdapter para mantener consistencia
     */
    private fun setupFullScreenZoom(imageView: android.widget.ImageView, imageUrl: String, title: String) {
        imageView.setOnClickListener {
            openFullScreenZoom(imageUrl, title, 0f, 0f)
        }
    }

    /**
     * Abre un overlay de pantalla completa para hacer zoom 
     * Copiado del AnunciosAdapter para mantener consistencia
     */
    private fun openFullScreenZoom(imageUrl: String, title: String, focusX: Float, focusY: Float) {
        val activity = requireActivity()
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        
        // Crear overlay de pantalla completa
        val overlay = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            alpha = 0f
            isFocusableInTouchMode = true // Permitir que reciba el foco para capturar teclas
            requestFocus()
        }
        
        // Crear PhotoView para pantalla completa
        val fullScreenPhotoView = PhotoView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // Configurar niveles de zoom para pantalla completa
            minimumScale = 0.5f
            mediumScale = 1.0f
            maximumScale = 4.0f
            setZoomTransitionDuration(300)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            setAllowParentInterceptOnEdge(true)
        }
        
        // Cargar la imagen en el PhotoView de pantalla completa con manejo inteligente
        com.tecsup.aquanqa.utils.ImageLoadingUtils.loadGenericImage(
            context = requireContext(),
            imageView = fullScreenPhotoView,
            imageUrl = imageUrl,
            placeholderRes = R.drawable.logo_aq,
            errorRes = R.drawable.logo_aq
        )
        
        // Añadir PhotoView al overlay
        overlay.addView(fullScreenPhotoView)
        
        // IMPORTANTE: Capturar el botón "Atrás" para cerrar el overlay
        overlay.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                closeFullScreenZoom(overlay, rootView)
                true // Consumir el evento para que no salga de la app
            } else {
                false
            }
        }
        
        // Configurar listener para cerrar con un tap cuando esté en escala normal
        fullScreenPhotoView.setOnPhotoTapListener { _, _, _ ->
            if (fullScreenPhotoView.scale <= fullScreenPhotoView.mediumScale) {
                closeFullScreenZoom(overlay, rootView)
            }
        }
        
        // Configurar listener para detectar zoom out completo
        fullScreenPhotoView.setOnScaleChangeListener { scaleFactor, _, _ ->
            if (scaleFactor < 0.8f) {
                closeFullScreenZoom(overlay, rootView)
            }
        }
        
        // Añadir overlay a la pantalla
        rootView.addView(overlay)
        
        // Animar entrada del overlay
        overlay.animate()
            .alpha(1f)
            .setDuration(200)
            .withEndAction {
                // Asegurar que el overlay tenga el foco después de la animación
                overlay.requestFocus()
            }
            .start()
    }

    /**
     * Cierra el overlay de pantalla completa con animación.
     * Copiado del AnunciosAdapter para mantener consistencia
     */
    private fun closeFullScreenZoom(overlay: FrameLayout, rootView: ViewGroup) {
        overlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                rootView.removeView(overlay)
            }
            .start()
    }

    /**
     * Configura los botones de acción (like, comentar) usando LikeManager.
     */
    private fun setupActionButtons(itemBinding: com.tecsup.aquanqa.databinding.ItemAnuncioBinding, evento: com.tecsup.aquanqa.data.model.content.Anuncio) {
        // Configurar botón de like
        LikeManager.setupLikeButton(
            context = requireContext(),
            button = itemBinding.btnLike,
            anuncio = evento,
            lifecycleScope = lifecycleScope
        )
        
        // Configurar botón de comentarios
        LikeManager.setupCommentButton(
            button = itemBinding.btnComment,
            anuncio = evento,
            comentariosCount = evento.comentariosCount,
            onCommentClick = { anuncio ->
                showCommentsModal(anuncio)
            }
        )
    }
    
    /**
     * Muestra el modal de comentarios para un evento específico.
     */
    private fun showCommentsModal(anuncio: com.tecsup.aquanqa.data.model.content.Anuncio) {
        val commentsBottomSheet = CommentsBottomSheetFragment.newInstance(anuncio)
        commentsBottomSheet.show(parentFragmentManager, "CommentsBottomSheet")
    }
}


