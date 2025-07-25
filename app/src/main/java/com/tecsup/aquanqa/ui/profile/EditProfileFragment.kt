package com.tecsup.aquanqa.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment para editar el perfil del usuario.
 * Permite cambiar la foto de perfil y la firma digitalizada.
 */
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel

    // Uri de la nueva foto de perfil seleccionada
    private var selectedPhotoUri: Uri? = null
    
    // Uri de la nueva firma seleccionada
    private var selectedSignatureUri: Uri? = null
    
    // Uri temporal para la cámara
    private var temporalCameraUri: Uri? = null
    
    // Flag para recordar si estamos eligiendo para firma o foto de perfil
    private var isForSignatureSelection = false
    
    // Request code para permisos de cámara
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    
    // Registro para manejar la selección de imágenes de la galería para la foto de perfil
    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedPhotoUri = uri
                // Mostrar la foto seleccionada en la UI
                Glide.with(requireContext())
                    .load(uri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(binding.profileImageView)
                
            }
        }
    }
    
    // Registro para manejar la toma de fotos con la cámara para la foto de perfil
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && temporalCameraUri != null) {
            selectedPhotoUri = temporalCameraUri
            // Mostrar la foto tomada en la UI
            Glide.with(requireContext())
                .load(temporalCameraUri)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.profileImageView)
            
        }
    }
    
    // Registro para manejar la selección de imágenes de la galería para la firma
    private val pickSignatureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedSignatureUri = uri
                // Mostrar la firma seleccionada en la UI
                Glide.with(requireContext())
                    .load(uri)
                    .fitCenter()
                    .into(binding.signatureImageView)
                
            }
        }
    }
    
    // Registro para manejar la toma de fotos con la cámara para la firma
    private val takeSignatureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && temporalCameraUri != null) {
            selectedSignatureUri = temporalCameraUri
            // Mostrar la foto tomada como firma en la UI
            Glide.with(requireContext())
                .load(temporalCameraUri)
                .fitCenter()
                .into(binding.signatureImageView)
            
        }
    }
    
    // Registro para solicitar permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido, proceder con la acción correspondiente
            if (isForSignatureSelection) {
                launchCamera(true)
            } else {
                launchCamera(false)
            }
        } else {
            // Permiso denegado, mostrar mensaje al usuario
            Toast.makeText(
                requireContext(),
                "Permiso de cámara denegado. No se puede tomar la foto.",
                Toast.LENGTH_SHORT
            ).show()
        }
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
    
    /**
     * Muestra un diálogo para seleccionar la fuente de la imagen (cámara o galería)
     * @param isForSignature true si es para la firma, false si es para la foto de perfil
     */
    private fun showImageSourceDialog(isForSignature: Boolean) {
        val options = arrayOf("Tomar foto", "Seleccionar de galería")
        isForSignatureSelection = isForSignature
        
        AlertDialog.Builder(requireContext())
            .setTitle(if (isForSignature) "Seleccionar firma" else "Seleccionar foto")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Tomar foto con la cámara
                        if (checkCameraPermission()) {
                            launchCamera(isForSignature)
                        } else {
                            requestCameraPermission()
                        }
                    }
                    1 -> { // Seleccionar de galería
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        if (isForSignature) {
                            pickSignatureLauncher.launch(intent)
                        } else {
                            pickPhotoLauncher.launch(intent)
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    /**
     * Verifica si la aplicación tiene permiso para usar la cámara
     * @return true si tiene permiso, false en caso contrario
     */
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), 
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Solicita el permiso de cámara al usuario
     */
    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
    
    /**
     * Inicia la cámara para tomar una foto
     * @param isForSignature true si es para la firma, false si es para la foto de perfil
     */
    private fun launchCamera(isForSignature: Boolean) {
        try {
            temporalCameraUri = createTempImageUri()
            if (isForSignature) {
                takeSignatureLauncher.launch(temporalCameraUri)
            } else {
                takePhotoLauncher.launch(temporalCameraUri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al abrir la cámara", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Crea un URI temporal para guardar la imagen capturada por la cámara
     * @return URI del archivo temporal
     */
    @Throws(IOException::class)
    private fun createTempImageUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = requireContext().getExternalFilesDir(null)
        val imageFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
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