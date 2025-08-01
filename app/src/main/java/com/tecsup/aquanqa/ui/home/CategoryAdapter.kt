package com.tecsup.aquanqa.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.data.model.Category

/**
 * Adapter para mostrar categorías en formato horizontal tipo chip.
 * 
 * Características:
 * - Diseño horizontal compacto tipo chip/tag
 * - Selección visual con color aquanqa_green_light
 * - Íconos personalizables por categoría
 * - Actualizaciones eficientes con DiffUtil
 * 
 * @param onCategoryClick Callback ejecutado al seleccionar una categoría
 */
class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var selectedCategoryId: Int? = null

    private val categoryIcons = mapOf(
        "Todos" to R.drawable.ic_home_filled,
        "Anuncios" to R.drawable.ic_announcement_filled,
        "Charlas" to R.drawable.ic_chatbot_modern,
        "Talleres" to R.drawable.ic_benefits_filled,
        "Conferencias" to R.drawable.ic_profile_modern,
        "Noticias" to R.drawable.ic_announcement_outline,
        "Eventos" to R.drawable.ic_badge
    )

    /**
     * ViewHolder para cada elemento de categoría.
     */
    class CategoryViewHolder(
        itemView: View,
        private val onCategoryClick: (Category) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardView = itemView as MaterialCardView
        private val categoryIcon: ImageView = itemView.findViewById(R.id.iv_category_icon)
        private val categoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        private var currentCategory: Category? = null

        init {
            itemView.setOnClickListener {
                currentCategory?.let(onCategoryClick)
            }
        }

        /**
         * Vincula los datos de la categoría con las vistas.
         */
        fun bind(category: Category, isSelected: Boolean, categoryIcons: Map<String, Int>) {
            currentCategory = category
            
            categoryName.text = category.getDisplayName()
            
            val iconResource = categoryIcons[category.nombre] ?: R.drawable.ic_announcement_filled
            categoryIcon.setImageResource(iconResource)
            
            applySelectionStyle(isSelected)
        }

        /**
         * Aplica el estilo visual según el estado de selección.
         * Usa aquanqa_green_light para categorías seleccionadas.
         */
        private fun applySelectionStyle(isSelected: Boolean) {
            val context = itemView.context
            
            if (isSelected) {
                // Estilo seleccionado
                cardView.apply {
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.aquanqa_green))
                    strokeColor = ContextCompat.getColor(context, R.color.aquanqa_green)
                    strokeWidth = 2
                    elevation = 4f
                }
                
                categoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.aquanqa_green))
                categoryName.setTextColor(ContextCompat.getColor(context, R.color.aquanqa_green))
                
            } else {
                // Estilo no seleccionado: fondo blanco
                cardView.apply {
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                    strokeColor = ContextCompat.getColor(context, R.color.stroke_color)
                    strokeWidth = 1
                    elevation = 0f
                }
                
                categoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.aquanqa_green))
                categoryName.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view, onCategoryClick)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)
        val isSelected = category.id == selectedCategoryId
        holder.bind(category, isSelected, categoryIcons)
    }

    /**
     * Actualiza la categoría seleccionada y refresca solo los items necesarios.
     */
    fun setSelectedCategory(categoryId: Int?) {
        val previousSelectedId = selectedCategoryId
        selectedCategoryId = categoryId

        // Refrescar solo los items que cambiaron de estado
        currentList.forEachIndexed { index, category ->
            if (category.id == previousSelectedId || category.id == categoryId) {
                notifyItemChanged(index)
            }
        }
    }

    /**
     * Obtiene la categoría actualmente seleccionada.
     */
    fun getSelectedCategory(): Category? {
        return currentList.find { it.id == selectedCategoryId }
    }

    /**
     * DiffUtil callback para actualizaciones eficientes.
     */
    private class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
} 