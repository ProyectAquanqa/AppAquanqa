package com.tecsup.aquanqa.ui.notifications

import com.tecsup.aquanqa.data.model.content.NotificationItem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.databinding.FragmentNotificationsBinding
import com.tecsup.aquanqa.ui.base.BaseFragment
import com.tecsup.aquanqa.utils.InfiniteScrollListener

/**
 * Fragment para mostrar la lista de notificaciones del usuario.
 * Extiende BaseFragment para mantener consistencia con el resto de la aplicación.
 */
class NotificationsFragment : BaseFragment<FragmentNotificationsBinding>() {

    private lateinit var viewModel: NotificationViewModel
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var infiniteScrollListener: InfiniteScrollListener

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
        
        // Observar estado de carga de más elementos
        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoadingMore ->
            // El estado de loading more se puede mostrar en el último item del adapter si es necesario
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
     * Configura el RecyclerView con su adapter, layout manager e infinite scroll.
     */
    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter { notification ->
            android.util.Log.d("NotificationsFragment", "🎯 CLICK RECIBIDO desde adapter")
            android.util.Log.d("NotificationsFragment", "Notification: ${notification.id} - '${notification.title}'")
            viewModel.onNotificationClicked(notification)
            // Navegar al detalle si la notificación tiene evento
            notification.evento?.let { evento ->
                android.util.Log.d("NotificationsFragment", "=== NAVEGACIÓN A EVENTO ===")
                android.util.Log.d("NotificationsFragment", "Notification ID: ${notification.id}")
                android.util.Log.d("NotificationsFragment", "Evento ID: ${evento.id}")
                android.util.Log.d("NotificationsFragment", "Evento título: '${evento.titulo}'")
                android.util.Log.d("NotificationsFragment", "Evento autor: '${evento.autor.fullName}'")
                
                if (evento.id <= 0) {
                    android.util.Log.e("NotificationsFragment", "ERROR: eventoId inválido: ${evento.id}")
                    return@let
                }
                
                val action = com.tecsup.aquanqa.R.id.action_notifications_to_eventDetailFragment
                android.util.Log.d("NotificationsFragment", "Usando acción de navegación: $action con eventoId=${evento.id}")
                findNavController().navigate(action, bundleOf("eventoId" to evento.id))
            } ?: run {
                android.util.Log.w("NotificationsFragment", "Notificación sin evento asociado: ${notification.title}")
            }
        }
        
        val layoutManager = LinearLayoutManager(requireContext())
        
        // Configurar InfiniteScrollListener
        infiniteScrollListener = InfiniteScrollListener(
            layoutManager = layoutManager,
            visibleThreshold = 5
        ) {
            // Callback para cargar más datos
            if (viewModel.canLoadMore()) {
                viewModel.loadMoreNotifications()
            }
        }
        
        binding.rvNotifications.apply {
            adapter = notificationAdapter
            this.layoutManager = layoutManager
            setHasFixedSize(true)
            
            // Agregar el scroll listener para infinite scroll
            addOnScrollListener(infiniteScrollListener)
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