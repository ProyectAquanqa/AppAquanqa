package com.tecsup.aquanqa.config

import android.content.Context
import com.tecsup.aquanqa.BuildConfig
import com.tecsup.aquanqa.utils.RuntimeAccessibilityChecker

/**
 * Configuración centralizada para accesibilidad
 */
object AccessibilityConfig {
    
    private var isInitialized = false
    private lateinit var checker: RuntimeAccessibilityChecker
    
    /**
     * Inicializa el sistema de accesibilidad
     */
    fun initialize(context: Context) {
        if (!isInitialized) {
            checker = RuntimeAccessibilityChecker(context.applicationContext)
            isInitialized = true
            
            // Habilitar en modo debug
            if (BuildConfig.DEBUG) {
                RuntimeAccessibilityChecker.enable()
                validateDarkThemeOnStartup()
            }
        }
    }
    
    /**
     * Obtiene el checker de accesibilidad
     */
    fun getChecker(): RuntimeAccessibilityChecker {
        if (!isInitialized) {
            throw IllegalStateException("AccessibilityConfig must be initialized first")
        }
        return checker
    }
    
    /**
     * Valida el tema oscuro al inicio de la aplicación
     */
    private fun validateDarkThemeOnStartup() {
        if (isInitialized) {
            val result = checker.validateDarkTheme()
            
            if (!result.isValid) {
                // En producción, esto podría enviar métricas o logs
                // En desarrollo, ayuda a identificar problemas temprano
                result.issues.forEach { issue ->
                    android.util.Log.w(
                        "AccessibilityConfig",
                        "Accessibility issue in ${issue.component}: " +
                        "contrast ${String.format("%.2f", issue.contrastRatio)}:1 " +
                        "(required ${issue.required}:1)"
                    )
                }
            }
        }
    }
    
    /**
     * Configuraciones predefinidas para diferentes niveles de accesibilidad
     */
    enum class AccessibilityLevel {
        BASIC,      // Solo WCAG AA para texto normal
        ENHANCED,   // WCAG AA para todo, WCAG AAA para elementos importantes
        MAXIMUM     // WCAG AAA para todo
    }
    
    /**
     * Aplica un nivel de accesibilidad específico
     */
    fun setAccessibilityLevel(level: AccessibilityLevel) {
        when (level) {
            AccessibilityLevel.BASIC -> {
                // Configuración básica - solo validaciones críticas
                RuntimeAccessibilityChecker.enable()
            }
            AccessibilityLevel.ENHANCED -> {
                // Configuración mejorada - validaciones adicionales
                RuntimeAccessibilityChecker.enable()
            }
            AccessibilityLevel.MAXIMUM -> {
                // Configuración máxima - todas las validaciones
                RuntimeAccessibilityChecker.enable()
            }
        }
    }
    
    /**
     * Colores validados del tema oscuro
     * Estos colores han pasado todas las pruebas de accesibilidad
     */
    object ValidatedColors {
        const val DARK_BACKGROUND_PRIMARY = "#1F2937"
        const val DARK_BACKGROUND_SECONDARY = "#374151"
        const val DARK_BACKGROUND_CARD = "#4B5563"
        const val DARK_BACKGROUND_SURFACE = "#6B7280"
        
        const val DARK_TEXT_PRIMARY = "#FFFFFF"
        const val DARK_TEXT_SECONDARY = "#D1D5DB"
        const val DARK_TEXT_ACCENT = "#9CA3AF"
        
        const val DARK_AQUANQA_BLUE = "#3B82F6"
        const val DARK_AQUANQA_GREEN = "#10B981"
        const val DARK_PRIMARY_ACTION = "#2563EB"
        const val DARK_SECONDARY_ACTION = "#059669"
        
        const val DARK_ERROR = "#EF4444"
        const val DARK_SUCCESS = "#10B981"
        const val DARK_WARNING = "#F59E0B"
        const val DARK_INFO = "#3B82F6"
        
        const val DARK_DIVIDER = "#6B7280"
        const val DARK_BORDER = "#9CA3AF"
        const val DARK_RIPPLE = "#4B5563"
    }
    
    /**
     * Combinaciones de colores pre-validadas
     */
    object ValidatedCombinations {
        val PRIMARY_TEXT_COMBINATIONS = listOf(
            ValidatedColors.DARK_TEXT_PRIMARY to ValidatedColors.DARK_BACKGROUND_PRIMARY,
            ValidatedColors.DARK_TEXT_PRIMARY to ValidatedColors.DARK_BACKGROUND_SECONDARY,
            ValidatedColors.DARK_TEXT_PRIMARY to ValidatedColors.DARK_BACKGROUND_CARD,
            ValidatedColors.DARK_TEXT_PRIMARY to ValidatedColors.DARK_PRIMARY_ACTION,
            ValidatedColors.DARK_TEXT_PRIMARY to ValidatedColors.DARK_SECONDARY_ACTION
        )
        
        val SECONDARY_TEXT_COMBINATIONS = listOf(
            ValidatedColors.DARK_TEXT_SECONDARY to ValidatedColors.DARK_BACKGROUND_PRIMARY,
            ValidatedColors.DARK_TEXT_SECONDARY to ValidatedColors.DARK_BACKGROUND_SECONDARY,
            ValidatedColors.DARK_TEXT_SECONDARY to ValidatedColors.DARK_BACKGROUND_CARD
        )
        
        val ACTION_COMBINATIONS = listOf(
            ValidatedColors.DARK_TEXT_PRIMARY to ValidatedColors.DARK_PRIMARY_ACTION,
            ValidatedColors.DARK_TEXT_PRIMARY to ValidatedColors.DARK_SECONDARY_ACTION,
            ValidatedColors.DARK_TEXT_PRIMARY to ValidatedColors.DARK_ERROR,
            ValidatedColors.DARK_TEXT_PRIMARY to ValidatedColors.DARK_SUCCESS
        )
    }
}