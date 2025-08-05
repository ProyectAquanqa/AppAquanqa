package com.tecsup.aquanqa.ui.notifications

import com.tecsup.aquanqa.data.model.content.NotificationItem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.databinding.FragmentNotificationsBinding
import com.tecsup.aquanqa.ui.base.BaseFragment

/**
 * Fragment para mostrar la lista de notificaciones del usuario.
 * Extiende BaseFragment para mantener consistencia con el resto de la aplicación.
 */
class NotificationsFragment : BaseFragment<FragmentNotificationsBinding>() {

    private lateinit var viewModel: NotificationViewModel
    private lateinit var notificationAdapter: NotificationAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNotificationsBinding {
        return FragmentNotificationsBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        super.setupUI()
        
        // Inicializar ViewModel usando el Factory mejorado
        val factory = NotificationViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[NotificationViewModel::class.java]
        
        // Configurar RecyclerView
        setupRecyclerView()
        
        // Configurar SwipeRefreshLayout
        setupSwipeRefresh()
        
        // Configurar botón de reintentar
        binding.btnRetry.setOnClickListener {
            viewModel.loadNotifications()
        }
    }

    override fun setupObservers() {
        super.setupObservers()
        
        // Observar estado de las notificaciones
        viewModel.notificationsState.observe(viewLifecycleOwner) { result ->
            android.util.Log.d("NotificationsFragment", "Estado observado: $result")
            when (result) {
                is Result.Loading -> {
                    android.util.Log.d("NotificationsFragment", "Mostrando estado de carga")
                    showLoadingState()
                }
                is Result.Success -> {
                    android.util.Log.d("NotificationsFragment", "Éxito con ${result.data.size} items")
                    showSuccessState(result.data)
                }
                is Result.Error -> {
                    android.util.Log.e("NotificationsFragment", "Error: ${result.exception.message}")
                    showErrorState(result.exception.message ?: "Error desconocido")
                }
            }
        }
        
        // Observar estado de refresh
        viewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }
        
        // Cargar notificaciones automáticamente al inicializar el fragment
        if (!::viewModel.isInitialized) {
            android.util.Log.d("NotificationsFragment", "ViewModel no inicializado, esperando...")
        } else {
            android.util.Log.d("NotificationsFragment", "Cargando notificaciones automáticamente")
            viewModel.loadNotifications()
        }
    }

    /**
     * Configura el RecyclerView con su adapter y layout manager.
     */
    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter { notification ->
            viewModel.onNotificationClicked(notification)
        }
        
        binding.rvNotifications.apply {
            adapter = notificationAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    /**
     * Configura el SwipeRefreshLayout para pull-to-refresh.
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshNotifications()
        }
        
        // Configurar colores del refresh indicator
        binding.swipeRefresh.setColorSchemeColors(
            requireContext().getColor(com.tecsup.aquanqa.R.color.aquanqa_blue)
        )
    }

    /**
     * Muestra el estado de carga.
     */
    private fun showLoadingState() {
        binding.apply {
            progressBar.visibility = View.VISIBLE
            rvNotifications.visibility = View.GONE
            emptyState.visibility = View.GONE
            errorState.visibility = View.GONE
        }
    }

    /**
     * Muestra el estado de éxito con datos.
     */
    private fun showSuccessState(notificationItems: List<NotificationItem>) {
        android.util.Log.d("NotificationsFragment", "showSuccessState con ${notificationItems.size} items")
        binding.apply {
            progressBar.visibility = View.GONE
            errorState.visibility = View.GONE
            
            if (notificationItems.isEmpty()) {
                android.util.Log.d("NotificationsFragment", "Lista vacía, mostrando empty state")
                rvNotifications.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            } else {
                android.util.Log.d("NotificationsFragment", "Mostrando ${notificationItems.size} items en RecyclerView")
                rvNotifications.visibility = View.VISIBLE
                emptyState.visibility = View.GONE
                notificationAdapter.submitList(notificationItems)
            }
        }
    }

    /**
     * Muestra el estado de error.
     */
    private fun showErrorState(errorMessage: String) {
        binding.apply {
            progressBar.visibility = View.GONE
            rvNotifications.visibility = View.GONE
            emptyState.visibility = View.GONE
            errorState.visibility = View.VISIBLE
            tvErrorMessage.text = errorMessage
        }
    }
}