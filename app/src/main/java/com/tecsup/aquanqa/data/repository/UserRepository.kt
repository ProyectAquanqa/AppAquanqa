package com.tecsup.aquanqa.data.repository

import android.content.Context
import android.net.Uri
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.api.ApiConfig
import com.tecsup.aquanqa.data.model.UserProfile
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Repositorio para manejar la información del perfil del usuario.
 * Abstrae el origen de los datos (API, preferencias) y proporciona
 * una interfaz limpia para que los ViewModels interactúen.
 */
class UserRepository(
    private val context: Context,
    private val userPreferences: UserPreferences
) {

    /**
     * Obtiene el perfil del usuario autenticado desde la API.
     *
     * @return Un objeto [Result] que contiene el [UserProfile] en caso de éxito,
     * o una [IOException] en caso de error.
     */
    suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val accessToken = userPreferences.accessToken.first()
                ?: return Result.Error(IOException("Token de acceso no disponible"))
            
            val bearerToken = "Bearer $accessToken"
            val response = ApiClient.apiService.getUserProfile(bearerToken)
            
            if (response.isSuccessful) {
                response.body()?.let { Result.Success(it) } 
                    ?: Result.Error(IOException("La respuesta de la API no contiene un perfil."))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                Result.Error(IOException("Error al obtener perfil: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.Error(IOException("No se pudo conectar al servidor.", e))
        }
    }
    
    /**
     * Actualiza el perfil del usuario, permitiendo cambiar la foto y/o la firma.
     *
     * @param photoUri La [Uri] de la nueva foto de perfil (opcional).
     * @param signatureUri La [Uri] de la nueva firma (opcional).
     * @return Un objeto [Result] con el perfil actualizado o un error.
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
                response.body()?.let { Result.Success(it) }
                    ?: Result.Error(IOException("La respuesta de la API no contiene un perfil actualizado."))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                Result.Error(IOException("Error al actualizar el perfil: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.Error(IOException("No se pudo conectar al servidor para actualizar el perfil.", e))
        }
    }

    /**
     * Actualiza los datos de texto del perfil (e.g., email).
     *
     * @param textData Un mapa con los campos a actualizar.
     * @return Un objeto [Result] con el perfil actualizado o un error.
     */
    suspend fun updateProfileTextData(textData: Map<String, String>): Result<UserProfile> {
        return try {
            val accessToken = userPreferences.accessToken.first()
                ?: return Result.Error(IOException("Token de acceso no disponible"))
            
            val bearerToken = "Bearer $accessToken"
            val response = ApiClient.apiService.updateProfileTextData(bearerToken, textData)

            if (response.isSuccessful) {
                response.body()?.let { Result.Success(it) }
                    ?: Result.Error(IOException("La respuesta de la API no contiene un perfil actualizado."))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                Result.Error(IOException("Error al actualizar datos: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.Error(IOException("No se pudo conectar al servidor para actualizar los datos.", e))
        }
    }
    
    /**
     * Función de utilidad para crear un [MultipartBody.Part] a partir de una [Uri].
     *
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
     *
     * @return La URL base del servidor de medios.
     */
    fun getBaseUrl(): String {
        return ApiConfig.MEDIA_URL
    }
} 