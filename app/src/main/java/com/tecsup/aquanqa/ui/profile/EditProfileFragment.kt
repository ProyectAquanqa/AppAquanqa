package com.tecsup.aquanqa.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.databinding.FragmentEditProfileBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment para editar el perfil del usuario con funcionalidad de recorte de imágenes.
 * 
 * Características principales:
 * - Captura de imágenes desde cámara y galería
 * - Recorte profesional con UCrop
 * - Configuraciones específicas para fotos de perfil (1:1) y firmas (3:1)
 * - Gestión automática de permisos
 * - Interfaz de usuario intuitiva
 */
class EditProfileFragment : Fragment() {

    // ========== CONSTANTES ==========
    companion object {
        private const val TAG = "EditProfileFragment"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        
        // Configuraciones de imagen
        private const val PROFILE_MAX_SIZE = 512
        private const val SIGNATURE_MAX_WIDTH = 800
        private const val SIGNATURE_MAX_HEIGHT = 300
        private const val IMAGE_COMPRESSION_QUALITY = 90
    }

    // ========== PROPIEDADES ==========
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel

    // URIs de las imágenes seleccionadas
    private var selectedPhotoUri: Uri? = null
    private var selectedSignatureUri: Uri? = null
    private var temporalCameraUri: Uri? = null
    
    // Estado del flujo de selección
    private var isForSignatureSelection = false
    private var currentImageType: ImageType = ImageType.PROFILE_PHOTO
    
    /**
     * Enum para definir el tipo de imagen que se está procesando
     */
    private enum class ImageType {
        PROFILE_PHOTO,
        SIGNATURE
    }
    
    // ========== ACTIVITY RESULT LAUNCHERS ==========
    
    /** Launcher para seleccionar imagen de galería (foto de perfil) */
    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGalleryResult(result, ImageType.PROFILE_PHOTO)
    }
    
    /** Launcher para capturar imagen con cámara (foto de perfil) */
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        handleCameraResult(success, ImageType.PROFILE_PHOTO)
    }
    
    /** Launcher para seleccionar imagen de galería (firma) */
    private val pickSignatureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGalleryResult(result, ImageType.SIGNATURE)
    }
    
    /** Launcher para capturar imagen con cámara (firma) */
    private val takeSignatureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        handleCameraResult(success, ImageType.SIGNATURE)
    }
    
    /** Launcher para solicitar permisos de cámara */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handlePermissionResult(isGranted)
    }
    
    /** Launcher para manejar el resultado del recorte con UCrop */
    private val cropImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleCropResult(result)
    }

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
        
        // Inicializar ViewModel con el Factory
        viewModel = ViewModelProvider(
            requireActivity(), 
            ProfileViewModelFactory(requireActivity().application)
        )[ProfileViewModel::class.java]
        
        // Configurar observadores
        setupObservers()
        
        // Configurar listeners
        setupListeners()
    }
    
    private fun setupObservers() {
        // Observar cambios en el perfil del usuario para poblar la UI
        viewModel.userProfile.observe(viewLifecycleOwner) { userProfile ->
            binding.nameTextView.text = userProfile.first_name
            val role = userProfile.groups?.firstOrNull() ?: "Usuario"
            binding.roleTextView.text = role
            binding.emailEditText.setText(userProfile.email)
            
            // Cargar la foto de perfil actual o la seleccionada
            val photoToShow = selectedPhotoUri ?: userProfile.foto_perfil?.let {
                if (it.startsWith("http")) Uri.parse(it) else Uri.parse(viewModel.getBaseUrl() + it)
                }
                
                Glide.with(requireContext())
                .load(photoToShow ?: R.drawable.ic_person)
                    .apply(RequestOptions.circleCropTransform())
                    .into(binding.profileImageView)
                
            // Cargar la firma actual o la seleccionada
            val signatureToShow = selectedSignatureUri ?: userProfile.firma?.let {
                if (it.startsWith("http")) Uri.parse(it) else Uri.parse(viewModel.getBaseUrl() + it)
            }
                
                Glide.with(requireContext())
                .load(signatureToShow ?: R.drawable.dotted_border)
                .fitCenter()
                    .into(binding.signatureImageView)
        }
        
        // Observar el resultado de la actualización
        viewModel.updateSuccess.observe(viewLifecycleOwner) { hasFinished ->
            if (hasFinished) {
                Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                // Resetear el estado para evitar que se dispare de nuevo
                viewModel.onUpdateFinished()
            }
        }
        
        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observar errores
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setupListeners() {
        // Configurar botón para cambiar la foto
        binding.changePhotoButton.setOnClickListener {
            showImageSourceDialog(isForSignature = false)
        }
        
        // Configurar botón para cambiar la firma
        binding.changeSignatureButton.setOnClickListener {
            showImageSourceDialog(isForSignature = true)
        }
        
        // Configurar botón para guardar cambios
        binding.saveButton.setOnClickListener {
            val newEmail = binding.emailEditText.text.toString().trim()
            val newPassword = binding.passwordEditText.text.toString().trim()
            
            // Llamar al ViewModel para que inicie la actualización
            viewModel.updateProfile(selectedPhotoUri, selectedSignatureUri, newEmail, newPassword)
        }
        
        // Configurar botón para cancelar
        binding.cancelButton.setOnClickListener {
            // Navegar de vuelta al fragmento de perfil
            findNavController().popBackStack()
        }
    }
    
    // ========== FUNCIONES PÚBLICAS ==========
    
    /**
     * Muestra diálogo para seleccionar fuente de imagen
     */
    private fun showImageSourceDialog(isForSignature: Boolean) {
        isForSignatureSelection = isForSignature
        val imageType = if (isForSignature) ImageType.SIGNATURE else ImageType.PROFILE_PHOTO
        val title = if (isForSignature) "Seleccionar firma" else "Seleccionar foto de perfil"
        
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setItems(arrayOf("Tomar foto", "Seleccionar de galería")) { _, option ->
                when (option) {
                    0 -> handleCameraSelection(imageType)
                    1 -> handleGallerySelection(imageType)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    // ========== MANEJO DE RESULTADOS ==========
    
    /**
     * Maneja el resultado de selección desde galería
     */
    private fun handleGalleryResult(result: androidx.activity.result.ActivityResult, imageType: ImageType) {
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                currentImageType = imageType
                startImageCrop(uri, imageType)
            }
        }
    }
    
    /**
     * Maneja el resultado de captura con cámara
     */
    private fun handleCameraResult(success: Boolean, imageType: ImageType) {
        if (success && temporalCameraUri != null) {
            currentImageType = imageType
            startImageCrop(temporalCameraUri!!, imageType)
        }
    }
    
    /**
     * Maneja el resultado de solicitud de permisos
     */
    private fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            val imageType = if (isForSignatureSelection) ImageType.SIGNATURE else ImageType.PROFILE_PHOTO
            launchCamera(imageType)
        } else {
            showToast("Permiso de cámara requerido para tomar fotos")
        }
    }
    
    /**
     * Maneja el resultado del recorte de imagen
     */
    private fun handleCropResult(result: androidx.activity.result.ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { intent ->
                    UCrop.getOutput(intent)?.let { croppedUri ->
                        displayCroppedImage(croppedUri, currentImageType)
                        showToast("Imagen recortada exitosamente")
                    }
                }
            }
            UCrop.RESULT_ERROR -> {
                result.data?.let { intent ->
                    val error = UCrop.getError(intent)
                    Log.e(TAG, "Error en recorte", error)
                    showToast("Error al recortar imagen")
                }
            }
        }
    }
    
    // ========== FUNCIONES DE SELECCIÓN ==========
    
    /**
     * Maneja la selección de cámara
     */
    private fun handleCameraSelection(imageType: ImageType) {
        if (hasCameraPermission()) {
            launchCamera(imageType)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    /**
     * Maneja la selección de galería
     */
    private fun handleGallerySelection(imageType: ImageType) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val launcher = if (imageType == ImageType.PROFILE_PHOTO) pickPhotoLauncher else pickSignatureLauncher
        launcher.launch(intent)
    }
    
    /**
     * Lanza la cámara para captura de imagen
     */
    private fun launchCamera(imageType: ImageType) {
        try {
            temporalCameraUri = createTempImageUri()
            val launcher = if (imageType == ImageType.PROFILE_PHOTO) takePhotoLauncher else takeSignatureLauncher
            launcher.launch(temporalCameraUri)
        } catch (e: IOException) {
            Log.e(TAG, "Error al crear URI temporal", e)
            showToast("Error al preparar la cámara")
        }
    }
    
    // ========== FUNCIONES DE RECORTE DE IMÁGENES (UCROP) ==========
    
    /**
     * Inicia el proceso de recorte de imagen usando UCrop
     * @param sourceUri URI de la imagen original
     * @param imageType Tipo de imagen (PROFILE_PHOTO o SIGNATURE)
     */
    private fun startImageCrop(sourceUri: Uri, imageType: ImageType) {
        try {
            val destinationUri = createDestinationUri(imageType)
            val uCrop = UCrop.of(sourceUri, destinationUri)
                .withOptions(createCropOptions(imageType))
            
            cropImageLauncher.launch(uCrop.getIntent(requireContext()))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar recorte", e)
            showToast("Error al preparar el recorte de imagen")
        }
    }
    
    // ========== FUNCIONES DE UTILIDAD ==========
    
    /**
     * Verifica si se tienen permisos de cámara
     */
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), 
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Muestra un toast con el mensaje especificado
     */
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Muestra la imagen recortada en la interfaz correspondiente
     */
    private fun displayCroppedImage(croppedUri: Uri, imageType: ImageType) {
        when (imageType) {
            ImageType.PROFILE_PHOTO -> {
                selectedPhotoUri = croppedUri
                Glide.with(requireContext())
                    .load(croppedUri)
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(binding.profileImageView)
            }
            
            ImageType.SIGNATURE -> {
                selectedSignatureUri = croppedUri
                Glide.with(requireContext())
                    .load(croppedUri)
                    .fitCenter()
                    .placeholder(R.drawable.dotted_border)
                    .error(R.drawable.dotted_border)
                    .into(binding.signatureImageView)
            }
        }
    }
    
    /**
     * Configura UCrop con modelo único (estilo firma libre)
     */
    private fun createCropOptions(imageType: ImageType): UCrop.Options {
        return UCrop.Options().apply {
            // Configuración base
            setCompressionQuality(IMAGE_COMPRESSION_QUALITY)
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setHideBottomControls(false)
            setShowCropFrame(true)
            setShowCropGrid(true)
            
            // El status bar se maneja ahora desde el tema Theme.Aquanqa.UCrop
            setToolbarWidgetColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            
            // Ambos con recorte libre como la firma, solo diferente aspecto inicial
            setFreeStyleCropEnabled(true) // Recorte libre para ambos
            
            // Configurar zoom libre desde cualquier punto
            setMaxScaleMultiplier(10.0f) // Zoom máximo 10x para control detallado
            setImageToCropBoundsAnimDuration(500) // Animación suave
            
            if (imageType == ImageType.PROFILE_PHOTO) {
                // Foto de perfil: scale y recortable como la firma, pero cuadrado
                withAspectRatio(1f, 1f) // Sugerencia cuadrada inicial
                withMaxResultSize(PROFILE_MAX_SIZE, PROFILE_MAX_SIZE) // 512x512
                setToolbarTitle("Recortar imagen de perfil")
            } else {
                // Firma: scale y recortable (comportamiento original)
                withAspectRatio(3f, 1f) // Sugerencia rectangular inicial
                withMaxResultSize(SIGNATURE_MAX_WIDTH, SIGNATURE_MAX_HEIGHT) // 800x300
                setToolbarTitle("Recortar firma digital")
            }
            
            // Color unificado para ambos tipos
            val toolbarColor = ContextCompat.getColor(requireContext(), R.color.aquanqa_blue)
            setToolbarColor(toolbarColor)
            setActiveControlsWidgetColor(toolbarColor)
        }
    }
    
    /**
     * Genera URI de destino único para imagen recortada
     */
    private fun createDestinationUri(imageType: ImageType): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val prefix = if (imageType == ImageType.PROFILE_PHOTO) "PROFILE" else "SIGNATURE"
        val fileName = "CROPPED_${prefix}_${timestamp}.jpg"
        return Uri.fromFile(File(requireContext().cacheDir, fileName))
    }
    
    /**
     * Crea URI temporal para captura de cámara
     */
    @Throws(IOException::class)
    private fun createTempImageUri(): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File.createTempFile(
            "TEMP_${timestamp}_",
            ".jpg",
            requireContext().getExternalFilesDir(null)
        )
        
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 