package com.tecsup.aquanqa.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.databinding.ItemPublicationBinding
import de.hdodenhof.circleimageview.CircleImageView

class PublicationAdapter(private val publications: List<Publication>) :
    RecyclerView.Adapter<PublicationAdapter.PublicationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicationViewHolder {
        val binding = ItemPublicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PublicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PublicationViewHolder, position: Int) {
        holder.bind(publications[position])
    }

    override fun getItemCount() = publications.size

    class PublicationViewHolder(private val binding: ItemPublicationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(publication: Publication) {
            // Configurar el encabezado
            binding.publicationCategory.text = publication.title
            binding.publicationBadge.setImageResource(publication.iconResId)
            
            // Configurar el título principal
            binding.publicationTitle.text = "¡Buen trabajo equipo!"
            
            // Configurar información del autor
            binding.authorName.text = publication.author.name
            binding.publicationDate.text = publication.date
            binding.publicationContent.text = publication.content

            // Cargar imagen del autor con Glide
            Glide.with(itemView.context)
                .load(publication.author.imageUrl)
                .error(R.drawable.ic_profile)
                .into(binding.authorImage)


            // Configurar botones de acción
            binding.likeButton.setOnClickListener {
                // Implementar acción de "Me gusta"
            }

            binding.commentButton.setOnClickListener {
                // Implementar acción de "Comentar"
            }
        }


    }
} 