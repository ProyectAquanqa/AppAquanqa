package com.tecsup.aquanqa.data.model

/**
 * Modelo genérico para respuestas paginadas de la API.
 * 
 * Este modelo maneja la estructura de respuesta estándar de Django
 * para endpoints paginados, incluyendo información sobre la paginación y los resultados.
 * @param T Tipo de datos contenidos en la lista de resultados
 */
data class PaginatedResponse<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)

//Información de paginación extraída de una respuesta paginada. Útil para manejar el estado de carga en infinite scroll.

data class PaginationInfo(
    val count: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    val currentPage: Int = 1
) {
    companion object {
        fun from(response: PaginatedResponse<*>): PaginationInfo {
            return PaginationInfo(
                count = response.count,
                hasNext = response.next != null,
                hasPrevious = response.previous != null
            )
        }
    }
} 