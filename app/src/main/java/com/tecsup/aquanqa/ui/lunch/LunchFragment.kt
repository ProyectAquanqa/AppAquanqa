package com.tecsup.aquanqa.ui.lunch

import com.tecsup.aquanqa.data.model.content.Almuerzo
import com.tecsup.aquanqa.data.repository.LunchRepository
import com.tecsup.aquanqa.ui.adapters.LunchAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.tecsup.aquanqa.databinding.FragmentLunchBinding
import com.tecsup.aquanqa.ui.base.BaseFragment
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment que muestra los menús de almuerzos del comedor de Tecsup.
 * 
 * Implementa el patrón MVVM y hereda de BaseFragment para mantener consistencia
 * con otros fragments de la aplicación. Maneja todos los estados de la UI:
 * loading, success, error y empty state.
 * 
 * Funcionalidades principales:
 * - Mostrar lista de almuerzos (solo días no feriados)
 * - Pull-to-refresh para actualizar datos
 * - Click en cards para abrir links de pedidos en navegador
 * - Manejo de errores con Snackbar
 * - Estados de carga y lista vacía
 */
class LunchFragment : BaseFragment<FragmentLunchBinding>() {

    // ViewModel con inyección de dependencias manual
    private val lunchViewModel: LunchViewModel by viewModels {
        // Crear dependencias siguiendo el patrón establecido en la app
        val apiClient = ApiClient.getClient(requireContext())
        val userPreferences = UserPreferences(requireContext())
        val repository = LunchRepository(apiClient.apiService, userPreferences)
        LunchViewModelFactory(repository)
    }
    
    private lateinit var lunchAdapter: LunchAdapter

    /**
     * Infla el layout específico del fragment usando ViewBinding.
     * 
     * @param inflater Inflater para crear las vistas
     * @param container Contenedor padre (puede ser null)
     * @return Binding del layout del fragment
     */
    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLunchBinding {
        return FragmentLunchBinding.inflate(inflater, container, false)
    }

    /**
     * Configura la UI inicial del fragment.
     * 
     * Inicializa todos los componentes de la interfaz de usuario,
     * incluyendo RecyclerView, SwipeRefreshLayout y listeners.
     */
    override fun setupUI() {
        super.setupUI()
        setupRecyclerView()
        setupSwipeRefresh()
    }

    /**
     * Configura el RecyclerView con su adapter y layout manager.
     * 
     * Incluye el callback para manejar clicks adicionales en los elementos
     * (además del click para abrir links que maneja directamente el adapter).
     */
    private fun setupRecyclerView() {
        lunchAdapter = LunchAdapter { almuerzo ->
            // Callback adicional para clicks en elementos (opcional)
            // Se puede usar para analytics, logging, etc.
            println("LunchFragment: Click en almuerzo ${almuerzo.nombreDia}")
        }
        
        binding.rvLunchMenu.apply {
            adapter = lunchAdapter
            layoutManager = LinearLayoutManager(requireContext())
            
            // Mejorar rendimiento con tamaño fijo
            setHasFixedSize(true)
        }
    }

    /**
     * Configura el SwipeRefreshLayout para pull-to-refresh.
     * 
     * Permite al usuario refrescar los datos deslizando hacia abajo
     * desde la parte superior de la lista.
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Refrescar almuerzos con fuerza cuando el usuario desliza
            lunchViewModel.refreshAlmuerzos()
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
     * Configura todos los observadores de datos del ViewModel.
     * 
     * Observa los cambios en el estado de la aplicación y actualiza
     * la UI correspondiente:
     * - Lista de almuerzos
     * - Estado de carga
     * - Mensajes de error
     * - Estado de lista vacía
     */
    override fun onResume() {
        super.onResume()
        // SIEMPRE intentar refresh para detectar contenido nuevo
        lunchViewModel.onAppResumed()
    }

    override fun setupObservers() {
        super.setupObservers()
        
        // Observar lista de almuerzos
        lunchViewModel.almuerzos.observe(viewLifecycleOwner) { almuerzos ->
            lunchAdapter.submitList(almuerzos)
            
            // Log para debugging
            println("LunchFragment: Actualizando UI con ${almuerzos.size} almuerzos")
        }

        // Observar estado de carga
        lunchViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Mostrar/ocultar indicador de carga
            if (isLoading) {
                showLoading()
            } else {
                hideLoading()
                // Ocultar indicador de pull-to-refresh cuando termine la carga
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        // Observar errores
        lunchViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                showLunchError(it)
                lunchViewModel.clearError() // Limpiar error después de mostrarlo
            }
        }

        // Observar estado vacío
        lunchViewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            if (isEmpty && lunchViewModel.isLoading.value != true) {
                showEmptyState()
            } else {
                hideEmptyState()
            }
        }
    }

    /**
     * Muestra el indicador de carga inicial.
     * 
     * Se usa cuando se carga por primera vez.
     */
    private fun showLoading() {
        // Mostrar indicador de carga si existe en el layout
        binding.rvLunchMenu.visibility = View.GONE
    }

    /**
     * Oculta el indicador de carga.
     */
    private fun hideLoading() {
        binding.rvLunchMenu.visibility = View.VISIBLE
    }

    /**
     * Muestra un mensaje de error usando Snackbar con opción de reintentar.
     * 
     * @param message Mensaje de error a mostrar
     */
    private fun showLunchError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Reintentar") {
                lunchViewModel.refreshAlmuerzos()
            }
            .show()
    }

    /**
     * Muestra el estado de lista vacía cuando no hay almuerzos disponibles.
     */
    private fun showEmptyState() {
        binding.rvLunchMenu.visibility = View.GONE
        // Mostrar mensaje usando Toast si no hay TextView específico para estado vacío
        showToast("No hay almuerzos disponibles en este momento", Toast.LENGTH_LONG)
    }

    /**
     * Oculta el estado de lista vacía.
     */
    private fun hideEmptyState() {
        binding.rvLunchMenu.visibility = View.VISIBLE
    }

    /**
     * Método público para detectar nuevos almuerzos.
     * Puede ser llamado desde otros fragments o activities.
     */
    fun detectNewLunches() {
        // Como lunchViewModel está inicializado con 'by viewModels', 
        // siempre está disponible cuando el fragment está activo
        lunchViewModel.checkForNewLunches()
    }

    /**
     * Limpia recursos cuando el fragment se destruye.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        // El adapter se limpia automáticamente al destruir el ViewBinding
    }
}