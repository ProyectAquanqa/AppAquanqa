package com.tecsup.aquanqa.utils

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tecsup.aquanqa.R

/**
 * Helper para centralizar la lógica de carga y display de imágenes.
 * Elimina duplicación de código Glide y estandariza el comportamiento.
 */
object ImageDisplayHelper {
    
    /**
     * Carga imagen de perfil con transformación circular
     */
    fun loadProfileImage(
        context: Context,
        imageView: ImageView,
        imageUri: Uri?,
        fallbackUrl: String? = null,
        baseUrl: String = ""
    ) {
        val imageToLoad = imageUri ?: fallbackUrl?.takeIf { it.isNotBlank() }?.let { url ->
            if (url.startsWith("http")) {
                Uri.parse(url)
            } else {
                Uri.parse(baseUrl + url)
            }
        }
        
        Glide.with(context)
            .load(imageToLoad ?: R.drawable.ic_profile)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(imageView)
    }
    
    /**
     * Carga imagen de firma con ajuste centrado
     */
    fun loadSignatureImage(
        context: Context,
        imageView: ImageView,
        imageUri: Uri?,
        fallbackUrl: String? = null,
        baseUrl: String = ""
    ) {
        val imageToLoad = imageUri ?: fallbackUrl?.let { url ->
            if (url.startsWith("http")) {
                Uri.parse(url)
            } else {
                Uri.parse(baseUrl + url)
            }
        }
        
        Glide.with(context)
            .load(imageToLoad ?: R.drawable.dotted_border)
            .fitCenter()
            .placeholder(R.drawable.dotted_border)
            .error(R.drawable.dotted_border)
            .into(imageView)
    }
}