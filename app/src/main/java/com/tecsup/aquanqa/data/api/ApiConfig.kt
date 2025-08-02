package com.tecsup.aquanqa.data.api

/**
 * Clase de utilidad para configurar la API
 */
object ApiConfig {
    const val BASE_URL = "http://192.168.18.13:8000/"
    
    // Obtener la URL base para recursos como imágenes
    const val MEDIA_URL = "${BASE_URL}media/"
    // Espera en segundos para conexiones normales
    const val TIMEOUT = 30L
    // Espera en segundos para subidas de archivos
    const val UPLOAD_TIMEOUT = 60L
    // Prefijo del token
    const val TOKEN_PREFIX = "Bearer "
} 