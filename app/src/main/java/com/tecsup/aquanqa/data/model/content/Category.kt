package com.tecsup.aquanqa.data.model.content

/**
 * Modelo de datos que representa una categoría de eventos.
 * Los nombres de los campos están en snake_case para coincidir directamente
 * con las claves del JSON devuelto por la API de Django.
 */
data class Category(

    val id: Int,
    val nombre: String,
    val descripcion: String?,
    val created_at: String?,
    val updated_at: String?
) {
    
    /**
     * Compañero objeto que contiene constantes útiles para el manejo de categorías.
     */
    companion object {
        // Identificador especial para la categoría "Todos" que representa
        const val ALL_CATEGORIES_ID = -1
        const val ALL_CATEGORIES_NAME = "Todos" //nombre
        
        //Crea una instancia especial de Category que representa la opción "Todos".

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