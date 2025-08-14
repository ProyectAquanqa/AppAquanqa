package com.tecsup.aquanqa.data.api

/**
 * Configuración centralizada para los endpoints y parámetros de la API.
 */
object ApiConfig {
    /** URL base para todos los endpoints de la API */
    const val BASE_URL = "http://172.16.11.29:8000/"
    
    /** URL base para recursos multimedia */
    const val MEDIA_URL = "${BASE_URL}media/"
    
    /** Tiempo de espera estándar en segundos */
    const val TIMEOUT = 30L
    
    /** Tiempo de espera extendido para subida de archivos en segundos */
    const val UPLOAD_TIMEOUT = 60L
    
    /** Prefijo para tokens de autenticación */
    const val TOKEN_PREFIX = "Bearer "
}