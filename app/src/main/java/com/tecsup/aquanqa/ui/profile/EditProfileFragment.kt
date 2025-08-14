package com.tecsup.aquanqa.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.databinding.FragmentEditProfileBinding
import com.tecsup.aquanqa.data.model.user.PasswordChangeData
import com.tecsup.aquanqa.data.model.user.UserProfile
import com.tecsup.aquanqa.utils.ImageDisplayHelper
import com.tecsup.aquanqa.utils.ImagePickerManager
import com.tecsup.aquanqa.utils.ValidationHelper

/**
 * Fragment para editar el perfil del usuario.
 * Refactorizado para seguir principios SOLID y eliminar duplicación de código.
 * 
 * Responsabilidades:
 * - Gestión de UI del perfil
 * - Coordinación con ViewModel
 * - Delegación de manejo de imágenes a ImagePickerManager
 */
class EditProfileFragment : Fragment() {

    companion object {
        private const val TAG = "EditProfileFragment"
    }

    // ========== PROPIEDADES ==========
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private lateinit var imagePickerManager: ImagePickerManager

    // URIs de las imágenes seleccionadas
    private var selectedPhotoUri: Uri? = null
    private var selectedSignatureUri: Uri? = null

    // Datos de cambio de contraseña
    private var passwordChangeData: PasswordChangeData? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeComponents()
        setupObservers()
        setupListeners()
    }

    private fun initializeComponents() {
        // Inicializar ViewModel
        viewModel = ViewModelProvider(
            requireActivity(),
            ProfileViewModelFactory(requireActivity().application)
        )[ProfileViewModel::class.java]

        // Inicializar ImagePickerManager
        imagePickerManager = ImagePickerManager(
            fragment = this,
            onImageSelected = { uri, imageType ->
                handleImageSelected(uri, imageType)
            },
            onError = { message ->
                showToast(message)
            }
        )
    }
    
    private fun setupObservers() {
        //  Observar estado de UI consolidado
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            handleUiState(state)
        }
        
        viewModel.userProfile.observe(viewLifecycleOwner) { userProfile ->
            populateUserData(userProfile)
            loadImages(userProfile)
        }

        viewModel.updateSuccess.observe(viewLifecycleOwner) { hasFinished ->
            if (hasFinished) {
                handleUpdateSuccess()
            }
        }
    }
    
    /**
     *  Maneja todos los estados de UI de manera centralizada.
     */
    private fun handleUiState(state: ProfileViewModel.ProfileUiState) {
        when (state) {
            is ProfileViewModel.ProfileUiState.Idle -> {
                binding.loadingOverlay.visibility = View.GONE
            }
            is ProfileViewModel.ProfileUiState.Loading -> {
                binding.loadingOverlay.visibility = View.VISIBLE
            }
            is ProfileViewModel.ProfileUiState.Success -> {
                binding.loadingOverlay.visibility = View.GONE
            }
            is ProfileViewModel.ProfileUiState.Error -> {
                binding.loadingOverlay.visibility = View.GONE
                handleErrorMessage(state.message)
            }
        }
    }

    private fun populateUserData(userProfile: UserProfile) {
        binding.nameTextView.text = userProfile.first_name
        
        // Mostrar solo el cargo
        val cargo = when {
            userProfile.cargo_detail != null -> {
                userProfile.cargo_detail.nombre
            }
            else -> "Sin cargo asignado"
        }
        
        binding.roleTextView.text = cargo
        binding.emailEditText.setText(userProfile.email)
    }

    private fun loadImages(userProfile: UserProfile) {
        // Cargar foto de perfil
        ImageDisplayHelper.loadProfileImage(
            context = requireContext(),
            imageView = binding.profileImageView,
            imageUri = selectedPhotoUri,
            fallbackUrl = userProfile.foto_perfil,
            baseUrl = viewModel.getBaseUrl()
        )

        // Cargar firma
        ImageDisplayHelper.loadSignatureImage(
            context = requireContext(),
            imageView = binding.signatureImageView,
            imageUri = selectedSignatureUri,
            fallbackUrl = userProfile.firma,
            baseUrl = viewModel.getBaseUrl()
        )
    }

    private fun handleUpdateSuccess() {
        showToast("Perfil actualizado correctamente")
        findNavController().popBackStack()
        viewModel.onUpdateFinished()
    }

    /**
     * Maneja los mensajes de error mostrándolos en los inputs correspondientes.
     */
    private fun handleErrorMessage(errorMessage: String) {
        when {
            // Errores específicos de contraseña
            errorMessage.contains("contraseña actual no es correcta") || 
            errorMessage.contains("password") && errorMessage.contains("incorrect") -> {
                binding.passwordInputLayout.error = "La contraseña actual no es correcta"
                // Limpiar los datos de contraseña para que el usuario los vuelva a ingresar
                passwordChangeData = null
                binding.passwordEditText.setText("")
                binding.passwordInputLayout.helperText = null
            }
            
            // Errores específicos de email
            errorMessage.contains("email") && (errorMessage.contains("válido") || errorMessage.contains("uso")) -> {
                binding.emailInputLayout.error = "El email no es válido o ya está en uso"
            }
            
            // Errores generales - mostrar como toast ya que no corresponden a un input específico
            else -> {
                val userFriendlyMessage = when {
                    errorMessage.contains("sesión ha expirado") || errorMessage.contains("Token de acceso no disponible") -> 
                        "Tu sesión ha expirado. Inicia sesión nuevamente"
                    errorMessage.contains("network") || errorMessage.contains("conectar") || errorMessage.contains("conexión") -> 
                        "No hay conexión a internet. Verifica tu conexión"
                    errorMessage.contains("timeout") || errorMessage.contains("tardó demasiado") -> 
                        "La operación tardó demasiado tiempo. Intenta nuevamente"
                    errorMessage.contains("imágenes") && errorMessage.contains("grandes") -> 
                        "Las imágenes son muy grandes. Elige imágenes más pequeñas"
                    errorMessage.contains("formato") && errorMessage.contains("imagen") -> 
                        "Formato de imagen no válido. Usa JPG o PNG"
                    errorMessage.length < 80 && !errorMessage.contains("Exception") && 
                    !errorMessage.contains("Error:") && !errorMessage.contains("IOException") && 
                    !errorMessage.contains("401") && !errorMessage.contains("400") && !errorMessage.contains("500") -> 
                        errorMessage
                    else -> "Error al actualizar el perfil. Intenta nuevamente"
                }
                showToast(userFriendlyMessage)
            }
        }
    }
    
    private fun setupListeners() {
        binding.changePhotoButton.setOnClickListener {
            imagePickerManager.showImageSourceDialog(ImagePickerManager.ImageType.PROFILE_PHOTO)
        }

        binding.changeSignatureButton.setOnClickListener {
            imagePickerManager.showImageSourceDialog(ImagePickerManager.ImageType.SIGNATURE)
        }

        binding.passwordEditText.setOnClickListener {
            showPasswordChangeBottomSheet()
        }

        // Configurar campo de contraseña como no editable directamente
        binding.passwordEditText.isFocusable = false
        binding.passwordEditText.isClickable = true

        // Validación en tiempo real para email
        binding.emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateEmailField()
            }
        }

        binding.saveButton.setOnClickListener {
            saveProfile()
        }

        binding.cancelButton.setOnClickListener {
            // Mostrar confirmación si hay cambios pendientes
            if (hasUnsavedChanges()) {
                showCancelConfirmationDialog()
            } else {
                findNavController().popBackStack()
            }
        }
    }

    private fun saveProfile() {
        // Limpiar errores previos
        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null
        
        // Validar email
        val newEmail = binding.emailEditText.text.toString().trim()
        val (isEmailValid, emailError) = ValidationHelper.validateEmail(newEmail)
        
        if (!isEmailValid) {
            binding.emailInputLayout.error = emailError
            return
        }

        // Si hay cambio de contraseña, validar que se haya proporcionado la contraseña actual
        if (passwordChangeData != null) {
            val currentPassword = passwordChangeData?.currentPassword
            val newPassword = passwordChangeData?.newPassword
            
            if (currentPassword.isNullOrBlank()) {
                binding.passwordInputLayout.error = "Debes ingresar tu contraseña actual"
                return
            }
            
            if (newPassword.isNullOrBlank()) {
                binding.passwordInputLayout.error = "La nueva contraseña no puede estar vacía"
                return
            }
        }
        
        // Si todas las validaciones pasan, proceder con la actualización
        viewModel.updateProfile(
            selectedPhotoUri,
            selectedSignatureUri,
            newEmail,
            passwordChangeData?.newPassword,
            passwordChangeData?.currentPassword
        )
    }
    
    // funciones privadas

    /**
     * Maneja la selección de imagen desde ImagePickerManager
     */
    private fun handleImageSelected(uri: Uri, imageType: ImagePickerManager.ImageType) {
        when (imageType) {
            ImagePickerManager.ImageType.PROFILE_PHOTO -> {
                selectedPhotoUri = uri
                ImageDisplayHelper.loadProfileImage(
                    context = requireContext(),
                    imageView = binding.profileImageView,
                    imageUri = uri
                )
                showToast("Imagen de perfil seleccionada")
            }
            ImagePickerManager.ImageType.SIGNATURE -> {
                selectedSignatureUri = uri
                ImageDisplayHelper.loadSignatureImage(
                    context = requireContext(),
                    imageView = binding.signatureImageView,
                    imageUri = uri
                )
                showToast("Firma seleccionada")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Valida el campo de email en tiempo real.
     */
    private fun validateEmailField() {
        val email = binding.emailEditText.text.toString().trim()
        val (isValid, errorMessage) = ValidationHelper.validateEmail(email)
        
        if (!isValid) {
            binding.emailInputLayout.error = errorMessage
        } else {
            binding.emailInputLayout.error = null
        }
    }

    /**
     * Verifica si hay cambios sin guardar en el formulario.
     */
    private fun hasUnsavedChanges(): Boolean {
        val currentEmail = binding.emailEditText.text.toString().trim()
        val originalEmail = viewModel.userProfile.value?.email ?: ""
        
        return currentEmail != originalEmail || 
               selectedPhotoUri != null || 
               selectedSignatureUri != null || 
               passwordChangeData != null
    }

    /**
     * Muestra un diálogo de confirmación antes de cancelar con cambios pendientes.
     */
    private fun showCancelConfirmationDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("¿Descartar cambios?")
        builder.setMessage("Tienes cambios sin guardar. ¿Estás seguro de que quieres salir?")
        builder.setPositiveButton("Descartar") { _, _ ->
            findNavController().popBackStack()
        }
        builder.setNegativeButton("Continuar editando", null)
        builder.show()
    }
    
    /**
     * Muestra el bottom sheet para cambiar contraseña
     */
    private fun showPasswordChangeBottomSheet() {
        val bottomSheet = ChangePasswordFragment.newInstance()
        
        // Configurar callback para cuando se guarde la nueva contraseña
        bottomSheet.setOnPasswordSavedListener { passwordData ->
            passwordChangeData = passwordData
            
            // Actualizar el texto del campo para mostrar que hay una nueva contraseña
            binding.passwordEditText.setText("••••••••") // Mostrar asteriscos
            binding.passwordInputLayout.helperText = "Nueva contraseña configurada - Se verificará al guardar"
            binding.passwordInputLayout.setHelperTextColor(
                requireContext().getColorStateList(R.color.aquanqa_blue)
            )
        }
        
        // Mostrar el bottom sheet
        bottomSheet.show(parentFragmentManager, "PasswordChangeBottomSheet")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 