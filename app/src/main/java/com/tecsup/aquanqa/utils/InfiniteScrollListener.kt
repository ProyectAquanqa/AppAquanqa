package com.tecsup.aquanqa.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView.OnScrollListener personalizado para implementar infinite scroll.
 * 
 * Esta clase detecta automáticamente cuando el usuario se acerca al final de la lista
 * y dispara un callback para cargar más elementos. Es reutilizable y configurable.
 * 
 * @param layoutManager El LinearLayoutManager del RecyclerView
 * @param visibleThreshold Número de elementos restantes que deben ser visibles antes de cargar más
 * @param onLoadMore Callback que se ejecuta cuando se necesita cargar más elementos
 */
class InfiniteScrollListener(
    private val layoutManager: LinearLayoutManager,
    private val visibleThreshold: Int = 5,
    private val onLoadMore: () -> Unit
) : RecyclerView.OnScrollListener() {

    private var loading = false
    private var previousTotalItemCount = 0

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val totalItemCount = layoutManager.itemCount
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

        // Si el total de elementos cambió, ya no estamos cargando
        if (totalItemCount < previousTotalItemCount) {
            this.previousTotalItemCount = totalItemCount
            if (totalItemCount == 0) {
                this.loading = true
            }
        }

        // Si estaba cargando y el total de elementos cambió, ya terminó de cargar
        if (loading && (totalItemCount > previousTotalItemCount)) {
            loading = false
            previousTotalItemCount = totalItemCount
        }

        // Si no está cargando y estamos cerca del final, cargar más
        if (!loading && (lastVisibleItemPosition + visibleThreshold) > totalItemCount) {
            onLoadMore()
            loading = true
        }
    }

    /**
     * Resetea el estado del scroll listener cuando se recarga la lista completa.
     */
    fun resetState() {
        this.loading = false
        this.previousTotalItemCount = 0
    }
} 