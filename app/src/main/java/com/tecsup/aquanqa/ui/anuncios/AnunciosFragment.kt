package com.tecsup.aquanqa.ui.anuncios

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.tecsup.aquanqa.R
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
        
        // ✅ PRIMERO: Observar estado de carga (siguiendo patrón ProfileFragment)
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            handleLoadingState(isLoading)
        }
        
        // ✅ SEGUNDO: Observar datos de anuncios
        viewModel.anuncios.observe(viewLifecycleOwner) { anuncios ->
            adapter.updateData(anuncios)
            binding.swipeRefreshLayout.isRefreshing = false
        }
        
        // ✅ TERCERO: Observar errores
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                handleAnunciosError(errorMsg)
            }
        }
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
            R.color.aquanqa_blue ,
            R.color.success,
            R.color.warning_color,
            R.color.error_color
        )
    }

    override fun onResume() {
        super.onResume()
        // ✅ Solo recargar si es necesario (evitar llamadas innecesarias, patrón ProfileFragment)
        viewModel.onAppResumed()
    }
    
    /**
     * ✅ Maneja el estado de carga de manera centralizada (patrón ProfileFragment)
     */
    private fun handleLoadingState(isLoading: Boolean) {
        if (!isLoading) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }
    
    /**
     * ✅ Maneja errores específicos de anuncios sin conflicto con BaseFragment (patrón ProfileFragment)
     */
    private fun handleAnunciosError(message: String) {
        binding.swipeRefreshLayout.isRefreshing = false
        com.google.android.material.snackbar.Snackbar.make(binding.root, message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            .setAction("Reintentar") { 
                viewModel.refreshAnuncios() 
            }
            .show()
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