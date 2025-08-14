package com.tecsup.aquanqa.ui.anuncios

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.KeyEvent
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.SessionManager
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.tecsup.aquanqa.data.model.content.NuevoComentarioRequest
import com.tecsup.aquanqa.utils.DateUtils
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.data.model.content.Anuncio
import com.tecsup.aquanqa.databinding.ItemAnuncioBinding
import com.tecsup.aquanqa.utils.ImageLoadingUtils
import com.github.chrisbanes.photoview.PhotoView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para el RecyclerView que muestra la lista de anuncios.
 * Se encarga de vincular los datos de cada `Anuncio` con la vista `item_anuncio`.
 */
class AnunciosAdapter(
    private var anuncios: List<Anuncio>,
    private val lifecycleScope: LifecycleCoroutineScope? = null,
    private val onCommentClick: ((Anuncio) -> Unit)? = null
) : RecyclerView.Adapter<AnunciosAdapter.AnuncioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnuncioViewHolder {
        val binding = ItemAnuncioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnuncioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnuncioViewHolder, position: Int) {
        holder.bind(anuncios[position], lifecycleScope, onCommentClick)
    }

    override fun getItemCount(): Int = anuncios.size
    
    /**
     * Actualiza la lista completa de anuncios en el adapter.
     * Usado para refresh completo o primera carga.
     */
    fun updateData(newAnuncios: List<Anuncio>) {
        val oldSize = anuncios.size
        anuncios = newAnuncios
        
        // Usar notificaciones más específicas para mejor performance
        if (oldSize == 0) {
            notifyItemRangeInserted(0, newAnuncios.size)
        } else {
            notifyDataSetChanged() // Para refresh completo
        }
    }
    
    /**
     * Agrega nuevos anuncios al final de la lista.
     * Usado para infinite scroll / lazy loading.
     */
    fun addMoreData(moreAnuncios: List<Anuncio>) {
        if (moreAnuncios.isEmpty()) return
        
        val startPosition = anuncios.size
        anuncios = anuncios + moreAnuncios // Crear nueva lista inmutable
        
        // Notificar solo los elementos nuevos agregados
        notifyItemRangeInserted(startPosition, moreAnuncios.size)
    }
    
    /**
     * Limpia todos los datos del adapter.
     * Usado antes de recargar datos completamente.
     */
    fun clearData() {
        val oldSize = anuncios.size
        anuncios = emptyList()
        notifyItemRangeRemoved(0, oldSize)
    }

    /**
     * ViewHolder para un solo item de anuncio.
     *
     * Maneja la lógica de presentación de los datos del anuncio, incluyendo
     * una funcionalidad avanzada para expandir y contraer el texto de la descripción de forma inline
     */
    class AnuncioViewHolder(private val binding: ItemAnuncioBinding) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd 'de' MMMM", Locale("es", "ES"))
        private var isExpanded = false
        private val collapsedMaxLines = 7

        fun bind(anuncio: Anuncio, lifecycleScope: LifecycleCoroutineScope?, onCommentClick: ((Anuncio) -> Unit)?) {
            // Rellenar datos estáticos
            binding.tvAuthorName.text = anuncio.autor.fullName
            binding.tvPublishDate.text = "Publicado: ${dateFormat.format(anuncio.fecha)}"
            binding.tvAnnouncementTitle.text = anuncio.titulo
            
            // Mostrar/ocultar ícono de pinned basado en isPinned
            binding.pinned.visibility = if (anuncio.isPinned) View.VISIBLE else View.GONE
            
            // Configurar la descripción expandible
            setupExpandableDescription(anuncio.descripcion)

            // Cargar imágenes con Glide
            loadImages(anuncio)
            
            // Configurar botones de acción
            setupActionButtons(anuncio, lifecycleScope, onCommentClick)

            // Compartir
            binding.btnShare.setOnClickListener {
                val title = anuncio.titulo
                val desc = anuncio.descripcion
                val imageUrl = anuncio.imagen
                val author = anuncio.autor.fullName

                val shareText = buildString {
                    appendLine(title)
                    if (author.isNotBlank()) appendLine("Por: $author")
                    if (desc.isNotBlank()) appendLine().append(desc)
                    if (!imageUrl.isNullOrBlank()) appendLine().append("\nImagen: ").append(imageUrl)
                }.trim()

                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, title)
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                }
                val chooser = android.content.Intent.createChooser(sendIntent, "Compartir evento")
                itemView.context.startActivity(chooser)
            }

        }

        /**
         *  Configura las imágenes del autor y del anuncio con manejo inteligente de conectividad.
         * Implementa click para abrir vista con zoom y usa imágenes por defecto cuando no hay internet.
         */
        private fun loadImages(anuncio: Anuncio) {
            //  Foto del autor con manejo inteligente de conectividad
            ImageLoadingUtils.loadProfileImage(
                context = itemView.context,
                imageView = binding.ivAuthorPhoto,
                imageUrl = anuncio.autor.fotoPerfil,
                useCircleCrop = true
            )

            //  Imagen del anuncio con manejo inteligente de conectividad
            if (anuncio.imagen != null) {
                binding.ivAnnouncementImage.visibility = View.VISIBLE
                
                // Cargar imagen en el ImageView usando utilidad inteligente
                ImageLoadingUtils.loadAnuncioImage(
                    context = itemView.context,
                    imageView = binding.ivAnnouncementImage,
                    imageUrl = anuncio.imagen
                )
                
                // Configurar click para abrir pantalla completa 
                setupFullScreenZoom(anuncio.imagen, anuncio.titulo)
            } else {
                binding.ivAnnouncementImage.visibility = View.GONE
            }
        }

        /**
         * Configura el click para abrir pantalla completa 
         * El usuario hace click en la imagen para expandirla a toda la pantalla.
         */
        private fun setupFullScreenZoom(imageUrl: String, title: String) {
            // Configurar click listener para abrir overlay de pantalla completa
            binding.ivAnnouncementImage.setOnClickListener {
                openFullScreenZoom(imageUrl, title, 0f, 0f)
            }
        }

        /**
         * Abre un overlay de pantalla completa para hacer zoom 
         */
        private fun openFullScreenZoom(imageUrl: String, title: String, focusX: Float, focusY: Float) {
            val activity = itemView.context as? Activity ?: return
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            
            // Crear overlay de pantalla completa
            val overlay = FrameLayout(itemView.context).apply {
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
            val fullScreenPhotoView = PhotoView(itemView.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                
                // Configurar niveles de zoom para pantalla completa
                minimumScale = 0.5f
                mediumScale = 1.0f
                maximumScale = 4.0f
                setZoomTransitionDuration(300)
                setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER)
                setAllowParentInterceptOnEdge(true)
            }
            
            //  Cargar la imagen en el PhotoView de pantalla completa con manejo inteligente
            ImageLoadingUtils.loadGenericImage(
                context = itemView.context,
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
         * Prepara el TextView de la descripción para la funcionalidad de
         * "Ver más" / "Ver menos".
         */
        private fun setupExpandableDescription(description: String) {
            isExpanded = false
            binding.tvAnnouncementDescription.movementMethod = LinkMovementMethod.getInstance()

            binding.tvAnnouncementDescription.post {
                updateDescriptionText(description)
            }
        }

        /**
         * Orquesta la actualización del texto de la descripción, mostrando la
         * versión completa o truncada según el estado `isExpanded`.
         */
        private fun updateDescriptionText(description: String) {
            if (isExpanded) {
                setExpandedText(description)
            } else {
                setCollapsedText(description)
            }
        }

        /**
         * Muestra el texto completo junto con un "Ver menos" clickeable.
         */
        private fun setExpandedText(description: String) {
            val seeLessText = " Ver menos"
            val fullText = description + seeLessText
            
            val spannable = createClickableSpannable(fullText, seeLessText) {
                isExpanded = false
                updateDescriptionText(description)
            }
            binding.tvAnnouncementDescription.text = spannable
        }
        
        /**
         * Muestra el texto truncado si excede el máximo de líneas, añadiendo
         * un "... Ver más" clickeable.
         */
        private fun setCollapsedText(description: String) {
            binding.tvAnnouncementDescription.maxLines = collapsedMaxLines
            binding.tvAnnouncementDescription.text = description

            binding.tvAnnouncementDescription.post {
                val layout = binding.tvAnnouncementDescription.layout
                if (layout == null || layout.lineCount <= collapsedMaxLines) {
                    // El texto no necesita ser truncado, se muestra completo.
                    binding.tvAnnouncementDescription.text = description
                    return@post
                }
                
                // El texto necesita ser truncado
                val seeMoreText = "... Ver más"
                val truncatedText = findTruncatedText(description, seeMoreText)
                val fullText = truncatedText + seeMoreText.substring(3) // Excluye "..." que ya está en el texto

                val spannable = createClickableSpannable(fullText, seeMoreText.substring(4)) {
                    isExpanded = true
                    updateDescriptionText(description)
                }
                
                binding.tvAnnouncementDescription.maxLines = Integer.MAX_VALUE
                binding.tvAnnouncementDescription.text = spannable
            }
        }
        
        /**
         * Calcula el punto exacto de truncamiento para que "... Ver más"
         * quepa en la última línea.
         * @return El texto original cortado en el punto correcto.
         */
        private fun findTruncatedText(description: String, seeMoreText: String): String {
            val layout = binding.tvAnnouncementDescription.layout
            val lastLineEnd = layout.getLineEnd(collapsedMaxLines - 1)
            var cutIndex = lastLineEnd
            
            val textPaint = binding.tvAnnouncementDescription.paint
            
            // Bucle para encontrar el punto de corte exacto
            while(cutIndex > 0) {
                val measuredWidth = textPaint.measureText(description, 0, cutIndex) + textPaint.measureText(seeMoreText)
                if (measuredWidth < (layout.width * collapsedMaxLines)) {
                    // Encontramos un punto donde cabe, pero puede ser muy pronto.
                    // Ajustamos para que esté lo más cerca posible del final.
                    val testText = description.substring(0, cutIndex) + seeMoreText
                    val tempLayout = android.text.StaticLayout.Builder.obtain(
                        testText, 0, testText.length, textPaint, layout.width
                    ).build()
                    
                    if (tempLayout.lineCount <= collapsedMaxLines) {
                        break // Encontramos el punto de corte ideal
                    }
                }
                cutIndex--
            }
            
            return description.substring(0, cutIndex).trim()
        }

        /**
         * Función de utilidad para crear un SpannableString con una porción de texto clickeable.
         *
         * @param fullText El texto completo que se mostrará.
         * @param clickableText La parte del texto que será clickeable.
         * @param onClick La acción a ejecutar cuando se haga click.
         * @return Un `SpannableString` con el estilo y la acción aplicados.
         */
        private fun createClickableSpannable(fullText: String, clickableText: String, onClick: () -> Unit): SpannableString {
            val spannable = SpannableString(fullText)
            val startIndex = fullText.lastIndexOf(clickableText)
            val endIndex = startIndex + clickableText.length

            if (startIndex == -1) return spannable

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onClick()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false // Quitar subrayado
                }
            }

            // Aplicar el span clickeable
            spannable.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            
            // Aplicar el color
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(itemView.context, R.color.text_secondary)),
                startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            
            // Aplicar negrita
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            return spannable
        }
        
        /**
         * Configura los botones de acción (like, comentar, compartir) usando LikeManager.
         */
        private fun setupActionButtons(anuncio: Anuncio, lifecycleScope: LifecycleCoroutineScope?, onCommentClick: ((Anuncio) -> Unit)?) {
            // Configurar botón de like usando LikeManager
            if (lifecycleScope != null) {
                com.tecsup.aquanqa.utils.LikeManager.setupLikeButton(
                    context = itemView.context,
                    button = binding.btnLike,
                    anuncio = anuncio,
                    lifecycleScope = lifecycleScope
                )
            }
            
            // Configurar botón de comentar usando LikeManager
            com.tecsup.aquanqa.utils.LikeManager.setupCommentButton(
                button = binding.btnComment,
                anuncio = anuncio,
                comentariosCount = anuncio.comentariosCount,
                onCommentClick = { evento ->
                    onCommentClick?.invoke(evento)
                }
            )
        }
    }
} 