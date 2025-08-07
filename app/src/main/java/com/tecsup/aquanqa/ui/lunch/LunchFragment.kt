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
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    override fun onResume() {
        super.onResume()
        // ❌ REMOVIDO: No llamar automáticamente onAppResumed en cada navegación
        // Solo debería llamarse cuando la app realmente vuelve del background
    }

    override fun setupObservers() {
        super.setupObservers()
        
        // ✅ PRIMERO: Observar estado de UI (para configurar la vista correctamente)
        lunchViewModel.uiState.observe(viewLifecycleOwner) { state ->
            handleUiState(state)
        }
        
        // ✅ SEGUNDO: Observar lista de almuerzos (para mostrar los datos)
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
                hideLoading()
                hideEmptyState()
                binding.rvLunchMenu.visibility = View.VISIBLE
            }
            is LunchViewModel.LunchUiState.Loading -> {
                showLoading()
                hideEmptyState()
            }
            is LunchViewModel.LunchUiState.Success -> {
                hideLoading()
                hideEmptyState()
                binding.rvLunchMenu.visibility = View.VISIBLE
            }
            is LunchViewModel.LunchUiState.Empty -> {
                hideLoading()
                showEmptyState()
            }
            is LunchViewModel.LunchUiState.Error -> {
                hideLoading()
                hideEmptyState()
                showLunchError(state.message)
            }
        }
    }

    private fun showLoading() {
        binding.swipeRefreshLayout.isRefreshing = true
        binding.rvLunchMenu.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.swipeRefreshLayout.isRefreshing = false
        binding.rvLunchMenu.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        binding.rvLunchMenu.visibility = View.GONE
        showToast("No hay almuerzos disponibles en este momento", Toast.LENGTH_LONG)
    }

    private fun hideEmptyState() {
        binding.rvLunchMenu.visibility = View.VISIBLE
    }

    private fun showLunchError(message: String) {
        binding.rvLunchMenu.visibility = View.VISIBLE
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Reintentar") {
                lunchViewModel.refreshAlmuerzos()
            }
            .show()
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