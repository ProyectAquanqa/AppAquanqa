package com.tecsup.aquanqa.ui.anuncios

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.tecsup.aquanqa.databinding.FragmentAnunciosBinding
import com.tecsup.aquanqa.ui.base.BaseFragment

/**
 * Fragment refactorizado para anuncios con cache híbrido inteligente.
 * Ahora con persistencia que sobrevive al cierre de la app + imagen por defecto.
 */
class AnunciosFragment : BaseFragment<FragmentAnunciosBinding>() {

    // ViewModel con cache híbrido
    private val viewModel: AnunciosViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[AnunciosViewModel::class.java]
    }

    private lateinit var adapter: AnunciosAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAnunciosBinding {
        return FragmentAnunciosBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        super.setupUI()
        setupRecyclerView()
        setupSwipeRefresh()
    }

    override fun setupObservers() {
        super.setupObservers()
        observeViewModel()
    }

    /**
     * Inicializa el RecyclerView con un adapter vacío.
     */
    private fun setupRecyclerView() {
        adapter = AnunciosAdapter(emptyList())
        binding.rvAnuncios.adapter = adapter
    }
    
    /**
     * Configura el SwipeRefreshLayout para pull-to-refresh.
     * Permite al usuario refrescar anuncios deslizando hacia abajo.
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Refrescar anuncios con fuerza
            viewModel.refreshAnuncios()
        }
        
        // Personalizar colores del indicador de refresh
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    override fun onResume() {
        super.onResume()
        // SIEMPRE intentar refresh para detectar contenido nuevo
        viewModel.refreshAnuncios()
    }

    /**
     * Configura los observadores con cache híbrido inteligente.
     * Ahora los anuncios persisten al cerrar la app.
     */
    private fun observeViewModel() {
        viewModel.anuncios.observe(viewLifecycleOwner) { anuncios ->
            // Actualizar adapter con anuncios (con imagen por defecto si no hay internet)
            adapter.updateData(anuncios)
            // Ocultar SwipeRefreshLayout cuando se cargan los datos
            binding.swipeRefreshLayout.isRefreshing = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Ocultar SwipeRefreshLayout cuando termine la carga
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                showError(errorMsg)
                // Ocultar SwipeRefreshLayout también en caso de error
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    /**
     * Refresca anuncios con cache híbrido.
     */
    fun refreshAnuncios() {
        viewModel.refreshAnuncios()
    }

    /**
     * Limpia cache y recarga (para debugging).
     */
    fun clearCacheAndReload() {
        viewModel.clearCacheAndReload()
    }
} 