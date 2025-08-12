package com.tecsup.aquanqa.utils

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Utilidad para convertir errores técnicos en mensajes comprensibles para el usuario.
 */
object ErrorHelper {
    
    /**
     * Convierte una excepción en un mensaje user-friendly.
     * 
     * @param exception La excepción a procesar
     * @return Mensaje comprensible para el usuario
     */
    fun getErrorMessage(exception: Throwable): String {
        return when {
            exception is UnknownHostException -> 
                "Sin conexión a internet. Verifique su conexión."
            
            exception is SocketTimeoutException -> 
                "La conexión tardó demasiado. Intente de nuevo."
            
            exception is ConnectException -> 
                "No se pudo conectar al servidor. Intente más tarde."
            
            exception.message?.contains("401") == true || exception.message?.contains("sesión ha expirado") == true -> 
                "Su sesión ha expirado. Inicie sesión nuevamente."
            
            exception.message?.contains("403") == true || exception.message?.contains("permisos") == true -> 
                "No tiene permisos para realizar esta acción."
            
            exception.message?.contains("404") == true || exception.message?.contains("no encontrado") == true -> 
                "El contenido solicitado no está disponible."
            
            exception.message?.contains("500") == true || exception.message?.contains("servidor") == true -> 
                "Problema temporal del servidor. Intente más tarde."
            
            exception.message?.contains("Token de acceso no disponible") == true -> 
                "Su sesión ha expirado. Inicie sesión nuevamente."
            
            else -> "Ocurrió un problema inesperado. Intente de nuevo."
        }
    }
    
    /**
     * Determina si un error requiere reautenticación del usuario.
     */
    fun requiresReauth(exception: Throwable): Boolean {
        return exception.message?.contains("401") == true ||
               exception.message?.contains("Token de acceso no disponible") == true
    }
}