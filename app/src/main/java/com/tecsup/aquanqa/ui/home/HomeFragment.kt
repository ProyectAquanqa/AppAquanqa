package com.tecsup.aquanqa.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.model.content.Anuncio
import com.tecsup.aquanqa.data.model.content.Category
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.tecsup.aquanqa.databinding.FragmentHomeBinding
import com.tecsup.aquanqa.ui.anuncios.AnunciosAdapterWrapper
import com.tecsup.aquanqa.ui.anuncios.createAnunciosAdapter
import com.tecsup.aquanqa.ui.base.BaseFragment
import com.tecsup.aquanqa.utils.DateUtils
import com.tecsup.aquanqa.utils.InfiniteScrollListener
import com.tecsup.aquanqa.ui.anuncios.CommentsBottomSheetFragment
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Fragment optimizado para Home con cache híbrido inteligente.
 * Sigue el mismo patrón exitoso del ProfileFragment para consistencia.
 */
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var eventsAdapter: AnunciosAdapterWrapper
    private lateinit var infiniteScrollListener: InfiniteScrollListener

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Inicialización optimizada y secuencial
        initializeViewModelOptimized()
        setupRecyclerViewsOptimized()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        //  Solo recargar si es necesario (evitar llamadas innecesarias, patrón ProfileFragment)
        if (::viewModel.isInitialized) {
            viewModel.onAppResumed()
        }
    }

    /**
     * Inicialización optimizada del ViewModel con cache inteligente
     */
    private fun initializeViewModelOptimized() {
        // Reutilizar instancias para mejor performance
        val context = requireContext().applicationContext
        val userPreferences = UserPreferences(context)
        val apiClient = ApiClient.getClient(context)
        val repository = com.tecsup.aquanqa.data.repository.HomeRepository(
            apiClient.apiService, 
            userPreferences
        )
        
        viewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(repository)
        )[HomeViewModel::class.java]
    }

    override fun setupUI() {
        super.setupUI()
        // Configuración inicial rápida de UI
        setupInitialUI()
        setupSwipeRefresh()
    }
    
    /**
     * Configuración inicial optimizada de la UI
     */
    private fun setupInitialUI() {
        // Mostrar fecha inmediatamente
        binding.tvDate.text = viewModel.currentDateSpanish
        
        // Preconfigurar elementos para evitar redraws
        binding.tvUserName.text = "¡Hola, Usuario!"
    }

    /**
     * Configuración optimizada de RecyclerViews con mejor performance
     */
    private fun setupRecyclerViewsOptimized() {
        // Configurar adapter de categorías con mejor performance
        categoryAdapter = CategoryAdapter { category ->
            viewModel.onCategorySelected(category)
        }
        
        binding.rvCategories.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            // Optimizaciones para mejor scroll
            isNestedScrollingEnabled = false
            itemAnimator = null // Eliminar animaciones para mayor velocidad
        }
        
        // Configurar adapter de eventos con lazy loading, likes y comentarios
        eventsAdapter = createAnunciosAdapter(
            lifecycleScope = lifecycleScope,
            onItemClick = { anuncio ->
                // TODO: Navigate to event detail
                android.util.Log.d("HomeFragment", "Event clicked: ${anuncio.titulo}")
            },
            onCommentClick = { anuncio ->
                showCommentsModal(anuncio)
            }
        )
        
        val layoutManager = LinearLayoutManager(requireContext())
        
        // Configurar InfiniteScrollListener
        infiniteScrollListener = InfiniteScrollListener(
            layoutManager = layoutManager,
            visibleThreshold = 5
        ) {
            // Callback para cargar más datos
            if (viewModel.canLoadMore()) {
                viewModel.loadMoreEvents()
            }
        }
        
        binding.rvPublications.apply {
            adapter = eventsAdapter.getAdapter()
            this.layoutManager = layoutManager
            setHasFixedSize(false) // Permitir altura dinámica para contenido variable
            
            // Optimizaciones de memoria y rendimiento
            setItemViewCacheSize(10) // Reducir cache para evitar uso excesivo de memoria
            recycledViewPool.setMaxRecycledViews(0, 20) // Pool más grande para mejor reciclaje
            
            // Optimizaciones de drawing (DEPRECATED - remover para mejor rendimiento)
            // setDrawingCacheEnabled(false) // Desactivado por defecto desde API 28
            
            // Optimización de scroll suave
            isNestedScrollingEnabled = true
            
            // Prefetch para mejor scroll
            (layoutManager as? LinearLayoutManager)?.isItemPrefetchEnabled = true
            (layoutManager as? LinearLayoutManager)?.initialPrefetchItemCount = 4
            
            // Agregar el scroll listener para infinite scroll
            addOnScrollListener(infiniteScrollListener)
        }
    }



    override fun setupObservers() {
        super.setupObservers()
        
        //  PRIMERO: Observar estado de UI global (siguiendo patrón ProfileFragment)
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            handleLoadingState(isLoading)
        }
        
        //  Observar estado de carga de más elementos
        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoadingMore ->
            // El estado de loading more se puede mostrar en el último item del adapter si es necesario
        }
        
        //  SEGUNDO: Observar datos del usuario
        viewModel.userFirstName.observe(viewLifecycleOwner) { firstName ->
            binding.tvUserName.text = "¡Hola, $firstName!"
            binding.tvDate.text = viewModel.currentDateSpanish
        }
        
        //  TERCERO: Observar datos de categorías con manejo inteligente
        viewModel.categoriesState.observe(viewLifecycleOwner) { result ->
            handleCategoriesState(result)
        }
        
        //  CUARTO: Observar datos de eventos con manejo inteligente
        viewModel.eventsState.observe(viewLifecycleOwner) { result ->
            handleEventsState(result)
            
            // Precarga inteligente de imágenes cuando se cargan eventos exitosamente
            if (result is Result.Success) {
                preloadImagesInBackground(result.data)
            }
        }
        
        //  QUINTO: Observar categoría seleccionada
        lifecycleScope.launch {
            viewModel.selectedCategory.collect { category ->
                category?.let { 
                    categoryAdapter.setSelectedCategory(it.id)
                    // Resetear scroll listener cuando cambia la categoría
                    if (::infiniteScrollListener.isInitialized) {
                        infiniteScrollListener.resetState()
                    }
                }
            }
        }
    }
    
    /**
     *  Maneja el estado de carga global de manera centralizada (patrón ProfileFragment)
     */
    private fun handleLoadingState(isLoading: Boolean) {
        binding.swipeRefreshLayout.isRefreshing = isLoading
    }
    
    /**
     *  Maneja todos los estados de categorías de manera centralizada y clara
     */
    private fun handleCategoriesState(result: Result<List<Category>>) {
        when (result) {
            is Result.Success -> {
                categoryAdapter.submitList(result.data)
                binding.swipeRefreshLayout.isRefreshing = false
            }
            is Result.Error -> {
                binding.swipeRefreshLayout.isRefreshing = false
                showHomeError("No se pudieron cargar las categorías", "Reintentar categorías") {
                    viewModel.refreshData()
                }
            }
            is Result.Loading -> {
                // El loading se maneja en handleLoadingState
            }

            else -> {Log.w("Categorias", "Error de conexion")}
        }
    }
    
    /**
     *  Maneja todos los estados de eventos de manera centralizada y clara
     */
    private fun handleEventsState(result: Result<List<Anuncio>>) {
        when (result) {
            is Result.Success -> {
                eventsAdapter.submitList(result.data)
                binding.swipeRefreshLayout.isRefreshing = false
            }
            is Result.Error -> {
                binding.swipeRefreshLayout.isRefreshing = false
                showHomeError("No se pudieron cargar los eventos", "Reintentar eventos") {
                    viewModel.refreshData()
                }
            }
            is Result.Loading -> {
                // El loading se maneja en handleLoadingState
            }

            else -> {Log.w("Eventos", "Error de conexiónn")}
        }
    }
    
    /**
     *  Muestra errores específicos del home sin conflicto con BaseFragment (patrón ProfileFragment)
     */
    private fun showHomeError(message: String, actionText: String, action: () -> Unit) {
        com.google.android.material.snackbar.Snackbar.make(binding.root, message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            .setAction(actionText) { action() }
            .show()
    }

    /**
     *  Refresca datos con cache inteligente (patrón ProfileFragment)
     */
    fun refreshData(forceRefresh: Boolean = true) {
        viewModel.refreshData(forceRefresh)
    }
    
    /**
     * Configura el SwipeRefreshLayout para pull-to-refresh manual.
     * Permite al usuario refrescar eventos y categorías deslizando hacia abajo.
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Refrescar datos con indicador visual
            refreshData(forceRefresh = true)
        }
        
        // Personalizar colores del indicador de refresh
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.aquanqa_blue,
            R.color.success,
            R.color.warning_color,
            R.color.error_color
        )
    }

    /**
     * Obtiene estadísticas del cache para debugging
     */
    fun getCacheStats() = viewModel.getCacheStats()
    
    /**
     * Invalida cache específico cuando sea necesario
     */
    fun invalidateCache(categoryName: String? = null) {
        viewModel.invalidateCache(categoryName)
    }
    
    /**
     * Método público para detectar nuevos eventos.
     * Puede ser llamado desde otros fragments o activities.
     */
    fun detectNewEvents() {
        if (::viewModel.isInitialized) {
            viewModel.checkForNewEvents()
        }
    }
    
    /**
     * Muestra el modal de comentarios para un evento específico.
     */
    private fun showCommentsModal(anuncio: Anuncio) {
        val commentsBottomSheet = CommentsBottomSheetFragment.newInstance(anuncio)
        commentsBottomSheet.show(parentFragmentManager, "CommentsBottomSheet")
    }
    
    /**
     * Precarga imágenes en segundo plano para mejorar rendimiento.
     * Solo precarga las primeras 5 imágenes para evitar uso excesivo de memoria.
     */
    private fun preloadImagesInBackground(anuncios: List<Anuncio>) {
        lifecycleScope.launch {
            try {
                // Obtener URLs de imágenes de los primeros 5 anuncios
                val imageUrls = anuncios.take(5)
                    .mapNotNull { it.imagen }
                    .filter { it.isNotBlank() }
                
                // Precarga con ImageLoadingUtils optimizado
                if (imageUrls.isNotEmpty()) {
                    com.tecsup.aquanqa.utils.ImageLoadingUtils.preloadImages(
                        context = requireContext(),
                        imageUrls = imageUrls
                    )
                }
            } catch (e: Exception) {
                // Silenciosamente manejar errores de precarga - no afecta funcionalidad principal
                android.util.Log.d("HomeFragment", "Preload images failed: ${e.message}")
            }
        }
    }
    
    /**
     * Optimización de memoria - limpiar cache de imágenes cuando sea necesario
     */
    override fun onDestroy() {
        super.onDestroy()
        // Limpiar callbacks de comentarios para evitar memory leaks
        com.tecsup.aquanqa.utils.CommentManager.clearAllCallbacks()
        
        // Limpiar cache de memoria de imágenes para liberar recursos
        try {
            com.tecsup.aquanqa.utils.ImageLoadingUtils.clearMemoryCache(requireContext())
        } catch (e: Exception) {
            // Ignorar errores de limpieza
        }
    }
}