package com.tecsup.aquanqa.utils

import android.graphics.Color
import kotlin.math.pow

/**
 * Utilidad para validar contraste y accesibilidad según estándares WCAG 2.1
 */
object AccessibilityValidator {
    
    // Ratios mínimos según WCAG 2.1
    private const val WCAG_AA_NORMAL_RATIO = 4.5
    private const val WCAG_AA_LARGE_RATIO = 3.0
    private const val WCAG_AAA_NORMAL_RATIO = 7.0
    private const val WCAG_AAA_LARGE_RATIO = 4.5
    
    /**
     * Calcula el ratio de contraste entre dos colores
     * @param foregroundColor Color del texto/primer plano
     * @param backgroundColor Color del fondo
     * @return Ratio de contraste (1.0 - 21.0)
     */
    fun calculateContrastRatio(foregroundColor: String, backgroundColor: String): Double {
        val foreground = parseColor(foregroundColor)
        val background = parseColor(backgroundColor)
        
        val foregroundLuminance = getRelativeLuminance(foreground)
        val backgroundLuminance = getRelativeLuminance(background)
        
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        
        return (lighter + 0.05) / (darker + 0.05)
    }
    
    /**
     * Valida si un color cumple con WCAG AA para texto normal
     */
    fun validateWCAG_AA_Normal(foregroundColor: String, backgroundColor: String): Boolean {
        return calculateContrastRatio(foregroundColor, backgroundColor) >= WCAG_AA_NORMAL_RATIO
    }
    
    /**
     * Valida si un color cumple con WCAG AA para texto grande
     */
    fun validateWCAG_AA_Large(foregroundColor: String, backgroundColor: String): Boolean {
        return calculateContrastRatio(foregroundColor, backgroundColor) >= WCAG_AA_LARGE_RATIO
    }
    
    /**
     * Valida si un color cumple con WCAG AAA para texto normal
     */
    fun validateWCAG_AAA_Normal(foregroundColor: String, backgroundColor: String): Boolean {
        return calculateContrastRatio(foregroundColor, backgroundColor) >= WCAG_AAA_NORMAL_RATIO
    }
    
    /**
     * Valida si un color cumple con WCAG AAA para texto grande
     */
    fun validateWCAG_AAA_Large(foregroundColor: String, backgroundColor: String): Boolean {
        return calculateContrastRatio(foregroundColor, backgroundColor) >= WCAG_AAA_LARGE_RATIO
    }
    
    /**
     * Valida el formato de color hexadecimal
     */
    fun validateColorFormat(color: String): Boolean {
        return color.matches(Regex("^#[0-9A-Fa-f]{6}$"))
    }
    
    /**
     * Obtiene un color de fallback si el color proporcionado no es válido
     */
    fun getColorFallback(colorName: String): String {
        return when (colorName.lowercase()) {
            "primary_background", "dark_background_primary" -> "#1F2937"
            "secondary_background", "dark_background_secondary" -> "#374151"
            "card_background", "dark_background_card" -> "#4B5563"
            "surface_background", "dark_background_surface" -> "#6B7280"
            "primary_text", "dark_text_primary" -> "#FFFFFF"
            "secondary_text", "dark_text_secondary" -> "#D1D5DB"
            "accent_text", "dark_text_accent" -> "#9CA3AF"
            "aquanqa_blue", "dark_aquanqa_blue" -> "#3B82F6"
            "aquanqa_green", "dark_aquanqa_green" -> "#10B981"
            "primary_action", "dark_primary_action" -> "#2563EB"
            "secondary_action", "dark_secondary_action" -> "#059669"
            "divider", "dark_divider" -> "#6B7280"
            "border", "dark_border" -> "#9CA3AF"
            "ripple", "dark_ripple" -> "#4B5563"
            "error", "dark_error" -> "#EF4444"
            "success", "dark_success" -> "#10B981"
            "warning", "dark_warning" -> "#F59E0B"
            "info", "dark_info" -> "#3B82F6"
            else -> "#1F2937" // Default fallback
        }
    }
    
    /**
     * Convierte string de color a Color
     */
    private fun parseColor(colorString: String): Int {
        return try {
            if (validateColorFormat(colorString)) {
                Color.parseColor(colorString)
            } else {
                Color.parseColor(getColorFallback(colorString))
            }
        } catch (e: IllegalArgumentException) {
            Color.parseColor("#1F2937") // Fallback seguro
        }
    }
    
    /**
     * Calcula la luminancia relativa de un color
     */
    private fun getRelativeLuminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        
        val rLinear = if (r <= 0.03928) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
        val gLinear = if (g <= 0.03928) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
        val bLinear = if (b <= 0.03928) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)
        
        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
    }
    
    /**
     * Genera un reporte de accesibilidad para una combinación de colores
     */
    fun generateAccessibilityReport(
        foregroundColor: String, 
        backgroundColor: String,
        textSize: TextSize = TextSize.NORMAL
    ): AccessibilityReport {
        val ratio = calculateContrastRatio(foregroundColor, backgroundColor)
        
        val wcagAA = when (textSize) {
            TextSize.NORMAL -> ratio >= WCAG_AA_NORMAL_RATIO
            TextSize.LARGE -> ratio >= WCAG_AA_LARGE_RATIO
        }
        
        val wcagAAA = when (textSize) {
            TextSize.NORMAL -> ratio >= WCAG_AAA_NORMAL_RATIO
            TextSize.LARGE -> ratio >= WCAG_AAA_LARGE_RATIO
        }
        
        return AccessibilityReport(
            contrastRatio = ratio,
            wcagAA = wcagAA,
            wcagAAA = wcagAAA,
            foregroundColor = foregroundColor,
            backgroundColor = backgroundColor,
            textSize = textSize
        )
    }
    
    enum class TextSize {
        NORMAL, // < 18sp regular o < 14sp bold
        LARGE   // >= 18sp regular o >= 14sp bold
    }
    
    data class AccessibilityReport(
        val contrastRatio: Double,
        val wcagAA: Boolean,
        val wcagAAA: Boolean,
        val foregroundColor: String,
        val backgroundColor: String,
        val textSize: TextSize
    ) {
        fun getRecommendation(): String {
            return when {
                wcagAAA -> "✅ Excelente contraste (WCAG AAA)"
                wcagAA -> "✅ Buen contraste (WCAG AA)"
                else -> "❌ Contraste insuficiente (${String.format("%.2f", contrastRatio)}:1)"
            }
        }
    }
}