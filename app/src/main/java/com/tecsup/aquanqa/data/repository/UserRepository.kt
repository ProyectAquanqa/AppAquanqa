package com.tecsup.aquanqa.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.api.ApiConfig
import com.tecsup.aquanqa.data.cache.CacheManager
import com.tecsup.aquanqa.data.cache.CacheResult
import com.tecsup.aquanqa.data.model.user.UserProfile
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.tecsup.aquanqa.utils.ErrorHelper
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Repositorio optimizado para manejar información del perfil con cache híbrido.
 * Implementa el mismo patrón que otros repositorios para consistencia.
 */
class UserRepository(
    private val context: Context,
    private val userPreferences: UserPreferences,
    private val cacheManager: CacheManager = CacheManager()
) {
    
    companion object {
        private const val TAG = "UserRepository"
        private const val USER_PROFILE_CACHE_KEY = "user_profile_data"
    }

    /**
     *  Obtiene el perfil con cache híbrido inteligente.
     * Memoria (rápido) + DataStore fallback (persistente) para máxima disponibilidad.
     * 
     * @param forceRefresh Forzar actualización desde API
     */
    suspend fun getUserProfile(forceRefresh: Boolean = false): Result<UserProfile> {
        return try {
            val userId = getCurrentUserId()
            
            //  1. PRIMERO: Verificar cache en memoria (rápido)
            if (!forceRefresh) {
                val memoryCacheResult = cacheManager.getCachedUserProfile(USER_PROFILE_CACHE_KEY, userId)
                when (memoryCacheResult) {
                    is CacheResult.Hit -> {
                        Log.d(TAG, "Profile hit from memory cache")
                        return Result.Success(memoryCacheResult.data)
                    }
                    is CacheResult.Miss -> {
                        //  2. SEGUNDO: Verificar fallback en DataStore (persistente)
                        val fallbackResult = getUserProfileFallbackFromDataStore()
                        if (fallbackResult != null) {
                            Log.d(TAG, "Profile loaded from DataStore fallback")
                            return Result.Success(fallbackResult)
                        }
                    }
                    is CacheResult.Expired -> {
                        // Cache expirado, buscar en API
                        Log.d(TAG, "Profile cache expired, refreshing from API")
                    }
                }
            }
            
            //  3. ÚLTIMO: Cargar desde API
            val token = getValidToken() ?: return Result.Error(Exception("Su sesión ha expirado."))
            val response = ApiClient.apiService.getUserProfile("Bearer $token")
            
            if (response.isSuccessful && response.body() != null) {
                val userProfile = response.body()!!
                
                //  Guardar en ambos caches
                cacheManager.cacheUserProfile(USER_PROFILE_CACHE_KEY, userProfile, userId)
                saveUserProfileFallbackToDataStore(userProfile)
                
                Log.d(TAG, "Profile loaded from API and cached")
                Result.Success(userProfile)
            } else {
                //  Si falla API, intentar fallback una vez más
                val fallbackResult = getUserProfileFallbackFromDataStore()
                if (fallbackResult != null) {
                    Log.w(TAG, "API failed, using DataStore fallback")
                    return Result.Success(fallbackResult)
                }
                
                Result.Error(IOException("No se pudo cargar el perfil. Intenta nuevamente"))
            }
        } catch (e: Exception) {
            //  En caso de excepción, intentar fallback
            val fallbackResult = getUserProfileFallbackFromDataStore()
            if (fallbackResult != null) {
                Log.w(TAG, "Exception occurred, using DataStore fallback: ${e.message}")
                return Result.Success(fallbackResult)
            }
            
            Result.Error(IOException(ErrorHelper.getErrorMessage(e), e))
        }
    }
    
    /**
     * Actualiza el perfil del usuario, permitiendo cambiar la foto y/o la firma.
     */
    suspend fun updateProfile(photoUri: Uri?, signatureUri: Uri?): Result<UserProfile> {
        return try {
            val accessToken = userPreferences.accessToken.first()
                ?: return Result.Error(IOException("Token de acceso no disponible"))

            val fotoPart = photoUri?.let { createMultipartBodyPart("foto_perfil", it) }
            val firmaPart = signatureUri?.let { createMultipartBodyPart("firma", it) }

            val bearerToken = "Bearer $accessToken"
            val response = ApiClient.apiService.updateProfile(bearerToken, fotoPart, firmaPart)
            
            if (response.isSuccessful) {
                response.body()?.let { 
                    // Actualizar cache con los nuevos datos
                    val userId = getCurrentUserId()
                    cacheManager.cacheUserProfile(USER_PROFILE_CACHE_KEY, it, userId)
                    saveUserProfileFallbackToDataStore(it)
                    Result.Success(it) 
                } ?: Result.Error(IOException("La respuesta de la API no contiene un perfil actualizado."))
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "Las imágenes seleccionadas no son válidas"
                    401 -> "Tu sesión ha expirado. Inicia sesión nuevamente"
                    413 -> "Las imágenes son demasiado grandes. Selecciona imágenes más pequeñas"
                    415 -> "Formato de imagen no soportado. Usa JPG o PNG"
                    500 -> "Error del servidor al procesar las imágenes"
                    else -> "Error al subir las imágenes. Intenta nuevamente"
                }
                Result.Error(IOException(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("timeout") == true -> "La subida tardó demasiado. Intenta con imágenes más pequeñas"
                e.message?.contains("network") == true -> "No se pudo conectar al servidor. Verifica tu conexión"
                else -> "Error al subir las imágenes. Verifica tu conexión"
            }
            Result.Error(IOException(errorMessage, e))
        }
    }

    /**
     * Actualiza los datos de texto del perfil email, contraseña
     * @param textData Un mapa con los campos a actualizar.
     * retorna Un objeto [Result] con el perfil actualizado o un error.
     */
    suspend fun updateProfileTextData(textData: Map<String, String>): Result<UserProfile> {
        return try {
            val accessToken = userPreferences.accessToken.first()
                ?: return Result.Error(IOException("Token de acceso no disponible"))
            
            val bearerToken = "Bearer $accessToken"
            val response = ApiClient.apiService.updateProfileTextData(bearerToken, textData)

            if (response.isSuccessful) {
                response.body()?.let { 
                    // Actualizar cache con los nuevos datos
                    val userId = getCurrentUserId()
                    cacheManager.cacheUserProfile(USER_PROFILE_CACHE_KEY, it, userId)
                    saveUserProfileFallbackToDataStore(it)
                    Result.Success(it) 
                } ?: Result.Error(IOException("La respuesta de la API no contiene un perfil actualizado."))
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMessage = when (response.code()) {
                    400 -> {
                        when {
                            errorBody.contains("password") || errorBody.contains("contraseña") -> 
                                "La contraseña actual no es correcta"
                            errorBody.contains("email") -> 
                                "El email ingresado no es válido o ya está en uso"
                            else -> "Los datos ingresados no son válidos"
                        }
                    }
                    401 -> "Tu sesión ha expirado. Inicia sesión nuevamente"
                    403 -> "No tienes permisos para realizar esta acción"
                    404 -> "No se encontró el perfil de usuario"
                    500 -> "Error interno del servidor. Intenta más tarde"
                    else -> "Error al actualizar el perfil. Intenta nuevamente"
                }
                Result.Error(IOException(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("timeout") == true -> "La operación tardó demasiado. Intenta nuevamente"
                e.message?.contains("network") == true -> "No se pudo conectar al servidor. Verifica tu conexión"
                else -> "No se pudo actualizar el perfil. Verifica tu conexión"
            }
            Result.Error(IOException(errorMessage, e))
        }
    }
    
    /**
     * Función de utilidad para crear un [MultipartBody.Part] a partir de una [Uri].
     * @param partName El nombre del campo en la petición multipart (e.g., "foto_perfil").
     * @param uri La [Uri] del archivo a subir.
     * @return El [MultipartBody.Part] creado, o null si la URI no se pudo procesar.
     */
    private fun createMultipartBodyPart(partName: String, uri: Uri): MultipartBody.Part? {
        return try {
            val file = createTempFileFromUri(uri)
                ?: throw IOException("No se pudo crear el archivo temporal desde la URI.")
            
            val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
            MultipartBody.Part.createFormData(partName, file.name, requestFile)
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Crea un archivo temporal en el directorio de caché de la app a partir de una Uri.
     *
     * @param uri La [Uri] del contenido a copiar.
     * @return El [File] temporal creado, o null en caso de error.
     */
    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val stream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return null
            val file = File.createTempFile("temp_image_", ".jpg", context.cacheDir)
            
            FileOutputStream(file).use { outputStream ->
                stream.use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Proporciona la URL base para construir las rutas completas de las imágenes.
     retorna La URL base del servidor de medios.
     */
    fun getBaseUrl(): String {
        return ApiConfig.MEDIA_URL
    }
    
    /**
     *  Limpia el cache híbrido de perfil de usuario.
     */
    suspend fun clearCache() {
        val userId = getCurrentUserId()
        cacheManager.invalidateUserProfile(userId)
        userPreferences.clearUserProfileCache()
        Log.d(TAG, "User profile cache cleared")
    }
    
    // ================= MÉTODOS PRIVADOS =================
    
    /**
     * Obtiene el ID del usuario actual.
     */
    private suspend fun getCurrentUserId(): String {
        return userPreferences.userDni.first() ?: "default_user"
    }
    
    /**
     * Obtiene un token válido o null si no está disponible.
     */
    private suspend fun getValidToken(): String? {
        return userPreferences.accessToken.first()
    }
    
    /**
     * Guarda el perfil en DataStore como fallback.
     */
    private suspend fun saveUserProfileFallbackToDataStore(userProfile: UserProfile) {
        try {
            val gson = Gson()
            val profileJson = gson.toJson(userProfile)
            userPreferences.saveUserProfileCache(profileJson)
            Log.d(TAG, "User profile fallback saved to DataStore")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile fallback: ${e.message}")
        }
    }
    
    /**
     * Obtiene el perfil desde DataStore fallback.
     */
    private suspend fun getUserProfileFallbackFromDataStore(): UserProfile? {
        return try {
            val profileJson = userPreferences.getUserProfileCache().first()
            if (profileJson != null) {
                val gson = Gson()
                gson.fromJson(profileJson, UserProfile::class.java)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing user profile fallback: ${e.message}")
            null
        }
    }
} 