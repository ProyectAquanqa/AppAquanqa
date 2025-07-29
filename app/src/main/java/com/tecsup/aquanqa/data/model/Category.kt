package com.tecsup.aquanqa.data.model

/**
 * Modelo de datos que representa una categoría de eventos.
 * 
 * Este modelo corresponde a la estructura de datos del backend para las categorías
 * que permiten clasificar y filtrar los eventos de la aplicación.
 * 
 * Los nombres de los campos están en snake_case para coincidir directamente
 * con las claves del JSON devuelto por la API de Django.
 */
data class Category(
    /**
     * Identificador único de la categoría en la base de datos.
     */
    val id: Int,
    
    /**
     * Nombre descriptivo de la categoría (ej: "Anuncios", "Charlas", "Talleres").
     * Este campo es único en el backend.
     */
    val nombre: String,
    
    /**
     * Descripción opcional que proporciona más detalles sobre la categoría.
     * Puede ser null si no se proporciona descripción.
     */
    val descripcion: String?,
    
    /**
     * Fecha y hora de creación de la categoría en formato ISO 8601.
     * Proporcionado por el sistema de auditoría del backend.
     */
    val created_at: String?,
    
    /**
     * Fecha y hora de la última actualización en formato ISO 8601.
     * Proporcionado por el sistema de auditoría del backend.
     */
    val updated_at: String?
) {
    
    /**
     * Compañero objeto que contiene constantes útiles para el manejo de categorías.
     */
    companion object {
        /**
         * Identificador especial para la categoría "Todos" que representa
         * la opción de mostrar eventos de todas las categorías sin filtro.
         */
        const val ALL_CATEGORIES_ID = -1
        
        /**
         * Nombre de la categoría especial "Todos".
         */
        const val ALL_CATEGORIES_NAME = "Todos"
        
        /**
         * Crea una instancia especial de Category que representa la opción "Todos".
         * Esta categoría no existe en el backend pero es útil para la UI.
         * 
         * @return Category Instancia especial para mostrar todos los eventos
         */
        fun createAllCategoriesOption(): Category {
            return Category(
                id = ALL_CATEGORIES_ID,
                nombre = ALL_CATEGORIES_NAME,
                descripcion = "Ver todos los eventos sin filtro de categoría",
                created_at = null,
                updated_at = null
            )
        }
    }
    
    /**
     * Verifica si esta categoría es la opción especial "Todos".
     * 
     * @return Boolean true si es la categoría "Todos", false en caso contrario
     */
    fun isAllCategoriesOption(): Boolean {
        return id == ALL_CATEGORIES_ID
    }
    
    /**
     * Obtiene un nombre de visualización amigable para la UI.
     * 
     * @return String Nombre formateado para mostrar en la interfaz
     */
    fun getDisplayName(): String {
        return nombre.trim().replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase() else it.toString() 
        }
    }
} 