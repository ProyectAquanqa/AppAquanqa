package com.tecsup.aquanqa.utils

import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.tecsup.aquanqa.BuildConfig

/**
 * Verificador de accesibilidad en tiempo de ejecución
 * Valida colores y contraste durante el desarrollo
 */
class RuntimeAccessibilityChecker(private val context: Context) {
    
    companion object {
        private const val TAG = "AccessibilityChecker"
        private var isEnabled = BuildConfig.DEBUG // Solo en debug por defecto
        
        fun enable() {
            isEnabled = true
        }
        
        fun disable() {
            isEnabled = false
        }
    }
    
    /**
     * Valida el contraste entre dos recursos de color
     */
    fun validateContrast(
        @ColorRes foregroundColorRes: Int,
        @ColorRes backgroundColorRes: Int,
        textSize: AccessibilityValidator.TextSize = AccessibilityValidator.TextSize.NORMAL,
        componentName: String = "Unknown"
    ): Boolean {
        if (!isEnabled) return true
        
        return try {
            val foregroundColor = ContextCompat.getColor(context, foregroundColorRes)
            val backgroundColor = ContextCompat.getColor(context, backgroundColorRes)
            
            val foregroundHex = String.format("#%06X", 0xFFFFFF and foregroundColor)
            val backgroundHex = String.format("#%06X", 0xFFFFFF and backgroundColor)
            
            val report = AccessibilityValidator.generateAccessibilityReport(
                foregroundHex, 
                backgroundHex, 
                textSize
            )
            
            if (!report.wcagAA) {
                Log.w(TAG, "❌ ACCESSIBILITY WARNING in $componentName:")
                Log.w(TAG, "   Foreground: $foregroundHex")
                Log.w(TAG, "   Background: $backgroundHex")
                Log.w(TAG, "   Contrast: ${String.format("%.2f", report.contrastRatio)}:1")
                Log.w(TAG, "   Required: ${if (textSize == AccessibilityValidator.TextSize.NORMAL) "4.5" else "3.0"}:1")
            } else {
                Log.d(TAG, "✅ ACCESSIBILITY OK in $componentName: ${String.format("%.2f", report.contrastRatio)}:1")
            }
            
            report.wcagAA
        } catch (e: Exception) {
            Log.e(TAG, "Error validating contrast for $componentName", e)
            false
        }
    }
    
    /**
     * Valida el contraste usando colores hexadecimales directamente
     */
    fun validateContrastHex(
        foregroundColor: String,
        backgroundColor: String,
        textSize: AccessibilityValidator.TextSize = AccessibilityValidator.TextSize.NORMAL,
        componentName: String = "Unknown"
    ): Boolean {
        if (!isEnabled) return true
        
        val report = AccessibilityValidator.generateAccessibilityReport(
            foregroundColor, 
            backgroundColor, 
            textSize
        )
        
        if (!report.wcagAA) {
            Log.w(TAG, "❌ ACCESSIBILITY WARNING in $componentName:")
            Log.w(TAG, "   ${report.getRecommendation()}")
        } else {
            Log.d(TAG, "✅ ACCESSIBILITY OK in $componentName: ${report.getRecommendation()}")
        }
        
        return report.wcagAA
    }
    
    /**
     * Ejecuta una validación completa del tema oscuro
     */
    fun validateDarkTheme(): ValidationResult {
        if (!isEnabled) return ValidationResult(true, emptyList())
        
        Log.i(TAG, "🔍 Starting Dark Theme Accessibility Validation...")
        
        val issues = mutableListOf<AccessibilityIssue>()
        
        // Validaciones principales del tema oscuro
        val validations = listOf(
            ValidationCase("Primary Text on Primary Background", "#FFFFFF", "#1F2937"),
            ValidationCase("Secondary Text on Primary Background", "#D1D5DB", "#1F2937"),
            ValidationCase("Primary Text on Secondary Background", "#FFFFFF", "#374151"),
            ValidationCase("Primary Text on Card Background", "#FFFFFF", "#4B5563"),
            ValidationCase("Primary Text on Primary Action", "#FFFFFF", "#2563EB"),
            ValidationCase("Primary Text on Secondary Action", "#FFFFFF", "#059669"),
            ValidationCase("Primary Text on Error", "#FFFFFF", "#EF4444"),
            ValidationCase("Primary Text on Success", "#FFFFFF", "#10B981"),
            ValidationCase("Primary Text on Warning", "#FFFFFF", "#F59E0B"),
            ValidationCase("Accent Text on Primary Background (Large)", "#9CA3AF", "#1F2937", AccessibilityValidator.TextSize.LARGE)
        )
        
        validations.forEach { validation ->
            val isValid = validateContrastHex(
                validation.foreground,
                validation.background,
                validation.textSize,
                validation.name
            )
            
            if (!isValid) {
                val ratio = AccessibilityValidator.calculateContrastRatio(validation.foreground, validation.background)
                issues.add(
                    AccessibilityIssue(
                        component = validation.name,
                        foregroundColor = validation.foreground,
                        backgroundColor = validation.background,
                        contrastRatio = ratio,
                        required = if (validation.textSize == AccessibilityValidator.TextSize.NORMAL) 4.5 else 3.0
                    )
                )
            }
        }
        
        val isValid = issues.isEmpty()
        
        Log.i(TAG, if (isValid) {
            "✅ Dark Theme Accessibility Validation PASSED"
        } else {
            "❌ Dark Theme Accessibility Validation FAILED with ${issues.size} issues"
        })
        
        return ValidationResult(isValid, issues)
    }
    
    /**
     * Valida un componente específico con logging detallado
     */
    fun validateComponent(
        componentName: String,
        foregroundColor: String,
        backgroundColor: String,
        textSize: AccessibilityValidator.TextSize = AccessibilityValidator.TextSize.NORMAL
    ) {
        if (!isEnabled) return
        
        Log.d(TAG, "🔍 Validating component: $componentName")
        
        val report = AccessibilityValidator.generateAccessibilityReport(
            foregroundColor,
            backgroundColor,
            textSize
        )
        
        Log.d(TAG, "   Foreground: $foregroundColor")
        Log.d(TAG, "   Background: $backgroundColor")
        Log.d(TAG, "   Contrast Ratio: ${String.format("%.2f", report.contrastRatio)}:1")
        Log.d(TAG, "   WCAG AA: ${if (report.wcagAA) "✅ PASS" else "❌ FAIL"}")
        Log.d(TAG, "   WCAG AAA: ${if (report.wcagAAA) "✅ PASS" else "❌ FAIL"}")
        Log.d(TAG, "   Recommendation: ${report.getRecommendation()}")
    }
    
    data class ValidationCase(
        val name: String,
        val foreground: String,
        val background: String,
        val textSize: AccessibilityValidator.TextSize = AccessibilityValidator.TextSize.NORMAL
    )
    
    data class AccessibilityIssue(
        val component: String,
        val foregroundColor: String,
        val backgroundColor: String,
        val contrastRatio: Double,
        val required: Double
    )
    
    data class ValidationResult(
        val isValid: Boolean,
        val issues: List<AccessibilityIssue>
    )
}