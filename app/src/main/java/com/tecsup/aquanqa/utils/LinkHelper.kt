package com.tecsup.aquanqa.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Utilidad para manejo de enlaces web de manera simplificada y robusta.
 */
object LinkHelper {
    
    /**
     * Abre un enlace en el navegador con manejo de errores user-friendly.
     * 
     * @param context Contexto para mostrar mensajes y abrir aplicaciones
     * @param url URL a abrir
     * @param onSuccess Callback ejecutado cuando se abre exitosamente
     */
    fun openUrl(context: Context, url: String, onSuccess: (() -> Unit)? = null) {
        try {
            val formattedUrl = formatUrl(url.trim())
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                onSuccess?.invoke()
            } else {
                showUserFriendlyError(context, "No se encontró una aplicación para abrir el enlace")
            }
        } catch (e: Exception) {
            showUserFriendlyError(context, "No se pudo abrir el enlace. Verifique su conexión.")
        }
    }
    
    /**
     * Formatea una URL para asegurar protocolo correcto.
     */
    private fun formatUrl(url: String): String {
        return when {
            url.isEmpty() -> throw IllegalArgumentException("URL vacía")
            url.startsWith("http://", ignoreCase = true) -> url
            url.startsWith("https://", ignoreCase = true) -> url
            url.startsWith("www.", ignoreCase = true) -> "https://$url"
            url.contains(".") -> "https://$url"
            else -> throw IllegalArgumentException("URL inválida")
        }
    }
    
    /**
     * Muestra mensajes de error comprensibles para el usuario.
     */
    private fun showUserFriendlyError(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}