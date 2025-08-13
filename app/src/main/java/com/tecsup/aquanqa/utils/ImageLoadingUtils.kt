package com.tecsup.aquanqa.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DecodeFormat
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
     * Carga imagen de perfil de usuario con manejo inteligente de conectividad y optimización de rendimiento.
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
            .priority(Priority.HIGH) // Prioridad alta para imágenes de perfil
            .format(DecodeFormat.PREFER_RGB_565) // Menos memoria para mejor rendimiento
            .override(120, 120) // Redimensionar para perfiles (120dp típico)
            .skipMemoryCache(false) // Permitir cache de memoria
        
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
     * Carga imagen de anuncio con manejo inteligente de conectividad y optimización de rendimiento.
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
            .priority(Priority.NORMAL) // Prioridad normal para imágenes de anuncios
            .format(DecodeFormat.PREFER_RGB_565) // Menos memoria para mejor rendimiento
            .centerCrop() // Crop centrado para mejor presentación
            .skipMemoryCache(false) // Permitir cache de memoria
            .override(800, 600) // Tamaño máximo optimizado (4:3 ratio)
        
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
     * Carga imagen genérica con manejo inteligente de conectividad y optimización de rendimiento.
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
            .priority(Priority.NORMAL)
            .format(DecodeFormat.PREFER_RGB_565) // Menos memoria para mejor rendimiento
            .skipMemoryCache(false) // Permitir cache de memoria
        
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
     * Precarga imágenes de manera inteligente para uso offline futuro.
     * Optimizado para rendimiento y gestión de memoria.
     * 
     * @param context Contexto de la aplicación
     * @param imageUrls Lista de URLs a precargar
     */
    fun preloadImages(context: Context, imageUrls: List<String>) {
        imageUrls.take(10) // Limitar a 10 imágenes para evitar sobrecarga de memoria
            .filter { it.isNotEmpty() }
            .forEach { imageUrl ->
                Glide.with(context)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .priority(Priority.LOW) // Baja prioridad para precarga
                    .format(DecodeFormat.PREFER_RGB_565)
                    .override(400, 300) // Precargar en tamaño reducido
                    .preload()
            }
    }
    
    /**
     * Limpia la caché de memoria de Glide para liberar espacio.
     * Útil cuando se detecta baja memoria.
     * 
     * @param context Contexto de la aplicación
     */
    fun clearMemoryCache(context: Context) {
        Glide.get(context).clearMemory()
    }
    
    /**
     * Limpia toda la caché (memoria + disco) de Glide.
     * Usar solo en casos extremos o por configuración del usuario.
     * 
     * @param context Contexto de la aplicación
     */
    fun clearAllCache(context: Context) {
        // Limpiar caché de memoria (hilo principal)
        Glide.get(context).clearMemory()
        
        // Limpiar caché de disco (debe ejecutarse en hilo de background)
        Thread {
            Glide.get(context).clearDiskCache()
        }.start()
    }
}
