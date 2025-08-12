package com.tecsup.aquanqa.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tecsup.aquanqa.utils.ImageLoadingUtils
import com.google.android.material.snackbar.Snackbar
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.databinding.FragmentProfileBinding
import com.tecsup.aquanqa.data.model.user.UserProfile
import com.tecsup.aquanqa.ui.base.BaseFragment
import com.tecsup.aquanqa.data.api.NetworkConfig

/**
 * Fragment optimizado para mostrar el perfil del usuario con cache híbrido.
 * Implementa validación de conectividad para navegación a edición.
 */
class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private lateinit var viewModel: ProfileViewModel

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        super.setupUI()
        
        // Inicializar ViewModel con el Factory
        viewModel = ViewModelProvider(
            requireActivity(), 
            ProfileViewModelFactory(requireActivity().application)
        )[ProfileViewModel::class.java]
        
        // Configurar SwipeRefreshLayout
        setupSwipeRefresh()
        
        // Configurar listeners
        setupListeners()
        
        // Configurar botón de reintentar
        binding.btnRetry.setOnClickListener {
            viewModel.refreshProfile()
        }
    }

    override fun setupObservers() {
        super.setupObservers()
        
        //  PRIMERO: Observar estado de UI (para configurar la vista correctamente)
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            handleUiState(state)
        }
        
        //  SEGUNDO: Observar datos del perfil
        viewModel.userProfile.observe(viewLifecycleOwner) { userProfile ->
            setupUserProfileData(userProfile)
        }
        
        // Observar éxito de actualización
        viewModel.updateSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                showToast("Perfil actualizado exitosamente")
                viewModel.onUpdateFinished()
            }
        }
    }
    
    /**
     * Configura el SwipeRefreshLayout para pull-to-refresh.
     * Permite al usuario refrescar el perfil deslizando hacia abajo.
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            //  Usar método de refresh específico
            viewModel.refreshProfile()
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
        //  Solo recargar si es necesario (evitar llamadas innecesarias)
        if (::viewModel.isInitialized && viewModel.userProfile.value == null) {
            viewModel.loadUserProfile()
        }
    }
    
    /**
     * Configura los datos del perfil del usuario usando componentes reutilizables
     */
    private fun setupUserProfileData(userProfile: UserProfile) {
        // Extraer datos del perfil usando la estructura real
        val fullName = "${userProfile.first_name} ${userProfile.last_name}".trim()
        val dni = userProfile.username // Asumiendo que username contiene el DNI
        val email = userProfile.email ?: getString(R.string.no_email)
        val role = userProfile.groups?.firstOrNull() ?: "Usuario"
        
        // Configurar información principal
        binding.nameTextView.text = fullName
        binding.roleTextView.text = role
        
        // Configurar campos usando el helper reutilizable
        setupProfileFields(fullName, dni, email)
        
        // Cargar imágenes
        loadProfileImages(userProfile)
    }
    
    /**
     * Configura los campos del perfil usando los TextViews existentes en el layout
     */
    private fun setupProfileFields(fullName: String, dni: String, email: String) {
        // Configurar valores en los TextViews existentes
        binding.fullNameValueTextView.text = fullName
        binding.dniValueTextView.text = dni
        binding.emailValueTextView.text = email
        
        // Configurar campo de contraseña usando el helper
        ProfileFieldHelper.setupProfileField(
            binding.passwordField.root,
            label = "Contraseña",
            value = "••••••••",
            iconRes = R.drawable.ic_lock,
            showActionIcon = true
        )
    }
    
    /**
     * Carga las imágenes del perfil y firma
     */
    private fun loadProfileImages(userProfile: UserProfile) {
        // Cargar foto de perfil usando ImageLoadingUtils
        val imageUrl = userProfile.foto_perfil?.let { fotoUrl ->
            if (fotoUrl.startsWith("http")) fotoUrl else viewModel.getBaseUrl() + fotoUrl
        }
        
        // Usar ImageLoadingUtils para cargar la imagen con ic_profile.png por defecto
        ImageLoadingUtils.loadProfileImage(
            context = requireContext(),
            imageView = binding.profileImageView,
            imageUrl = imageUrl,
            useCircleCrop = true
        )
        
        // Cargar firma digital
        userProfile.firma?.let { firmaUrl ->
            val signatureUrl = if (firmaUrl.startsWith("http")) firmaUrl else viewModel.getBaseUrl() + firmaUrl
            
            Glide.with(requireContext())
                .load(signatureUrl)
                .placeholder(android.R.color.transparent)
                .error(android.R.color.transparent)
                .into(binding.signatureImageView)
        } ?: run {
            // Si no hay firma, mostrar transparente
            binding.signatureImageView.setImageResource(android.R.color.transparent)
        }
    }

    
    private fun setupListeners() {
        //  Configurar botón de edición con validación de conectividad
        binding.editButton.setOnClickListener {
            navigateToEditProfile()
        }
    }
    
    /**
     * Maneja todos los estados de UI de manera centralizada y clara.
     */
    private fun handleUiState(state: ProfileViewModel.ProfileUiState) {
        when (state) {
            is ProfileViewModel.ProfileUiState.Idle -> {
                showContent()
            }
            is ProfileViewModel.ProfileUiState.Loading -> {
                showLoading()
            }
            is ProfileViewModel.ProfileUiState.Success -> {
                showContent()
            }
            is ProfileViewModel.ProfileUiState.Error -> {
                showErrorState(state.message)
            }
        }
    }
    
    /**
     *  CRÍTICO: Valida conectividad antes de navegar a edición.
     * Solo permite entrar al fragment de edición si hay conexión.
     */
    private fun navigateToEditProfile() {
        if (NetworkConfig.ConnectivityUtils.isNetworkAvailable(requireContext())) {
            //  HAY CONEXIÓN: Permitir navegación
            findNavController().navigate(R.id.action_navigation_profile_to_editProfileFragment)
        } else {
            // ❌ SIN CONEXIÓN: Mostrar error y NO navegar
            Snackbar.make(
                binding.root, 
                "Error de conexión. No se puede editar el perfil sin internet.", 
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Muestra el estado de carga inicial
     */
    private fun showLoading() {
        binding.apply {
            progressBar.visibility = android.view.View.VISIBLE
            swipeRefreshLayout.visibility = android.view.View.GONE
            errorState.visibility = android.view.View.GONE
        }
    }

    /**
     * Muestra el contenido del perfil
     */
    private fun showContent() {
        binding.apply {
            progressBar.visibility = android.view.View.GONE
            swipeRefreshLayout.visibility = android.view.View.VISIBLE
            swipeRefreshLayout.isRefreshing = false
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
            errorState.visibility = android.view.View.VISIBLE
            tvErrorMessage.text = errorMessage
        }
    }
} 