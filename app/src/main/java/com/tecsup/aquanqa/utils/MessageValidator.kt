package com.tecsup.aquanqa.utils

/**
 * Validador de mensajes del chatbot.
 * Contiene toda la lógica de validación y limpieza de mensajes de entrada.
 */
object MessageValidator {
    
    // Constantes de validación
    private const val MIN_MESSAGE_LENGTH = 1
    private const val MAX_MESSAGE_LENGTH = 1000
    private const val MAX_CONSECUTIVE_WHITESPACE = 5
    
    /**
     * Valida y limpia un mensaje de entrada del usuario.
     * 
     * @param message El mensaje original del usuario
     * @return ValidationResult con el mensaje validado o un error
     */
    fun validateMessage(message: String): ValidationResult {
        // 1. Verificar que el mensaje no sea nulo o vacío
        if (message.isBlank()) {
            return ValidationResult.Error("El mensaje no puede estar vacío")
        }
        
        // 2. Limpiar el mensaje (trim y normalizar espacios)
        val cleanedMessage = cleanMessage(message)
        
        // 3. Verificar longitud mínima después de limpiar
        if (cleanedMessage.length < MIN_MESSAGE_LENGTH) {
            return ValidationResult.Error("El mensaje es demasiado corto")
        }
        
        // 4. Verificar longitud máxima
        if (cleanedMessage.length > MAX_MESSAGE_LENGTH) {
            return ValidationResult.Error("El mensaje es demasiado largo (máximo $MAX_MESSAGE_LENGTH caracteres)")
        }
        
        // 5. Verificar contenido válido (no solo espacios o caracteres especiales)
        if (!hasValidContent(cleanedMessage)) {
            return ValidationResult.Error("El mensaje debe contener texto válido")
        }
        
        // 6. Detectar posible spam o contenido malicioso
        if (isPotentialSpam(cleanedMessage)) {
            return ValidationResult.Error("El mensaje parece contener spam o contenido no válido")
        }
        
        return ValidationResult.Success(cleanedMessage)
    }
    
    /**
     * Limpia un mensaje eliminando espacios extra y normalizando formato.
     */
    private fun cleanMessage(message: String): String {
        return message.trim()
            .replace(Regex("\\s+"), " ") // Normalizar múltiples espacios a uno solo
            .replace(Regex("\\n{3,}"), "\n\n") // Limitar saltos de línea consecutivos
    }
    
    /**
     * Verifica si el mensaje tiene contenido válido (no solo espacios o símbolos).
     */
    private fun hasValidContent(message: String): Boolean {
        // El mensaje debe tener al menos algunos caracteres alfanuméricos
        return message.any { it.isLetterOrDigit() }
    }
    
    /**
     * Detecta patrones que podrían indicar spam o contenido malicioso.
     */
    private fun isPotentialSpam(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Patrones de spam comunes
        val spamPatterns = listOf(
            Regex("(.)\\1{10,}"), // Más de 10 caracteres consecutivos iguales
            Regex("https?://[^\\s]+"), // URLs (opcional: podrías permitir URLs válidas)
            Regex("[\\p{So}\\p{Sk}]{5,}") // Más de 5 símbolos/emojis consecutivos
        )
        
        return spamPatterns.any { pattern -> pattern.containsMatchIn(message) }
    }
}