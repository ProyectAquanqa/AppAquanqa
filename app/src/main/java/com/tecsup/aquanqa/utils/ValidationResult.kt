package com.tecsup.aquanqa.utils

/**
 * Resultado de una operación de validación.
 * Utiliza un sealed class para garantizar type safety y manejo exhaustivo de casos.
 */
sealed class ValidationResult {
    /**
     * Validación exitosa con el mensaje limpio y procesado.
     */
    data class Success(val message: String) : ValidationResult()
    
    /**
     * Error de validación con descripción del problema.
     */
    data class Error(val error: String) : ValidationResult()
}