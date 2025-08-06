package com.tecsup.aquanqa.data.utils

import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Utilidad para decodificar tokens JWT y extraer información de expiración
 * 
 * Esta clase proporciona métodos para:
 * - Decodificar el payload de un token JWT
 * - Extraer el timestamp de expiración (exp)
 * - Validar si un token está expirado
 * - Calcular tiempo restante hasta la expiración
 */
object JwtDecoder {
    
    private const val TAG = "JwtDecoder"
    private const val EXP_CLAIM = "exp"
    
    /**
     * Datos extraídos de un token JWT
     */
    data class TokenInfo(
        val expirationTime: Long, // Timestamp de expiración en segundos
        val issuedAt: Long? = null, // Timestamp de emisión en segundos (opcional)
        val payload: JSONObject // Payload completo del token
    )
    
    /**
     * Decodifica un token JWT y extrae su información
     * 
     * @param token Token JWT a decodificar
     * @return TokenInfo con los datos extraídos o null si hay error
     */
    fun decodeToken(token: String?): TokenInfo? {
        if (token.isNullOrBlank()) {
            Log.w(TAG, "Token is null or empty")
            return null
        }
        
        return try {
            // JWT tiene formato: header.payload.signature
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.e(TAG, "Invalid JWT format: expected 3 parts, got ${parts.size}")
                return null
            }
            
            // Decodificar el payload (segunda parte)
            val payloadEncoded = parts[1]
            val payloadDecoded = decodeBase64(payloadEncoded)
            val payloadJson = JSONObject(payloadDecoded)
            
            // Extraer timestamp de expiración
            val exp = payloadJson.optLong(EXP_CLAIM, 0L)
            if (exp == 0L) {
                Log.w(TAG, "Token does not contain exp claim")
                return null
            }
            
            // Extraer timestamp de emisión (opcional)
            val iat = payloadJson.optLong("iat", 0L).takeIf { it != 0L }
            
            TokenInfo(
                expirationTime = exp,
                issuedAt = iat,
                payload = payloadJson
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding JWT token", e)
            null
        }
    }
    
    /**
     * Verifica si un token está expirado
     * 
     * @param token Token JWT a verificar
     * @param bufferSeconds Segundos de buffer antes de considerar expirado (default: 30s)
     * @return true si el token está expirado o es inválido
     */
    fun isTokenExpired(token: String?, bufferSeconds: Long = 30L): Boolean {
        val tokenInfo = decodeToken(token) ?: return true
        
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val expirationWithBuffer = tokenInfo.expirationTime - bufferSeconds
        
        return currentTimeSeconds >= expirationWithBuffer
    }
    
    /**
     * Obtiene el tiempo restante hasta la expiración en segundos
     * 
     * @param token Token JWT a verificar
     * @return Segundos restantes hasta expiración, 0 si ya expiró, -1 si es inválido
     */
    fun getTimeUntilExpiration(token: String?): Long {
        val tokenInfo = decodeToken(token) ?: return -1L
        
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val timeRemaining = tokenInfo.expirationTime - currentTimeSeconds
        
        return maxOf(0L, timeRemaining)
    }
    
    /**
     * Verifica si un token necesita ser refrescado pronto
     * 
     * @param token Token JWT a verificar
     * @param thresholdSeconds Umbral en segundos para considerar que necesita refresh (default: 10 minutos)
     * @return true si el token expira en menos del threshold especificado
     */
    fun shouldRefreshToken(token: String?, thresholdSeconds: Long = 600L): Boolean {
        val timeUntilExpiration = getTimeUntilExpiration(token)
        return timeUntilExpiration in 1..thresholdSeconds
    }
    
    /**
     * Decodifica una cadena Base64 URL-safe (usada en JWT)
     */
    private fun decodeBase64(encoded: String): String {
        // JWT usa Base64 URL-safe sin padding
        val paddedEncoded = when (encoded.length % 4) {
            2 -> "$encoded=="
            3 -> "$encoded="
            else -> encoded
        }
        
        val decoded = Base64.decode(paddedEncoded, Base64.URL_SAFE)
        return String(decoded, StandardCharsets.UTF_8)
    }
    
    /**
     * Obtiene información completa del token para debugging
     * 
     * @param token Token JWT a analizar
     * @return String con información formateada del token
     */
    fun getTokenDebugInfo(token: String?): String {
        val tokenInfo = decodeToken(token) ?: return "Token inválido o nulo"
        
        val currentTime = System.currentTimeMillis() / 1000
        val timeRemaining = getTimeUntilExpiration(token)
        val isExpired = isTokenExpired(token)
        val shouldRefresh = shouldRefreshToken(token)
        
        return buildString {
            appendLine("=== Token JWT Info ===")
            appendLine("Expiración: ${tokenInfo.expirationTime}")
            appendLine("Tiempo actual: $currentTime")
            appendLine("Tiempo restante: ${timeRemaining}s")
            appendLine("¿Expirado?: $isExpired")
            appendLine("¿Necesita refresh?: $shouldRefresh")
            tokenInfo.issuedAt?.let { 
                appendLine("Emitido en: $it")
            }
        }
    }
}