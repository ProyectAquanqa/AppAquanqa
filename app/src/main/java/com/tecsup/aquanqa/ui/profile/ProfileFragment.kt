package com.tecsup.aquanqa.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.databinding.FragmentProfileBinding
import com.tecsup.aquanqa.data.model.UserProfile
import com.tecsup.aquanqa.ui.base.BaseFragment

/**
 * Fragment para mostrar el perfil del usuario.
 * Muestra la información del usuario y permite navegar a la pantalla de edición.
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
        
        // Configurar listeners
        setupListeners()
        
        // Cargar datos del perfil
        viewModel.loadUserProfile()
    }

    override fun setupObservers() {
        super.setupObservers()
        
        // Observar cambios en el perfil del usuario
        viewModel.userProfile.observe(viewLifecycleOwner) { userProfile ->
            setupUserProfileData(userProfile)
        }
        
        // Observar errores
        viewModel.error.observe(viewLifecycleOwner) { error ->
            showError(error)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar perfil al regresar al fragmento (por ejemplo, después de editar)
        if (::viewModel.isInitialized) {
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
            iconRes = R.drawable.ic_lock_outline,
            showActionIcon = true
        )
    }
    
    /**
     * Carga las imágenes del perfil y firma
     */
    private fun loadProfileImages(userProfile: UserProfile) {
        // Cargar foto de perfil
        userProfile.foto_perfil?.let { fotoUrl ->
            val imageUrl = if (fotoUrl.startsWith("http")) fotoUrl else viewModel.getBaseUrl() + fotoUrl
            
            Glide.with(requireContext())
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.profileImageView)
        } ?: run {
            // Si no hay foto de perfil, mostrar el icono por defecto
            Glide.with(requireContext())
                .load(R.drawable.ic_person)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.profileImageView)
        }
        
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
        // Configurar botón de edición
        binding.editButton.setOnClickListener {
            // Navegar al fragmento de edición de perfil
            findNavController().navigate(R.id.action_navigation_profile_to_editProfileFragment)
        }
    }


} 