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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.data.model.Anuncio
import com.tecsup.aquanqa.databinding.ItemAnuncioBinding
import com.github.chrisbanes.photoview.PhotoView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para el RecyclerView que muestra la lista de anuncios.
 * Se encarga de vincular los datos de cada `Anuncio` con la vista `item_anuncio`.
 */
class AnunciosAdapter(private var anuncios: List<Anuncio>) :
    RecyclerView.Adapter<AnunciosAdapter.AnuncioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnuncioViewHolder {
        val binding = ItemAnuncioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnuncioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnuncioViewHolder, position: Int) {
        holder.bind(anuncios[position])
    }

    override fun getItemCount(): Int = anuncios.size
    
    //Actualiza la lista de anuncios en el adapter y notifica al RecyclerView para que se redibuje.

    fun updateData(newAnuncios: List<Anuncio>) {
        anuncios = newAnuncios
        notifyDataSetChanged()
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

        fun bind(anuncio: Anuncio) {
            // Rellenar datos estáticos
            binding.tvAuthorName.text = anuncio.autor.fullName
            binding.tvPublishDate.text = "Publicado: ${dateFormat.format(anuncio.fecha)}"
            binding.tvAnnouncementTitle.text = anuncio.titulo
            
            // Configurar la descripción expandible
            setupExpandableDescription(anuncio.descripcion)

            // Cargar imágenes con Glide
            loadImages(anuncio)

            // Lógica para los contadores (a implementar cuando la API los devuelva)
            binding.tvLikeCount.text = "123"
            binding.tvCommentCount.text = "45"
            binding.tvShareCount.text = "20"
        }

        /**
         * Configura las imágenes del autor y del anuncio.
         * Implementa click para abrir vista con zoom
         */
        private fun loadImages(anuncio: Anuncio) {
            // Foto del autor
            Glide.with(itemView.context)
                .load(anuncio.autor.fotoPerfil)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.ivAuthorPhoto)

            // Imagen del anuncio (si existe)
            if (anuncio.imagen != null) {
                binding.ivAnnouncementImage.visibility = View.VISIBLE
                
                // Cargar imagen en el ImageView
                Glide.with(itemView.context)
                    .load(anuncio.imagen)
                    .placeholder(R.drawable.logo_aq)
                    .into(binding.ivAnnouncementImage)
                
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
            
            // Cargar la imagen en el PhotoView de pantalla completa
            Glide.with(itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.logo_aq)
                .into(fullScreenPhotoView)
            
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
    }
} 