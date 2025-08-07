package com.tecsup.aquanqa.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tecsup.aquanqa.databinding.ItemLunchDayBinding
import com.tecsup.aquanqa.data.model.content.Almuerzo
import com.tecsup.aquanqa.utils.LinkHelper

/**
 * Adaptador limpio y optimizado para mostrar menús de almuerzo.
 * 
 * @property onAlmuerzoClick Callback opcional para clicks adicionales
 */
class LunchAdapter(
    private val onAlmuerzoClick: ((Almuerzo) -> Unit)? = null
) : ListAdapter<Almuerzo, LunchAdapter.LunchViewHolder>(AlmuerzoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LunchViewHolder {
        val binding = ItemLunchDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LunchViewHolder(binding, onAlmuerzoClick)
    }

    override fun onBindViewHolder(holder: LunchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder simplificado y claro.
     */
    class LunchViewHolder(
        private val binding: ItemLunchDayBinding,
        private val onAlmuerzoClick: ((Almuerzo) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(almuerzo: Almuerzo) {
            with(binding) {
                // Información básica del menú
                bindMenuInfo(almuerzo)
                
                // Estado visual según disponibilidad
                bindAvailabilityState(almuerzo)
                
                // Configurar interacción
                bindClickBehavior(almuerzo)
            }
        }
        
        private fun ItemLunchDayBinding.bindMenuInfo(almuerzo: Almuerzo) {
            tvDayName.text = almuerzo.fechaFormateada
            tvEntrada.text = "Entrada: ${almuerzo.entrada}"
            tvPlatoFondo.text = "Plato de fondo: ${almuerzo.platoFondo}"
            tvBebida.text = "Bebida: ${almuerzo.refresco}"
            
            // Mostrar dieta solo si está disponible
            tvDieta.apply {
                if (almuerzo.hasDietMenu) {
                    text = "*Dieta: ${almuerzo.dieta}"
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }
        }
        
        private fun ItemLunchDayBinding.bindAvailabilityState(almuerzo: Almuerzo) {
            cardMenu.apply {
                alpha = if (almuerzo.isAvailable) 1.0f else 0.6f
                isEnabled = almuerzo.isAvailable
            }
        }
        
        private fun ItemLunchDayBinding.bindClickBehavior(almuerzo: Almuerzo) {
            val clickListener = {
                handleMenuClick(almuerzo)
                onAlmuerzoClick?.invoke(almuerzo)
            }
            
            cardMenu.setOnClickListener { clickListener() }
        }
        
        private fun handleMenuClick(almuerzo: Almuerzo) {
            val context = binding.root.context
            
            when {
                !almuerzo.isAvailable -> {
                    Toast.makeText(context, almuerzo.statusMessage, Toast.LENGTH_SHORT).show()
                }
                almuerzo.hasOrderLink -> {
                    LinkHelper.openUrl(context, almuerzo.link!!) {
                        Toast.makeText(context, "Abriendo enlace...", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {
                    Toast.makeText(
                        context,
                        "Pedidos no disponibles para ${almuerzo.nombreDia}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * DiffUtil optimizado para comparar almuerzos.
     */
    private class AlmuerzoDiffCallback : DiffUtil.ItemCallback<Almuerzo>() {
        override fun areItemsTheSame(oldItem: Almuerzo, newItem: Almuerzo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Almuerzo, newItem: Almuerzo): Boolean {
            return oldItem == newItem
        }
    }
}