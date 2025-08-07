package com.tecsup.aquanqa.utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Clase utilitaria para el manejo y formateo de fechas.
 * 
 * Proporciona métodos para formatear fechas en español y otros
 * formatos comunes utilizados en la aplicación.
 */
object DateUtils {
    
    /**
     * Mapa de nombres de días de la semana en español.
     */
    private val daysOfWeekSpanish = mapOf(
        "MONDAY" to "Lunes",
        "TUESDAY" to "Martes", 
        "WEDNESDAY" to "Miércoles",
        "THURSDAY" to "Jueves",
        "FRIDAY" to "Viernes",
        "SATURDAY" to "Sábado",
        "SUNDAY" to "Domingo"
    )
    
    /**
     * Mapa de nombres de meses en español.
     */
    private val monthsSpanish = mapOf(
        1 to "enero", 2 to "febrero", 3 to "marzo", 4 to "abril",
        5 to "mayo", 6 to "junio", 7 to "julio", 8 to "agosto",
        9 to "septiembre", 10 to "octubre", 11 to "noviembre", 12 to "diciembre"
    )
    
    /**
     * Obtiene la fecha actual formateada en español.
     * 
     * Formato: "Lunes, 28 de julio de 2025"
     * 
     * @return String Fecha actual en formato español legible
     */
    fun getCurrentDateInSpanish(): String {
        val currentDate = LocalDate.now()
        val dayOfWeek = daysOfWeekSpanish[currentDate.dayOfWeek.toString()] ?: "Lunes"
        val dayOfMonth = currentDate.dayOfMonth
        val month = monthsSpanish[currentDate.monthValue] ?: "enero"
        val year = currentDate.year
        
        return "$dayOfWeek, $dayOfMonth de $month de $year"
    }
    
    /**
     * Formatea una fecha dada en español.
     * 
     * @param date LocalDate La fecha a formatear
     * @return String Fecha en formato español "Día, DD de mes de YYYY"
     */
    fun formatDateInSpanish(date: LocalDate): String {
        val dayOfWeek = daysOfWeekSpanish[date.dayOfWeek.toString()] ?: "Lunes"
        val dayOfMonth = date.dayOfMonth
        val month = monthsSpanish[date.monthValue] ?: "enero"
        val year = date.year
        
        return "$dayOfWeek, $dayOfMonth de $month de $year"
    }
    
    /**
     * Formatea una fecha sin el día de la semana.
     * 
     * @param date LocalDate La fecha a formatear
     * @return String Fecha en formato "DD de mes de YYYY"
     */
    fun formatDateShortSpanish(date: LocalDate): String {
        val dayOfMonth = date.dayOfMonth
        val month = monthsSpanish[date.monthValue] ?: "enero"
        val year = date.year
        
        return "$dayOfMonth de $month de $year"
    }
    
    /**
     * Formatea una fecha para almuerzos con día y fecha sin año.
     * 
     * @param date LocalDate La fecha a formatear
     * @return String Fecha en formato "Lunes 18 de agosto"
     */
    fun formatLunchDate(date: LocalDate): String {
        val dayOfWeek = daysOfWeekSpanish[date.dayOfWeek.toString()] ?: "Lunes"
        val dayOfMonth = date.dayOfMonth
        val month = monthsSpanish[date.monthValue] ?: "enero"
        
        return "$dayOfWeek $dayOfMonth de $month"
    }
    
    /**
     * Convierte fecha ISO de almuerzo a formato para UI.
     * 
     * @param isoDateString Fecha en formato "YYYY-MM-DD"
     * @return String Fecha formateada como "Lunes 18 de agosto"
     */
    fun formatLunchDateFromISO(isoDateString: String?): String {
        if (isoDateString.isNullOrBlank()) return "Fecha no disponible"
        
        return try {
            val parts = isoDateString.split("-")
            if (parts.size == 3) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                
                val localDate = LocalDate.of(year, month, day)
                formatLunchDate(localDate)
            } else {
                "Fecha inválida"
            }
        } catch (e: Exception) {
            "Fecha inválida"
        }
    }
    
    /**
     * Obtiene un saludo apropiado según la hora del día.
     * 
     * @return String Saludo apropiado ("Buenos días", "Buenas tardes", "Buenas noches")
     */
    fun getGreetingBasedOnTime(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        return when (hour) {
            in 6..11 -> "Buenos días"
            in 12..17 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }
    
    /**
     * Convierte una fecha en formato ISO 8601 a un formato legible en español.
     * Útil para fechas que vienen de la API.
     * 
     * @param isoDateString Fecha en formato ISO 8601 (ej: "2025-01-28T10:30:00Z")
     * @return String Fecha formateada en español o cadena vacía si hay error
     */
    fun formatISODateToSpanish(isoDateString: String?): String {
        if (isoDateString.isNullOrBlank()) return ""
        
        return try {
            // Formato ISO común: 2025-01-28T10:30:00Z o 2025-01-28T10:30:00
            val cleanDate = isoDateString.replace("Z", "").split("T")[0]
            val parts = cleanDate.split("-")
            
            if (parts.size == 3) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                
                val localDate = LocalDate.of(year, month, day)
                formatDateShortSpanish(localDate)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Calcula el tiempo transcurrido desde una fecha dada.
     * 
     * @param isoDateString Fecha en formato ISO 8601
     * @return String Tiempo transcurrido en formato amigable ("hace 2 días", "hace 1 semana")
     */
    fun getTimeAgo(isoDateString: String?): String {
        if (isoDateString.isNullOrBlank()) return "Fecha desconocida"
        
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(isoDateString.replace("Z", ""))
            val now = Date()
            val diffInMillis = now.time - (date?.time ?: 0)
            val diffInMinutes = diffInMillis / (1000 * 60)
            val diffInHours = diffInMinutes / 60
            val diffInDays = diffInHours / 24
            
            when {
                diffInMinutes < 1 -> "Hace un momento"
                diffInMinutes < 60 -> "Hace ${diffInMinutes.toInt()} minutos"
                diffInHours < 24 -> "Hace ${diffInHours.toInt()} horas"
                diffInDays < 7 -> "Hace ${diffInDays.toInt()} días"
                diffInDays < 30 -> "Hace ${(diffInDays / 7).toInt()} semanas"
                else -> "Hace ${(diffInDays / 30).toInt()} meses"
            }
        } catch (e: Exception) {
            "Fecha desconocida"
        }
    }
    
    /**
     * Verifica si una fecha es de hoy.
     * 
     * @param isoDateString Fecha en formato ISO 8601
     * @return Boolean true si la fecha es de hoy, false en caso contrario
     */
    fun isToday(isoDateString: String?): Boolean {
        if (isoDateString.isNullOrBlank()) return false
        
        return try {
            val cleanDate = isoDateString.replace("Z", "").split("T")[0]
            val today = LocalDate.now().toString()
            cleanDate == today
        } catch (e: Exception) {
            false
        }
    }
} 