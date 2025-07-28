package com.tecsup.aquanqa

import com.tecsup.aquanqa.utils.AccessibilityValidator
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests de accesibilidad para el tema oscuro
 * Valida que todos los colores cumplan con estándares WCAG 2.1
 */
class DarkThemeAccessibilityTest {
    
    // Colores del tema oscuro
    private val darkBackgroundPrimary = "#1F2937"
    private val darkBackgroundSecondary = "#374151"
    private val darkBackgroundCard = "#4B5563"
    private val darkBackgroundSurface = "#6B7280"
    
    private val darkTextPrimary = "#FFFFFF"
    private val darkTextSecondary = "#D1D5DB"
    private val darkTextAccent = "#9CA3AF"
    
    private val darkAquanqaBlue = "#3B82F6"
    private val darkAquanqaGreen = "#10B981"
    private val darkPrimaryAction = "#2563EB"
    private val darkSecondaryAction = "#059669"
    
    private val darkDivider = "#6B7280"
    private val darkBorder = "#9CA3AF"
    private val darkError = "#EF4444"
    private val darkSuccess = "#10B981"
    private val darkWarning = "#F59E0B"
    
    @Test
    fun testDarkThemeColors_ValidateFormat() {
        // Validar que todos los colores tengan formato hexadecimal válido
        assertTrue("darkBackgroundPrimary format", AccessibilityValidator.validateColorFormat(darkBackgroundPrimary))
        assertTrue("darkBackgroundSecondary format", AccessibilityValidator.validateColorFormat(darkBackgroundSecondary))
        assertTrue("darkBackgroundCard format", AccessibilityValidator.validateColorFormat(darkBackgroundCard))
        assertTrue("darkBackgroundSurface format", AccessibilityValidator.validateColorFormat(darkBackgroundSurface))
        
        assertTrue("darkTextPrimary format", AccessibilityValidator.validateColorFormat(darkTextPrimary))
        assertTrue("darkTextSecondary format", AccessibilityValidator.validateColorFormat(darkTextSecondary))
        assertTrue("darkTextAccent format", AccessibilityValidator.validateColorFormat(darkTextAccent))
        
        assertTrue("darkAquanqaBlue format", AccessibilityValidator.validateColorFormat(darkAquanqaBlue))
        assertTrue("darkAquanqaGreen format", AccessibilityValidator.validateColorFormat(darkAquanqaGreen))
        assertTrue("darkPrimaryAction format", AccessibilityValidator.validateColorFormat(darkPrimaryAction))
    }
    
    @Test
    fun testPrimaryTextContrast_WCAG_AA() {
        // Texto primario debe cumplir WCAG AA en todos los fondos principales
        assertTrue(
            "Primary text on primary background", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextPrimary, darkBackgroundPrimary)
        )
        assertTrue(
            "Primary text on secondary background", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextPrimary, darkBackgroundSecondary)
        )
        assertTrue(
            "Primary text on card background", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextPrimary, darkBackgroundCard)
        )
        assertTrue(
            "Primary text on surface background", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextPrimary, darkBackgroundSurface)
        )
    }
    
    @Test
    fun testSecondaryTextContrast_WCAG_AA() {
        // Texto secundario debe cumplir WCAG AA en fondos principales
        assertTrue(
            "Secondary text on primary background", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextSecondary, darkBackgroundPrimary)
        )
        assertTrue(
            "Secondary text on secondary background", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextSecondary, darkBackgroundSecondary)
        )
        assertTrue(
            "Secondary text on card background", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextSecondary, darkBackgroundCard)
        )
    }
    
    @Test
    fun testActionButtonsContrast_WCAG_AA() {
        // Botones de acción deben tener buen contraste con texto blanco
        assertTrue(
            "White text on primary action button", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextPrimary, darkPrimaryAction)
        )
        assertTrue(
            "White text on secondary action button", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextPrimary, darkSecondaryAction)
        )
        assertTrue(
            "White text on Aquanqa blue", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextPrimary, darkAquanqaBlue)
        )
        assertTrue(
            "White text on Aquanqa green", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextPrimary, darkAquanqaGreen)
        )
    }
    
    @Test
    fun testStatusColorsContrast_WCAG_AA() {
        // Colores de estado deben tener buen contraste con texto blanco
        assertTrue(
            "White text on error color", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextPrimary, darkError)
        )
        assertTrue(
            "White text on success color", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextPrimary, darkSuccess)
        )
        assertTrue(
            "White text on warning color", 
            AccessibilityValidator.validateWCAG_AA_Normal(darkTextPrimary, darkWarning)
        )
    }
    
    @Test
    fun testLargeTextContrast_WCAG_AA() {
        // Texto grande debe cumplir WCAG AA (ratio más bajo permitido)
        assertTrue(
            "Large primary text on primary background", 
            AccessibilityValidator.validateWCAG_AA_Large(darkTextPrimary, darkBackgroundPrimary)
        )
        assertTrue(
            "Large secondary text on primary background", 
            AccessibilityValidator.validateWCAG_AA_Large(darkTextSecondary, darkBackgroundPrimary)
        )
        assertTrue(
            "Large accent text on primary background", 
            AccessibilityValidator.validateWCAG_AA_Large(darkTextAccent, darkBackgroundPrimary)
        )
    }
    
    @Test
    fun testContrastRatios_MinimumValues() {
        // Verificar ratios específicos para casos críticos
        val primaryTextRatio = AccessibilityValidator.calculateContrastRatio(darkTextPrimary, darkBackgroundPrimary)
        val secondaryTextRatio = AccessibilityValidator.calculateContrastRatio(darkTextSecondary, darkBackgroundPrimary)
        val accentTextRatio = AccessibilityValidator.calculateContrastRatio(darkTextAccent, darkBackgroundPrimary)
        
        assertTrue("Primary text ratio >= 4.5", primaryTextRatio >= 4.5)
        assertTrue("Secondary text ratio >= 4.5", secondaryTextRatio >= 4.5)
        assertTrue("Accent text ratio >= 3.0", accentTextRatio >= 3.0) // Más permisivo para texto de acento
        
        println("=== CONTRAST RATIOS ===")
        println("Primary text: ${String.format("%.2f", primaryTextRatio)}:1")
        println("Secondary text: ${String.format("%.2f", secondaryTextRatio)}:1")
        println("Accent text: ${String.format("%.2f", accentTextRatio)}:1")
    }
    
    @Test
    fun testAccessibilityReports() {
        // Generar reportes de accesibilidad para combinaciones clave
        val reports = listOf(
            AccessibilityValidator.generateAccessibilityReport(darkTextPrimary, darkBackgroundPrimary),
            AccessibilityValidator.generateAccessibilityReport(darkTextSecondary, darkBackgroundPrimary),
            AccessibilityValidator.generateAccessibilityReport(darkTextPrimary, darkPrimaryAction),
            AccessibilityValidator.generateAccessibilityReport(darkTextPrimary, darkError),
            AccessibilityValidator.generateAccessibilityReport(darkTextAccent, darkBackgroundPrimary, AccessibilityValidator.TextSize.LARGE)
        )
        
        println("\n=== ACCESSIBILITY REPORTS ===")
        reports.forEach { report ->
            println("${report.foregroundColor} on ${report.backgroundColor}: ${report.getRecommendation()}")
            assertTrue("Report should pass WCAG AA", report.wcagAA)
        }
    }
    
    @Test
    fun testColorFallbacks() {
        // Verificar que los fallbacks funcionen correctamente
        assertEquals("#1F2937", AccessibilityValidator.getColorFallback("primary_background"))
        assertEquals("#FFFFFF", AccessibilityValidator.getColorFallback("primary_text"))
        assertEquals("#3B82F6", AccessibilityValidator.getColorFallback("aquanqa_blue"))
        assertEquals("#1F2937", AccessibilityValidator.getColorFallback("unknown_color"))
    }
    
    @Test
    fun testInvalidColorHandling() {
        // Verificar manejo de colores inválidos
        assertFalse("Invalid color format", AccessibilityValidator.validateColorFormat("#GGGGGG"))
        assertFalse("Invalid color format", AccessibilityValidator.validateColorFormat("blue"))
        assertFalse("Invalid color format", AccessibilityValidator.validateColorFormat("#12345"))
        
        // Los colores inválidos deben usar fallbacks y aún así pasar las pruebas
        val ratio = AccessibilityValidator.calculateContrastRatio("invalid_color", "another_invalid")
        assertTrue("Invalid colors should use fallbacks", ratio > 0)
    }
}