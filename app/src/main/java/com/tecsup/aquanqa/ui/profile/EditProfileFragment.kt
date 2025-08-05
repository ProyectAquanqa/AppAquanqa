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
        viewModel.userProfile.observe(viewLifecycleOwner) { userProfile ->
            populateUserData(userProfile)
            loadImages(userProfile)
        }

        viewModel.updateSuccess.observe(viewLifecycleOwner) { hasFinished ->
            if (hasFinished) {
                handleUpdateSuccess()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                showToast(error)
            }
        }
    }

    private fun populateUserData(userProfile: UserProfile) {
        binding.nameTextView.text = userProfile.first_name
        val role = userProfile.groups?.firstOrNull() ?: "Usuario"
        binding.roleTextView.text = role
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
        showToast(getString(R.string.profile_updated))
        findNavController().popBackStack()
        viewModel.onUpdateFinished()
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

        binding.saveButton.setOnClickListener {
            saveProfile()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun saveProfile() {
        val newEmail = binding.emailEditText.text.toString().trim()
        
        viewModel.updateProfile(
            selectedPhotoUri,
            selectedSignatureUri,
            newEmail,
            passwordChangeData?.newPassword,
            passwordChangeData?.currentPassword
        )
    }
    
    // ========== FUNCIONES PRIVADAS ==========

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
                showToast("Imagen de perfil actualizada")
            }
            ImagePickerManager.ImageType.SIGNATURE -> {
                selectedSignatureUri = uri
                ImageDisplayHelper.loadSignatureImage(
                    context = requireContext(),
                    imageView = binding.signatureImageView,
                    imageUri = uri
                )
                showToast("Firma actualizada")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
            binding.passwordInputLayout.helperText = "Nueva contraseña configurada"
        }
        
        // Mostrar el bottom sheet
        bottomSheet.show(parentFragmentManager, "PasswordChangeBottomSheet")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 