package com.tecsup.aquanqa.ui.anuncios

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.databinding.FragmentAnunciosBinding
import com.tecsup.aquanqa.ui.base.BaseFragment
import com.tecsup.aquanqa.utils.InfiniteScrollListener

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
    private lateinit var infiniteScrollListener: InfiniteScrollListener

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAnunciosBinding {
        return FragmentAnunciosBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        super.setupUI()
        setupRecyclerView()
        setupSwipeRefresh()
        
        // Configurar botón de reintentar
        binding.btnRetry.setOnClickListener {
            viewModel.refreshAnuncios()
        }
    }

    override fun setupObservers() {
        super.setupObservers()
        
        //  PRIMERO: Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showLoading()
            }
        }
        
        //  Observar estado de carga de más elementos
        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoadingMore ->
            // El estado de loading more se puede mostrar en el último item del adapter si es necesario
        }
        
        //  SEGUNDO: Observar datos de anuncios
        viewModel.anuncios.observe(viewLifecycleOwner) { anuncios ->
            if (anuncios.isEmpty()) {
                showEmptyState()
            } else {
                showContent()
                adapter.updateData(anuncios)
            }
        }
        
        //  TERCERO: Observar errores
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                showErrorState(errorMsg)
            }
        }
    }

    /**
     * Inicializa el RecyclerView con un adapter vacío y configura infinite scroll.
     */
    private fun setupRecyclerView() {
        adapter = AnunciosAdapter(emptyList())
        
        val layoutManager = LinearLayoutManager(requireContext())
        
        // Configurar InfiniteScrollListener
        infiniteScrollListener = InfiniteScrollListener(
            layoutManager = layoutManager,
            visibleThreshold = 5
        ) {
            // Callback para cargar más datos
            if (viewModel.canLoadMore()) {
                viewModel.loadMoreAnuncios()
            }
        }
        
        binding.rvAnuncios.apply {
            adapter = this@AnunciosFragment.adapter
            this.layoutManager = layoutManager
            setHasFixedSize(true)
            
            // Agregar el scroll listener para infinite scroll
            addOnScrollListener(infiniteScrollListener)
        }
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
        //  Solo recargar si es necesario (evitar llamadas innecesarias, patrón ProfileFragment)
        viewModel.onAppResumed()
    }
    
    /**
     * Muestra el estado de carga inicial
     */
    private fun showLoading() {
        binding.apply {
            progressBar.visibility = android.view.View.VISIBLE
            swipeRefreshLayout.visibility = android.view.View.GONE
            emptyState.visibility = android.view.View.GONE
            errorState.visibility = android.view.View.GONE
        }
    }

    /**
     * Muestra el contenido con datos
     */
    private fun showContent() {
        binding.apply {
            progressBar.visibility = android.view.View.GONE
            swipeRefreshLayout.visibility = android.view.View.VISIBLE
            swipeRefreshLayout.isRefreshing = false
            emptyState.visibility = android.view.View.GONE
            errorState.visibility = android.view.View.GONE
        }
    }

    /**
     * Muestra el estado vacío cuando no hay datos
     */
    private fun showEmptyState() {
        binding.apply {
            progressBar.visibility = android.view.View.GONE
            swipeRefreshLayout.visibility = android.view.View.GONE
            emptyState.visibility = android.view.View.VISIBLE
            errorState.visibility = android.view.View.GONE
        }
    }

    /**
     * Muestra el estado de error con botón reintentar
     */
    private fun showErrorState(errorMessage: String) {
        binding.apply {
            progressBar.visibility = android.view.View.GONE
            swipeRefreshLayout.visibility = android.view.View.GONE
            swipeRefreshLayout.isRefreshing = false
            emptyState.visibility = android.view.View.GONE
            errorState.visibility = android.view.View.VISIBLE
            tvErrorMessage.text = errorMessage
        }
    }

    /**
     * Refresca anuncios con cache híbrido.
     */
    fun refreshAnuncios() {
        viewModel.refreshAnuncios()
    }

    /**
     * Limpia cache y recarga
     */
    fun clearCacheAndReload() {
        viewModel.clearCacheAndReload()
    }



} 