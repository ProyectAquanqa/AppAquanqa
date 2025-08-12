package com.tecsup.aquanqa.ui.lunch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.tecsup.aquanqa.data.repository.LunchRepository
import com.tecsup.aquanqa.databinding.FragmentLunchBinding
import com.tecsup.aquanqa.ui.adapters.LunchAdapter
import com.tecsup.aquanqa.ui.base.BaseFragment
import com.google.android.material.snackbar.Snackbar
import com.tecsup.aquanqa.R

/**
 * Fragment optimizado para mostrar menús de almuerzo.
 * 
 * Utiliza estados unificados y manejo de errores user-friendly.
 */
class LunchFragment : BaseFragment<FragmentLunchBinding>() {

    // ViewModel con inyección de dependencias
    private val lunchViewModel: LunchViewModel by viewModels {
        val apiClient = ApiClient.getClient(requireContext())
        val userPreferences = UserPreferences(requireContext())
        val repository = LunchRepository(apiClient.apiService, userPreferences)
        LunchViewModelFactory(repository)
    }
    
    private lateinit var lunchAdapter: LunchAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLunchBinding {
        return FragmentLunchBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        super.setupUI()
        setupRecyclerView()
        setupSwipeRefresh()
        
        // Configurar botón de reintentar
        binding.btnRetry.setOnClickListener {
            lunchViewModel.refreshAlmuerzos()
        }
    }

    private fun setupRecyclerView() {
        lunchAdapter = LunchAdapter { almuerzo ->
            // Analytics o logging adicional si es necesario
        }
        
        binding.rvLunchMenu.apply {
            adapter = lunchAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            lunchViewModel.refreshAlmuerzos()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.aquanqa_blue,
            R.color.success,
            R.color.warning_color,
            R.color.error_color
        )
    }

    override fun onResume() {
        super.onResume()
        // ❌ REMOVIDO: No llamar automáticamente onAppResumed en cada navegación
        // Solo debería llamarse cuando la app realmente vuelve del background
    }

    override fun setupObservers() {
        super.setupObservers()
        
        //  PRIMERO: Observar estado de UI (para configurar la vista correctamente)
        lunchViewModel.uiState.observe(viewLifecycleOwner) { state ->
            handleUiState(state)
        }
        
        //  SEGUNDO: Observar lista de almuerzos (para mostrar los datos)
        lunchViewModel.almuerzos.observe(viewLifecycleOwner) { almuerzos ->
            // Forzar actualización creando una nueva lista para evitar problemas con DiffUtil
            val nuevaLista = almuerzos.toList()
            lunchAdapter.submitList(nuevaLista) {
                if (nuevaLista.isNotEmpty()) {
                    binding.rvLunchMenu.visibility = View.VISIBLE
                    binding.rvLunchMenu.scrollToPosition(0)
                }
            }
        }
    }

    /**
     * Maneja todos los estados de UI de manera centralizada y clara.
     */
    private fun handleUiState(state: LunchViewModel.LunchUiState) {
        when (state) {
            is LunchViewModel.LunchUiState.Idle -> {
                showContent()
            }
            is LunchViewModel.LunchUiState.Loading -> {
                showLoading()
            }
            is LunchViewModel.LunchUiState.Success -> {
                showContent()
            }
            is LunchViewModel.LunchUiState.Empty -> {
                showEmptyState()
            }
            is LunchViewModel.LunchUiState.Error -> {
                showErrorState(state.message)
            }
        }
    }

    /**
     * Muestra el estado de carga inicial
     */
    private fun showLoading() {
        binding.apply {
            progressBar.visibility = View.VISIBLE
            swipeRefreshLayout.visibility = View.GONE
            emptyState.visibility = View.GONE
            errorState.visibility = View.GONE
        }
    }

    /**
     * Muestra el contenido con datos
     */
    private fun showContent() {
        binding.apply {
            progressBar.visibility = View.GONE
            swipeRefreshLayout.visibility = View.VISIBLE
            swipeRefreshLayout.isRefreshing = false
            emptyState.visibility = View.GONE
            errorState.visibility = View.GONE
        }
    }

    /**
     * Muestra el estado vacío cuando no hay datos
     */
    private fun showEmptyState() {
        binding.apply {
            progressBar.visibility = View.GONE
            swipeRefreshLayout.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            errorState.visibility = View.GONE
        }
    }

    /**
     * Muestra el estado de error con botón reintentar
     */
    private fun showErrorState(errorMessage: String) {
        binding.apply {
            progressBar.visibility = View.GONE
            swipeRefreshLayout.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false
            emptyState.visibility = View.GONE
            errorState.visibility = View.VISIBLE
            tvErrorMessage.text = errorMessage
        }
    }

    /**
     * Método público para detectar nuevos almuerzos.
     */
    fun detectNewLunches() {
        lunchViewModel.checkForNewContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpieza automática del ViewBinding
    }
}