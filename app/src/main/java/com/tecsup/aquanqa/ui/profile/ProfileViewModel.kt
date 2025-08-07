package com.tecsup.aquanqa.ui.profile

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.api.ApiConfig
import com.tecsup.aquanqa.data.model.user.UserProfile
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.tecsup.aquanqa.data.repository.UserRepository
import com.tecsup.aquanqa.utils.ErrorHelper
import kotlinx.coroutines.launch

/**
 * ViewModel optimizado para gestión de perfil con cache híbrido inteligente.
 * Implementa el mismo patrón que otros ViewModels para consistencia.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val userPreferences = UserPreferences(application)
    private val userRepository = UserRepository(application, userPreferences)
    
    // Estados de UI consolidados
    private val _uiState = MutableLiveData<ProfileUiState>(ProfileUiState.Idle)
    val uiState: LiveData<ProfileUiState> = _uiState
    
    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile
    
    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess
    
    init {
        loadUserProfile()
    }
    
    /**
     * Carga perfil de usuario con cache híbrido inteligente.
     * Memoria (rápido) + DataStore fallback (persistente) para máxima disponibilidad.
     * 
     * @param forceRefresh Forzar actualización desde API
     */
    fun loadUserProfile(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            when (val result = userRepository.getUserProfile(forceRefresh)) {
                is Result.Success -> {
                    _userProfile.value = result.data
                    _uiState.value = ProfileUiState.Success
                    Log.d(TAG, "Profile loaded successfully")
                }
                is Result.Error -> {
                    val errorMessage = ErrorHelper.getErrorMessage(result.exception)
                    _uiState.value = ProfileUiState.Error(errorMessage)
                    Log.e(TAG, "Error loading profile: $errorMessage")
                }
                is Result.Loading -> {
                    // El loading ya se maneja arriba
                }
                else -> {
                    _uiState.value = ProfileUiState.Error("Error inesperado al cargar el perfil")
                }
            }
        }
    }
    
    /**
     * Refresca datos del perfil forzando llamada a API.
     */
    fun refreshProfile() {
        Log.d(TAG, "Refreshing profile from API")
        loadUserProfile(forceRefresh = true)
    }
    
    /**
     * Inicia el proceso de actualización del perfil.
     * Primero actualiza los datos de texto (email, contraseña) si han cambiado,
     * y luego actualiza las imágenes (foto, firma) si se han seleccionado nuevas.
     *
     * @param photoUri La [Uri] de la nueva foto de perfil (opcional).
     * @param signatureUri La [Uri] de la nueva firma (opcional).
     * @param email El nuevo email (opcional).
     * @param password La nueva contraseña (opcional).
     * @param currentPassword La contraseña actual (requerida si se cambia la contraseña).
     */
    fun updateProfile(photoUri: Uri?, signatureUri: Uri?, email: String?, password: String?, currentPassword: String? = null) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            _updateSuccess.value = false
            var currentProfile: UserProfile? = _userProfile.value
            var textUpdateError: String? = null

            // 1. Actualizar datos de texto si es necesario
            val textData = mutableMapOf<String, String>()
            email?.let { if (it.isNotEmpty()) textData["email"] = it }
            password?.let { if (it.isNotEmpty()) {
                textData["password"] = it
                // Si se va a cambiar la contraseña, la contraseña actual es requerida
                currentPassword?.let { if (it.isNotEmpty()) textData["current_password"] = it }
            }}
            
            // Log para debugging (omitir contraseñas por seguridad)
            Log.d(TAG, "Updating profile with ${textData.keys}")
            if (textData.containsKey("password")) {
                Log.d(TAG, "Password update requested")
            }

            if (textData.isNotEmpty()) {
                when (val result = userRepository.updateProfileTextData(textData)) {
                    is Result.Success -> {
                        currentProfile = result.data
                        Log.d(TAG, "Profile text data updated successfully")
                    }
                    is Result.Error -> {
                        textUpdateError = result.exception.message ?: "Error al actualizar datos de texto"
                        Log.e(TAG, "Error updating profile text data: $textUpdateError")
                    }
                    is Result.Loading -> {
                        // El loading se maneja en el nivel superior
                    }
                    else -> {
                        textUpdateError = "Error inesperado al actualizar datos de texto"
                    }
                }
            }

            // Si hubo un error en la actualización de texto, lo notificamos y paramos.
            if (textUpdateError != null) {
                _uiState.value = ProfileUiState.Error(textUpdateError)
                return@launch
            }
            
            // 2. Actualizar imágenes si es necesario
            if (photoUri != null || signatureUri != null) {
                when (val result = userRepository.updateProfile(photoUri, signatureUri)) {
                    is Result.Success -> currentProfile = result.data
                    is Result.Error -> {
                        _uiState.value = ProfileUiState.Error(result.exception.message ?: "Error desconocido al subir imágenes.")
                        return@launch
                    }
                    is Result.Loading -> {
                        // El loading se maneja en el nivel superior
                    }
                    else -> {
                        _uiState.value = ProfileUiState.Error("Error inesperado al subir imágenes.")
                        return@launch
                    }
                }
            }
            
            // 3. Finalizar y notificar a la UI
            currentProfile?.let { _userProfile.value = it }
            _updateSuccess.value = true
            _uiState.value = ProfileUiState.Success
        }
    }
    
    /**
     * Resetea el estado del LiveData de éxito de la actualización.
     * Debe ser llamado por la UI después de consumir el evento de éxito.
     */
    fun onUpdateFinished() {
        _updateSuccess.value = false
    }
    
    /**
     * Proporciona la URL base para construir las rutas completas de las imágenes
     *retorna La URL base del servidor de medios.
     */
    fun getBaseUrl(): String {
        return ApiConfig.MEDIA_URL
    }
    
    /**
     * Estados de UI consolidados para mejor manejo.
     */
    sealed class ProfileUiState {
        object Idle : ProfileUiState()
        object Loading : ProfileUiState()
        object Success : ProfileUiState()
        data class Error(val message: String) : ProfileUiState()
    }
} 