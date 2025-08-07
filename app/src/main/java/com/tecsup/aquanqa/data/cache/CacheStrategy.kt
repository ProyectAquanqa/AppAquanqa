package com.tecsup.aquanqa.data.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estrategia de cache inteligente que maneja diferentes tipos de datos
 * con duraciones y políticas específicas para cada tipo.
 */
class CacheStrategy {
    
    companion object {
        // Duraciones específicas por tipo de dato (optimizadas para app multi-usuario)
        const val CATEGORIES_CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 horas (datos estáticos)
        const val EVENTS_CACHE_DURATION = 30 * 60 * 1000L // 30 minutos (balance performance/freshness)
        const val ALMUERZOS_CACHE_DURATION = 2 * 60 * 60 * 1000L // 2 horas (menús diarios)
        const val USER_PROFILE_CACHE_DURATION = 60 * 60 * 1000L // 1 hora (datos personales)
        const val NOTIFICATIONS_CACHE_DURATION = 5 * 60 * 1000L // 5 minutos (tiempo real)
    }
    
    /**
     * Enum que define los diferentes tipos de cache disponibles
     */
    enum class CacheType(val duration: Long) {
        CATEGORIES(CATEGORIES_CACHE_DURATION),
        EVENTS(EVENTS_CACHE_DURATION),
        ALMUERZOS(ALMUERZOS_CACHE_DURATION), // ✅ Tipo específico para almuerzos
        USER_PROFILE(USER_PROFILE_CACHE_DURATION),
        NOTIFICATIONS(NOTIFICATIONS_CACHE_DURATION)
    }
    
    /**
     * Política de invalidación de cache
     */
    enum class InvalidationPolicy {
        TIME_BASED,      // Basado en tiempo de expiración
        MANUAL,          // Invalidación manual
        ON_DATA_CHANGE,  // Cuando detecta cambios en los datos
        HYBRID           // Combinación de tiempo y cambios
    }
}