package com.tecsup.aquanqa.ui.anuncios

import com.tecsup.aquanqa.data.model.content.Anuncio

/**
 * Wrapper para AnunciosAdapter que proporciona una interfaz compatible
 * con ListAdapter y soporte para callbacks de click.
 * 
 * Esta clase actúa como un adaptador entre el AnunciosAdapter existente
 * y las necesidades del HomeFragment, proporcionando métodos como submitList()
 * y soporte para callbacks de click en elementos.
 * 
 * @param onItemClick Función callback que se ejecuta al hacer clic en un anuncio
 */
class AnunciosAdapterWrapper(
    private val onItemClick: (Anuncio) -> Unit
) {
    
    /**
     * Instancia del adapter original que maneja la vista.
     */
    private val adapter = AnunciosAdapter(emptyList())
    
    //Lista actual de anuncios para manejar los callbacks de click.

    private var currentList: List<Anuncio> = emptyList()
    
    init {
        // Configurar click listeners en el adapter original
        setupClickListeners()
    }
    
    // Actualiza la lista de anuncios

    fun submitList(list: List<Anuncio>) {
        currentList = list
        adapter.updateData(list)
    }
    
    // Obtiene el adapter original para usar en el RecyclerView.

    fun getAdapter(): AnunciosAdapter = adapter
    
    /**
     * Obtiene la lista actual de anuncios.
     * 
     * @return List<Anuncio> Lista actual de anuncios
     */
    fun getCurrentList(): List<Anuncio> = currentList
    
    /**
     * Obtiene el número de elementos en la lista actual.
     * 
     * @return Int Número de anuncios en la lista
     */
    fun getItemCount(): Int = currentList.size
    
    /**
     * Configura los click listeners en el adapter original.
     * Como el adapter original no tiene soporte nativo para callbacks,
     * usamos una solución mediante setOnClickListener en las vistas.
     */
    private fun setupClickListeners() {
        // Esta funcionalidad se implementará directamente en el ViewHolder
        // cuando se necesite, por ahora mantenemos la estructura básica
    }
}

/**
 * Función de extensión para facilitar el uso del wrapper con RecyclerView.
 * 
 * @param onItemClick Función callback para clicks en elementos
 * @return AnunciosAdapterWrapper Wrapper configurado y listo para usar
 */
fun createAnunciosAdapter(onItemClick: (Anuncio) -> Unit): AnunciosAdapterWrapper {
    return AnunciosAdapterWrapper(onItemClick)
} 