package com.tecsup.aquanqa.ui.anuncios

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.data.model.Anuncio
import com.tecsup.aquanqa.databinding.ItemAnuncioBinding
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
    
    /**
     * Actualiza la lista de anuncios en el adapter y notifica al RecyclerView
     * para que se redibuje.
     */
    fun updateData(newAnuncios: List<Anuncio>) {
        anuncios = newAnuncios
        notifyDataSetChanged()
    }

    /**
     * ViewHolder para un solo item de anuncio.
     * Contiene la lógica para rellenar la vista con los datos del anuncio.
     */
    class AnuncioViewHolder(private val binding: ItemAnuncioBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("dd 'de' MMMM", Locale("es", "ES"))

        fun bind(anuncio: Anuncio) {
            // Rellenar datos de texto
            binding.tvAuthorName.text = anuncio.autor.fullName
            binding.tvPublishDate.text = "Publicado: ${dateFormat.format(anuncio.fecha)}"
            binding.tvAnnouncementTitle.text = anuncio.titulo
            binding.tvAnnouncementDescription.text = anuncio.descripcion

            // Cargar foto del autor con Glide
            Glide.with(itemView.context)
                .load(anuncio.autor.fotoPerfil)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile) // En caso de que la URL sea nula o falle
                .circleCrop()
                .into(binding.ivAuthorPhoto)

            // Cargar imagen del anuncio (si existe)
            if (anuncio.imagen != null) {
                binding.ivAnnouncementImage.visibility = ViewGroup.VISIBLE
                Glide.with(itemView.context)
                    .load(anuncio.imagen)
                    .placeholder(R.drawable.logo_aq) // Un placeholder genérico
                    .into(binding.ivAnnouncementImage)
            } else {
                binding.ivAnnouncementImage.visibility = ViewGroup.GONE
            }
            
            // Lógica para los contadores (a implementar cuando la API los devuelva)
            binding.tvLikeCount.text = "123"
            binding.tvCommentCount.text = "45"
            binding.tvShareCount.text = "20"
        }
    }
} 