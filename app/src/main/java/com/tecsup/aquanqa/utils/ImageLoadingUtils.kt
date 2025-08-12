package com.tecsup.aquanqa.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.tecsup.aquanqa.R

/**
 * Utilidad para cargar imágenes con soporte VERDADERO OFFLINE.
 * 
 *  Funcionalidades:
 * - Cache completo de imágenes con Glide
 * - OFFLINE REAL: Carga desde cache cuando no hay internet
 * - Imágenes por defecto solo si no están en cache
 * - Descarga automática cuando hay internet
 * - Optimizado para la app Aquanqa
 */
object ImageLoadingUtils {

    /**
     * Carga imagen de perfil de usuario con manejo inteligente de conectividad.
     * 
     * @param context Contexto de la aplicación
     * @param imageView ImageView donde cargar la imagen
     * @param imageUrl URL de la imagen a cargar
     * @param useCircleCrop Si debe aplicar crop circular
     */
    fun loadProfileImage(
        context: Context,
        imageView: ImageView,
        imageUrl: String?,
        useCircleCrop: Boolean = true
    ) {
        val glideRequest = Glide.with(context)
        val requestOptions = RequestOptions()
            .placeholder(R.drawable.ic_profile) // Mostrar mientras carga
            .error(R.drawable.ic_profile) // Mostrar si falla la carga
            .diskCacheStrategy(DiskCacheStrategy.ALL) //  SIEMPRE usar cache completo
        
        if (useCircleCrop) {
            requestOptions.circleCrop()
        }
        
        if (!imageUrl.isNullOrBlank()) {
            //  VERDADERO OFFLINE: Siempre intentar cargar imagen
            // Glide automáticamente:
            // - Con internet: Descarga y guarda en cache
            // - Sin internet: Carga desde cache si existe, sino muestra error (ic_profile)
            glideRequest
                .load(imageUrl)
                .apply(requestOptions)
                .into(imageView)
        } else {
            // ❌ NO HAY URL: Mostrar ic_profile.png por defecto
            glideRequest
                .load(R.drawable.ic_profile)
                .apply(requestOptions)
                .into(imageView)
        }
    }

    /**
     * Carga imagen de anuncio con manejo inteligente de conectividad.
     * 
     * @param context Contexto de la aplicación
     * @param imageView ImageView donde cargar la imagen
     * @param imageUrl URL de la imagen a cargar
     */
    fun loadAnuncioImage(
        context: Context,
        imageView: ImageView,
        imageUrl: String?
    ) {
        val glideRequest = Glide.with(context)
        val requestOptions = RequestOptions()
            .placeholder(R.drawable.logo_aq) // Logo de Aquanqa mientras carga
            .error(R.drawable.logo_aq) // Logo de Aquanqa si falla la carga
            .diskCacheStrategy(DiskCacheStrategy.ALL) //  SIEMPRE usar cache completo
        
        if (!imageUrl.isNullOrBlank()) {
            //  VERDADERO OFFLINE: Siempre intentar cargar imagen
            // Glide automáticamente:
            // - Con internet: Descarga y guarda en cache
            // - Sin internet: Carga desde cache si existe, sino muestra error (logo_aq)
            glideRequest
                .load(imageUrl)
                .apply(requestOptions)
                .into(imageView)
        } else {
            // ❌ NO HAY URL: Mostrar logo por defecto
            glideRequest
                .load(R.drawable.logo_aq)
                .apply(requestOptions)
                .into(imageView)
        }
    }

    /**
     * Carga imagen genérica con manejo inteligente de conectividad.
     * 
     * @param context Contexto de la aplicación
     * @param imageView ImageView donde cargar la imagen
     * @param imageUrl URL de la imagen a cargar
     * @param placeholderRes Resource ID de la imagen placeholder
     * @param errorRes Resource ID de la imagen de error
     */
    fun loadGenericImage(
        context: Context,
        imageView: ImageView,
        imageUrl: String?,
        placeholderRes: Int = R.drawable.logo_aq,
        errorRes: Int = R.drawable.logo_aq
    ) {
        val glideRequest = Glide.with(context)
        val requestOptions = RequestOptions()
            .placeholder(placeholderRes)
            .error(errorRes)
            .diskCacheStrategy(DiskCacheStrategy.ALL) //  SIEMPRE usar cache completo
        
        if (!imageUrl.isNullOrBlank()) {
            //  VERDADERO OFFLINE: Siempre intentar cargar imagen
            // Glide automáticamente:
            // - Con internet: Descarga y guarda en cache
            // - Sin internet: Carga desde cache si existe, sino muestra error
            glideRequest
                .load(imageUrl)
                .apply(requestOptions)
                .into(imageView)
        } else {
            // ❌ NO HAY URL: Mostrar imagen por defecto
            glideRequest
                .load(errorRes)
                .apply(requestOptions)
                .into(imageView)
        }
    }

    /**
     * Precarga imágenes para uso offline futuro.
     * Glide automáticamente maneja si hay conexión o no.
     * 
     * @param context Contexto de la aplicación
     * @param imageUrls Lista de URLs a precargar
     */
    fun preloadImages(context: Context, imageUrls: List<String>) {
        imageUrls.forEach { imageUrl ->
            if (imageUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .preload()
            }
        }
    }
}
