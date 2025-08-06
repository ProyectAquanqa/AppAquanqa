package com.tecsup.aquanqa.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var eventsAdapter: AnunciosAdapterWrapper
    
    // Variables para retry automático
    private var categoriesRetryCount = 0
    private var eventsRetryCount = 0
    private val maxRetries = 3
    private val baseDelayMs = 1000L

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
        // SIEMPRE intentar refresh para detectar contenido nuevo
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
        
        // Configurar adapter de eventos con lazy loading
        eventsAdapter = createAnunciosAdapter { anuncio ->
            // TODO: Handle event click
        }
        
        binding.rvPublications.apply {
            adapter = eventsAdapter.getAdapter()
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false) // Permitir altura dinámica
            // Optimizaciones de memoria y scroll
            setItemViewCacheSize(20)
            setDrawingCacheEnabled(true)
            setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH)
        }
    }



    override fun setupObservers() {
        // Observer optimizado del nombre de usuario
        viewModel.userFirstName.observe(viewLifecycleOwner) { firstName ->
            binding.tvUserName.text = "¡Hola, $firstName!"
            binding.tvDate.text = viewModel.currentDateSpanish
        }
        
        // Observer de estados de carga global
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Ocultar indicador de pull-to-refresh cuando termine la carga
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        
        // Observer mejorado de categorías con retry automático
        viewModel.categoriesState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    categoriesRetryCount = 0 // Reset contador en éxito
                    categoryAdapter.submitList(result.data)
                    // Ocultar SwipeRefreshLayout cuando las categorías se cargan exitosamente
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                is Result.Error -> {
                    handleCategoriesError(result.exception)
                    // Ocultar SwipeRefreshLayout también en caso de error
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                is Result.Loading -> {
                    // Mostrar indicador de carga si es necesario
                }
            }
        }
        
        // Observer mejorado de eventos con retry automático
        viewModel.eventsState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    eventsRetryCount = 0 // Reset contador en éxito
                    eventsAdapter.submitList(result.data)
                    // Ocultar SwipeRefreshLayout cuando los eventos se cargan exitosamente
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                is Result.Error -> {
                    handleEventsError(result.exception)
                    // Ocultar SwipeRefreshLayout también en caso de error
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                is Result.Loading -> {
                    // Mostrar indicador de carga si es necesario
                }
            }
        }
        
        // Observer de categoría seleccionada
        lifecycleScope.launch {
            viewModel.selectedCategory.collect { category ->
                category?.let { 
                    categoryAdapter.setSelectedCategory(it.id)
                }
            }
        }
    }
    
    /**
     * Maneja errores de categorías con retry automático inteligente
     */
    private fun handleCategoriesError(exception: Exception) {
        if (categoriesRetryCount < maxRetries) {
            categoriesRetryCount++
            val delay = baseDelayMs * categoriesRetryCount
            
            lifecycleScope.launch {
                delay(delay)
                viewModel.refreshData()
            }
        } else {
            showUserFriendlyError("No se pudieron cargar las categorías. Verifica tu conexión.")
        }
    }
    
    /**
     * Maneja errores de eventos con retry automático inteligente
     */
    private fun handleEventsError(exception: Exception) {
        if (eventsRetryCount < maxRetries) {
            eventsRetryCount++
            val delay = baseDelayMs * eventsRetryCount
            
            lifecycleScope.launch {
                delay(delay)
                viewModel.refreshData()
            }
        } else {
            showUserFriendlyError("No se pudieron cargar los eventos. Verifica tu conexión.")
        }
    }
    
    /**
     * Muestra mensajes de error más amigables al usuario
     */
    private fun showUserFriendlyError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    /**
     * Refresca datos con reset de contadores de retry y cache inteligente
     */
    fun refreshData(forceRefresh: Boolean = true) {
        categoriesRetryCount = 0
        eventsRetryCount = 0
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
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
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
}