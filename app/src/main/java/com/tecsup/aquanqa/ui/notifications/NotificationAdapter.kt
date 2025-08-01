package com.tecsup.aquanqa.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.data.model.Notification
import com.tecsup.aquanqa.data.model.NotificationItem

import de.hdodenhof.circleimageview.CircleImageView

/**
 * Adapter para el RecyclerView de notificaciones.
 * Maneja múltiples tipos de vista: headers de fecha y items de notificación.
 */
class NotificationAdapter(
    private val onNotificationClick: (Notification) -> Unit
) : ListAdapter<NotificationItem, RecyclerView.ViewHolder>(NotificationDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_NOTIFICATION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NotificationItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is NotificationItem.NotificationData -> VIEW_TYPE_NOTIFICATION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        android.util.Log.d("NotificationAdapter", "Creando ViewHolder para tipo: $viewType")
        
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val view = inflater.inflate(R.layout.item_notification_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            VIEW_TYPE_NOTIFICATION -> {
                val view = inflater.inflate(R.layout.item_notification, parent, false)
                NotificationViewHolder(view, onNotificationClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        android.util.Log.d("NotificationAdapter", "Binding item en posición $position: $item")
        when (item) {
            is NotificationItem.DateHeader -> {
                (holder as DateHeaderViewHolder).bind(item.date)
            }
            is NotificationItem.NotificationData -> {
                (holder as NotificationViewHolder).bind(item.notification)
            }
        }
    }

    /**
     * ViewHolder para los headers de fecha.
     */
    class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDateHeader: TextView = itemView.findViewById(R.id.tv_date_header)

        fun bind(date: String) {
            tvDateHeader.text = date
        }
    }

    /**
     * ViewHolder para los items de notificación.
     */
    class NotificationViewHolder(
        itemView: View,
        private val onNotificationClick: (Notification) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val ivAuthorImage: CircleImageView = itemView.findViewById(R.id.iv_author_image)
        private val tvNotificationTitle: TextView = itemView.findViewById(R.id.tv_notification_title)
        private val tvNotificationMessage: TextView = itemView.findViewById(R.id.tv_notification_message)
        private val tvNotificationTime: TextView = itemView.findViewById(R.id.tv_notification_time)
        private val unreadIndicator: View = itemView.findViewById(R.id.unread_indicator)

        fun bind(notification: Notification) {
            // Configurar título
            tvNotificationTitle.text = notification.title
            
            // Configurar mensaje/descripción
            tvNotificationMessage.text = notification.message
            
            // Configurar timestamp
            tvNotificationTime.text = formatNotificationTime(notification.timestampMillis)
            
            // Configurar imagen del autor o icono de notificación
            loadAuthorImage(notification.authorImageUrl)
            
            // Configurar indicador de no leída
            unreadIndicator.visibility = if (!notification.isRead) View.VISIBLE else View.GONE
            
            // Configurar click listener
            itemView.setOnClickListener {
                onNotificationClick(notification)
            }
        }

        /**
         * Cargael icono de notificación .
         * Aplica transformación circular para mantener consistencia visual.
         */
        private fun loadAuthorImage(imageUrl: String?) {
            val validImageUrl = when {
                imageUrl.isNullOrBlank() -> null
                imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> imageUrl
                imageUrl.startsWith("/") -> null // URLs relativas inválidas, usar icono por defecto
                else -> null
            }
            
            Glide.with(itemView.context)
                .load(validImageUrl)
                .placeholder(R.drawable.ic_alert)
                .error(R.drawable.ic_alert)
                .circleCrop()
                .into(ivAuthorImage)
        }

        /**
         * Formatea el timestamp de la notificación en español con lógica mejorada.
         */
        private fun formatNotificationTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val minutes = (diff / (1000 * 60)).toInt()
            val hours = (diff / (1000 * 60 * 60)).toInt()
            val days = (diff / (1000 * 60 * 60 * 24)).toInt()

            return when {
                minutes < 1 -> "hace 1 minuto"
                minutes < 60 -> "hace $minutes minutos"
                hours == 1 -> "hace 1 hora"
                hours < 24 -> "hace $hours horas"
                days == 1 -> "ayer"
                days < 7 -> "hace $days días"
                else -> {
                    val calendar = java.util.Calendar.getInstance()
                    calendar.timeInMillis = timestamp
                    val dateFormat = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale("es", "ES"))
                    dateFormat.format(calendar.time)
                }
            }
        }
    }

    /**
     * DiffUtil.ItemCallback para optimizar las actualizaciones del RecyclerView.
     */
    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        
        override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return when {
                oldItem is NotificationItem.DateHeader && newItem is NotificationItem.DateHeader -> {
                    oldItem.date == newItem.date
                }
                oldItem is NotificationItem.NotificationData && newItem is NotificationItem.NotificationData -> {
                    oldItem.notification.id == newItem.notification.id
                }
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return when {
                oldItem is NotificationItem.DateHeader && newItem is NotificationItem.DateHeader -> {
                    oldItem == newItem
                }
                oldItem is NotificationItem.NotificationData && newItem is NotificationItem.NotificationData -> {
                    oldItem.notification == newItem.notification
                }
                else -> false
            }
        }
    }
}