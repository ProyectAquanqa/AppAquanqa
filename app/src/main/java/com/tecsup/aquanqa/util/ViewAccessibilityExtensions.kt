package com.tecsup.aquanqa.utils

import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

/**
 * Extensiones para validar accesibilidad en Views
 */

/**
 * Valida el contraste de un TextView automáticamente
 */
fun TextView.validateAccessibility(
    componentName: String = this::class.java.simpleName,
    textSize: AccessibilityValidator.TextSize = AccessibilityValidator.TextSize.NORMAL
): Boolean {
    val checker = RuntimeAccessibilityChecker(context)
    
    val textColor = currentTextColor
    val backgroundColor = getBackgroundColor()
    
    val textColorHex = String.format("#%06X", 0xFFFFFF and textColor)
    val backgroundColorHex = String.format("#%06X", 0xFFFFFF and backgroundColor)
    
    return checker.validateContrastHex(
        textColorHex,
        backgroundColorHex,
        textSize,
        componentName
    )
}

/**
 * Valida el contraste entre dos colores específicos para una View
 */
fun View.validateContrast(
    @ColorRes foregroundColorRes: Int,
    @ColorRes backgroundColorRes: Int,
    componentName: String = this::class.java.simpleName,
    textSize: AccessibilityValidator.TextSize = AccessibilityValidator.TextSize.NORMAL
): Boolean {
    val checker = RuntimeAccessibilityChecker(context)
    return checker.validateContrast(
        foregroundColorRes,
        backgroundColorRes,
        textSize,
        componentName
    )
}

/**
 * Obtiene el color de fondo de una View
 */
private fun View.getBackgroundColor(): Int {
    return try {
        val drawable = background
        if (drawable is android.graphics.drawable.ColorDrawable) {
            drawable.color
        } else {
            // Fallback al color de fondo primario del tema oscuro
            ContextCompat.getColor(context, android.R.color.transparent)
        }
    } catch (e: Exception) {
        // Fallback seguro
        ContextCompat.getColor(context, android.R.color.transparent)
    }
}

/**
 * Determina el tamaño de texto para validación de accesibilidad
 */
fun TextView.getAccessibilityTextSize(): AccessibilityValidator.TextSize {
    val textSizeSp = textSize / resources.displayMetrics.scaledDensity
    val isBold = typeface?.isBold == true
    
    return if ((textSizeSp >= 18f && !isBold) || (textSizeSp >= 14f && isBold)) {
        AccessibilityValidator.TextSize.LARGE
    } else {
        AccessibilityValidator.TextSize.NORMAL
    }
}

/**
 * Valida automáticamente un TextView usando su tamaño real
 */
fun TextView.validateAccessibilityAuto(
    componentName: String = this::class.java.simpleName
): Boolean {
    return validateAccessibility(componentName, getAccessibilityTextSize())
}

/**
 * Aplica validación de accesibilidad a una View y sus hijos recursivamente
 */
fun View.validateAccessibilityRecursive(
    componentName: String = this::class.java.simpleName
): List<RuntimeAccessibilityChecker.AccessibilityIssue> {
    val issues = mutableListOf<RuntimeAccessibilityChecker.AccessibilityIssue>()
    
    // Validar la vista actual si es un TextView
    if (this is TextView) {
        val isValid = validateAccessibilityAuto("$componentName.${this::class.java.simpleName}")
        if (!isValid) {
            val textColor = String.format("#%06X", 0xFFFFFF and currentTextColor)
            val backgroundColor = String.format("#%06X", 0xFFFFFF and getBackgroundColor())
            val ratio = AccessibilityValidator.calculateContrastRatio(textColor, backgroundColor)
            
            issues.add(
                RuntimeAccessibilityChecker.AccessibilityIssue(
                    component = "$componentName.${this::class.java.simpleName}",
                    foregroundColor = textColor,
                    backgroundColor = backgroundColor,
                    contrastRatio = ratio,
                    required = if (getAccessibilityTextSize() == AccessibilityValidator.TextSize.NORMAL) 4.5 else 3.0
                )
            )
        }
    }
    
    // Validar vistas hijas si es un ViewGroup
    if (this is android.view.ViewGroup) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            issues.addAll(child.validateAccessibilityRecursive("$componentName.child$i"))
        }
    }
    
    return issues
}