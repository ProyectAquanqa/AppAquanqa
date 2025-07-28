package com.tecsup.aquanqa.utils

import android.content.Context
import android.widget.TextView
import com.tecsup.aquanqa.config.AccessibilityConfig

/**
 * Ejemplo de uso de las utilidades de accesibilidad
 * Este archivo muestra cómo integrar la validación de accesibilidad en la aplicación
 */
class AccessibilityUsageExample {
    
    /**
     * Ejemplo de inicialización en Application o MainActivity
     */
    fun initializeAccessibility(context: Context) {
        // Inicializar el sistema de accesibilidad
        AccessibilityConfig.initialize(context)
        
        // Configurar nivel de accesibilidad
        AccessibilityConfig.setAccessibilityLevel(AccessibilityConfig.AccessibilityLevel.ENHANCED)
        
        // Validar tema oscuro al inicio
        val checker = AccessibilityConfig.getChecker()
        val result = checker.validateDarkTheme()
        
        if (!result.isValid) {
            // Manejar problemas de accesibilidad
            result.issues.forEach { issue ->
                android.util.Log.w("Accessibility", "Issue: ${issue.component}")
            }
        }
    }
    
    /**
     * Ejemplo de validación en un Fragment o Activity
     */
    fun validateFragmentAccessibility(context: Context) {
        val checker = RuntimeAccessibilityChecker(context)
        
        // Validar combinaciones específicas
        checker.validateContrastHex(
            AccessibilityConfig.ValidatedColors.DARK_TEXT_PRIMARY,
            AccessibilityConfig.ValidatedColors.DARK_BACKGROUND_PRIMARY,
            AccessibilityValidator.TextSize.NORMAL,
            "ProfileFragment.nameTextView"
        )
        
        // Validar componente específico con logging detallado
        checker.validateComponent(
            "ChatbotFragment.messageText",
            "#FFFFFF",
            "#2563EB",
            AccessibilityValidator.TextSize.NORMAL
        )
    }
    
    /**
     * Ejemplo de validación automática de TextView
     */
    fun validateTextView(textView: TextView) {
        // Validación automática basada en el tamaño del texto
        val isValid = textView.validateAccessibilityAuto("MyTextView")
        
        if (!isValid) {
            // Manejar problema de accesibilidad
            android.util.Log.w("Accessibility", "TextView failed accessibility check")
        }
        
        // Validación manual con parámetros específicos
        textView.validateAccessibility(
            "CustomTextView",
            AccessibilityValidator.TextSize.LARGE
        )
    }
    
    /**
     * Ejemplo de validación de contraste directo
     */
    fun validateColorsDirectly() {
        // Validar formato de color
        val isValidFormat = AccessibilityValidator.validateColorFormat("#1F2937")
        
        // Calcular ratio de contraste
        val ratio = AccessibilityValidator.calculateContrastRatio("#FFFFFF", "#1F2937")
        
        // Validar WCAG AA
        val passesWCAG = AccessibilityValidator.validateWCAG_AA_Normal("#FFFFFF", "#1F2937")
        
        // Generar reporte completo
        val report = AccessibilityValidator.generateAccessibilityReport(
            "#FFFFFF",
            "#1F2937",
            AccessibilityValidator.TextSize.NORMAL
        )
        
        android.util.Log.d("Accessibility", "Report: ${report.getRecommendation()}")
    }
    
    /**
     * Ejemplo de uso de colores validados
     */
    fun useValidatedColors() {
        // Usar colores pre-validados
        val backgroundColor = AccessibilityConfig.ValidatedColors.DARK_BACKGROUND_PRIMARY
        val textColor = AccessibilityConfig.ValidatedColors.DARK_TEXT_PRIMARY
        
        // Usar combinaciones pre-validadas
        AccessibilityConfig.ValidatedCombinations.PRIMARY_TEXT_COMBINATIONS.forEach { (text, background) ->
            android.util.Log.d("Accessibility", "Valid combination: $text on $background")
        }
    }
    
    /**
     * Ejemplo de manejo de fallbacks
     */
    fun handleColorFallbacks() {
        // Obtener color de fallback para color inválido
        val fallbackColor = AccessibilityValidator.getColorFallback("invalid_color")
        
        // Validar con fallback automático
        val ratio = AccessibilityValidator.calculateContrastRatio("invalid_color", "another_invalid")
        // Los colores inválidos automáticamente usan fallbacks seguros
        
        android.util.Log.d("Accessibility", "Fallback color: $fallbackColor, Ratio: $ratio")
    }
}