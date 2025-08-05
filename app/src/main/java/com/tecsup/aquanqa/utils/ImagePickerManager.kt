package com.tecsup.aquanqa.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manager centralizado para manejo de selección y recorte de imágenes.
 * Elimina la duplicación de código y centraliza la lógica de imágenes.
 */
class ImagePickerManager(
    private val fragment: Fragment,
    private val onImageSelected: (Uri, ImageType) -> Unit,
    private val onError: (String) -> Unit
) {
    
    companion object {
        private const val TAG = "ImagePickerManager"
        
        // Configuraciones de imagen
        private const val PROFILE_MAX_SIZE = 512
        private const val SIGNATURE_MAX_WIDTH = 800
        private const val SIGNATURE_MAX_HEIGHT = 300
        private const val IMAGE_COMPRESSION_QUALITY = 90
    }
    
    enum class ImageType {
        PROFILE_PHOTO,
        SIGNATURE
    }
    
    private var currentImageType: ImageType = ImageType.PROFILE_PHOTO
    private var temporalCameraUri: Uri? = null
    
    // ========== ACTIVITY RESULT LAUNCHERS ==========
    
    private val galleryLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGalleryResult(result)
    }
    
    private val cameraLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        handleCameraResult(success)
    }
    
    private val permissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handlePermissionResult(isGranted)
    }
    
    private val cropLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleCropResult(result)
    }
    
    // ========== FUNCIONES PÚBLICAS ==========
    
    /**
     * Muestra diálogo para seleccionar fuente de imagen
     */
    fun showImageSourceDialog(imageType: ImageType) {
        currentImageType = imageType
        val title = when (imageType) {
            ImageType.PROFILE_PHOTO -> "Seleccionar foto de perfil"
            ImageType.SIGNATURE -> "Seleccionar firma"
        }
        
        AlertDialog.Builder(fragment.requireContext())
            .setTitle(title)
            .setItems(arrayOf("Tomar foto", "Seleccionar de galería")) { _, option ->
                when (option) {
                    0 -> handleCameraSelection()
                    1 -> handleGallerySelection()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    // ========== FUNCIONES PRIVADAS ==========
    
    private fun handleGallerySelection() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }
    
    private fun handleCameraSelection() {
        if (hasCameraPermission()) {
            launchCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun launchCamera() {
        try {
            temporalCameraUri = createTempImageUri()
            cameraLauncher.launch(temporalCameraUri)
        } catch (e: IOException) {
            Log.e(TAG, "Error al crear URI temporal", e)
            onError("Error al preparar la cámara")
        }
    }
    
    private fun handleGalleryResult(result: androidx.activity.result.ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                startImageCrop(uri)
            }
        }
    }
    
    private fun handleCameraResult(success: Boolean) {
        if (success && temporalCameraUri != null) {
            startImageCrop(temporalCameraUri!!)
        }
    }
    
    private fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            launchCamera()
        } else {
            onError("Permiso de cámara requerido para tomar fotos")
        }
    }
    
    private fun handleCropResult(result: androidx.activity.result.ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { intent ->
                    UCrop.getOutput(intent)?.let { croppedUri ->
                        onImageSelected(croppedUri, currentImageType)
                    }
                }
            }
            UCrop.RESULT_ERROR -> {
                result.data?.let { intent ->
                    val error = UCrop.getError(intent)
                    Log.e(TAG, "Error en recorte", error)
                    onError("Error al recortar imagen")
                }
            }
        }
    }
    
    private fun startImageCrop(sourceUri: Uri) {
        try {
            val destinationUri = createDestinationUri()
            val uCrop = UCrop.of(sourceUri, destinationUri)
                .withOptions(createCropOptions())
            
            cropLauncher.launch(uCrop.getIntent(fragment.requireContext()))
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar recorte", e)
            onError("Error al preparar el recorte de imagen")
        }
    }
    
    private fun createCropOptions(): UCrop.Options {
        return UCrop.Options().apply {
            // Configuración base
            setCompressionQuality(IMAGE_COMPRESSION_QUALITY)
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setHideBottomControls(false)
            setShowCropFrame(true)
            setShowCropGrid(true)
            setFreeStyleCropEnabled(true)
            setMaxScaleMultiplier(10.0f)
            setImageToCropBoundsAnimDuration(500)
            
            // Configuración específica por tipo
            when (currentImageType) {
                ImageType.PROFILE_PHOTO -> {
                    withAspectRatio(1f, 1f)
                    withMaxResultSize(PROFILE_MAX_SIZE, PROFILE_MAX_SIZE)
                    setToolbarTitle("Recortar imagen de perfil")
                }
                ImageType.SIGNATURE -> {
                    withAspectRatio(3f, 1f)
                    withMaxResultSize(SIGNATURE_MAX_WIDTH, SIGNATURE_MAX_HEIGHT)
                    setToolbarTitle("Recortar firma digital")
                }
            }
            
            // Colores del tema
            val context = fragment.requireContext()
            setToolbarWidgetColor(ContextCompat.getColor(context, android.R.color.white))
            val toolbarColor = ContextCompat.getColor(context, com.tecsup.aquanqa.R.color.aquanqa_blue)
            setToolbarColor(toolbarColor)
            setActiveControlsWidgetColor(toolbarColor)
        }
    }
    
    private fun createDestinationUri(): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val prefix = when (currentImageType) {
            ImageType.PROFILE_PHOTO -> "PROFILE"
            ImageType.SIGNATURE -> "SIGNATURE"
        }
        val fileName = "CROPPED_${prefix}_${timestamp}.jpg"
        return Uri.fromFile(File(fragment.requireContext().cacheDir, fileName))
    }
    
    @Throws(IOException::class)
    private fun createTempImageUri(): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File.createTempFile(
            "TEMP_${timestamp}_",
            ".jpg",
            fragment.requireContext().getExternalFilesDir(null)
        )
        
        return FileProvider.getUriForFile(
            fragment.requireContext(),
            "${fragment.requireContext().packageName}.provider",
            imageFile
        )
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            fragment.requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}