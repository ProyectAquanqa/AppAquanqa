package com.tecsup.aquanqa.ui.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tecsup.aquanqa.databinding.ItemLunchDayBinding
import com.tecsup.aquanqa.data.model.content.Almuerzo

/**
 * Adapter para mostrar los menús de almuerzos del comedor de Tecsup.
 * 
 * Implementa ListAdapter con DiffUtil para optimizar el rendimiento cuando
 * los datos cambian. Maneja el click en las cards para abrir los links
 * de pedidos en el navegador web.
 * 
 * @property onAlmuerzoClick Callback opcional para manejar clicks adicionales en los elementos
 */
class LunchAdapter(
    private val onAlmuerzoClick: ((Almuerzo) -> Unit)? = null
) : ListAdapter<Almuerzo, LunchAdapter.LunchViewHolder>(AlmuerzoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LunchViewHolder {
        val binding = ItemLunchDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LunchViewHolder(binding, onAlmuerzoClick)
    }

    override fun onBindViewHolder(holder: LunchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder para cada elemento de almuerzo en la lista.
     * 
     * Maneja la visualización de los datos y la lógica de click para abrir
     * links de pedidos en el navegador web del dispositivo.
     */
    class LunchViewHolder(
        private val binding: ItemLunchDayBinding,
        private val onAlmuerzoClick: ((Almuerzo) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        
        /**
         * Vincula los datos del almuerzo con las vistas del layout.
         * 
         * Configura toda la información visible del menú del día y establece
         * el listener de click para abrir el link de pedidos si está disponible.
         * 
         * @param almuerzo Datos del almuerzo a mostrar
         */
        fun bind(almuerzo: Almuerzo) {
            binding.apply {
                // Mostrar información del menú
                tvDayName.text = almuerzo.nombreDia
                tvEntrada.text = "Entrada: ${almuerzo.entrada}"
                tvPlatoFondo.text = "Plato de fondo: ${almuerzo.platoFondo}"
                tvBebida.text = "Bebida: ${almuerzo.refresco}"
                
                // Configurar click listener para abrir link en navegador
                val clickListener = {
                    if (!almuerzo.link.isNullOrBlank()) {
                        // Log para debugging - se puede remover en producción
                        println("LunchAdapter: Click detectado. Link: ${almuerzo.link}")
                        openLinkInBrowser(almuerzo.link)
                    } else {
                        // Mostrar mensaje si no hay link disponible
                        Toast.makeText(
                            root.context,
                            "Link de pedido no disponible para ${almuerzo.nombreDia}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    
                    // Ejecutar callback adicional si existe
                    onAlmuerzoClick?.invoke(almuerzo)
                }
                
                // Configurar click en el CardView
                cardMenu.setOnClickListener { clickListener() }
                
                // También configurar en el root como respaldo
                root.setOnClickListener { clickListener() }
                
                // Hacer el CardView visualmente clickeable
                cardMenu.isClickable = true
                cardMenu.isFocusable = true
            }
        }

        /**
         * Abre un link en el navegador web predeterminado del dispositivo.
         * 
         * Implementa múltiples estrategias para abrir URLs de manera robusta:
         * 1. Intento directo con la URL formateada
         * 2. Uso de Intent.createChooser como respaldo
         * 3. Manejo de errores con mensajes informativos
         * 
         * @param link URL a abrir en el navegador
         */
        private fun openLinkInBrowser(link: String) {
            val context = binding.root.context
            
            try {
                // Limpiar y formatear la URL
                val formattedUrl = formatUrl(link.trim())
                println("LunchAdapter: URL formateada: $formattedUrl")
                
                // Crear intent para abrir en navegador
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(formattedUrl)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                
                // Verificar si hay aplicaciones disponibles para manejar la URL
                val packageManager = context.packageManager
                val activities = packageManager.queryIntentActivities(intent, 0)
                
                if (activities.isNotEmpty()) {
                    // Hay aplicaciones disponibles, abrir directamente
                    context.startActivity(intent)
                    Toast.makeText(context, "Abriendo enlace...", Toast.LENGTH_SHORT).show()
                } else {
                    // No hay aplicaciones, usar chooser
                    val chooser = Intent.createChooser(intent, "Selecciona una aplicación para abrir el enlace")
                    chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(chooser)
                }
                
            } catch (e: Exception) {
                println("LunchAdapter: Error al abrir link: ${e.message}")
                
                // Intentar método alternativo
                try {
                    val formattedUrl = formatUrl(link.trim())
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
                    
                    // Forzar que se abra en el navegador predeterminado
                    browserIntent.addCategory(Intent.CATEGORY_BROWSABLE)
                    browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    
                    context.startActivity(browserIntent)
                    
                } catch (e2: Exception) {
                    println("LunchAdapter: Error en método alternativo: ${e2.message}")
                    
                    // Último recurso: mostrar la URL al usuario
                    Toast.makeText(
                        context,
                        "No se pudo abrir automáticamente. URL: $link",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        
        /**
         * Formatea una URL para asegurar que tenga el protocolo correcto.
         * 
         * Maneja diferentes formatos de URL y asegura compatibilidad:
         * - URLs que ya tienen protocolo se mantienen igual
         * - URLs sin protocolo reciben https:// por defecto
         * - Se valida que la URL no esté vacía o sea inválida
         * 
         * @param url URL original que puede o no tener protocolo
         * @return URL formateada con protocolo válido
         */
        private fun formatUrl(url: String): String {
            val cleanUrl = url.trim()
            
            return when {
                cleanUrl.isEmpty() -> throw IllegalArgumentException("URL vacía")
                cleanUrl.startsWith("http://", ignoreCase = true) -> cleanUrl
                cleanUrl.startsWith("https://", ignoreCase = true) -> cleanUrl
                cleanUrl.startsWith("www.", ignoreCase = true) -> "https://$cleanUrl"
                cleanUrl.contains(".") -> "https://$cleanUrl"
                else -> throw IllegalArgumentException("URL inválida: $cleanUrl")
            }
        }
    }

    /**
     * Implementa DiffUtil para optimizar las actualizaciones de la lista.
     * 
     * Compara elementos antiguos y nuevos para determinar qué cambios
     * son necesarios, mejorando significativamente el rendimiento.
     */
    private class AlmuerzoDiffCallback : DiffUtil.ItemCallback<Almuerzo>() {
        
        /**
         * Determina si dos elementos representan el mismo almuerzo.
         * 
         * @param oldItem Elemento anterior
         * @param newItem Elemento nuevo
         * @return true si representan el mismo almuerzo (mismo ID)
         */
        override fun areItemsTheSame(oldItem: Almuerzo, newItem: Almuerzo): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Determina si el contenido de dos elementos es exactamente igual.
         * 
         * @param oldItem Elemento anterior
         * @param newItem Elemento nuevo
         * @return true si todo el contenido es idéntico
         */
        override fun areContentsTheSame(oldItem: Almuerzo, newItem: Almuerzo): Boolean {
            return oldItem == newItem
        }
    }
}