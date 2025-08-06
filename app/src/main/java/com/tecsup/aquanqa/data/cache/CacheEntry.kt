package com.tecsup.aquanqa.data.cache

/**
 * Representa una entrada de cache con metadata completa
 * para manejo inteligente de invalidación y actualización.
 */
data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis(),
    val source: DataSource = DataSource.API,
    val userId: String? = null,
    val etag: String? = null,
    val lastModified: String? = null,
    val version: Int = 1,
    val metadata: Map<String, Any> = emptyMap()
) {
    
    /**
     * Verifica si la entrada de cache ha expirado
     */
    fun isExpired(cacheDuration: Long): Boolean {
        return (System.currentTimeMillis() - timestamp) > cacheDuration
    }
    
    /**
     * Verifica si la entrada es válida para un usuario específico
     */
    fun isValidForUser(currentUserId: String?): Boolean {
        return userId == null || userId == currentUserId
    }
    
    /**
     * Crea una copia actualizada de la entrada con nuevos datos
     */
    fun updateData(newData: T, newSource: DataSource = DataSource.API): CacheEntry<T> {
        return copy(
            data = newData,
            timestamp = System.currentTimeMillis(),
            source = newSource,
            version = version + 1
        )
    }
    
    /**
     * Obtiene información de debug sobre la entrada
     */
    fun getDebugInfo(): String {
        val age = (System.currentTimeMillis() - timestamp) / 1000
        return "CacheEntry(age=${age}s, source=$source, version=$version, userId=$userId)"
    }
}

/**
 * Enum que indica la fuente de los datos
 */
enum class DataSource {
    CACHE,      // Datos del cache local
    API,        // Datos frescos de la API
    FALLBACK    // Datos de respaldo (cache expirado pero usado por error de red)
}