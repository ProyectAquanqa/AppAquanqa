package com.tecsup.aquanqa.data

import com.tecsup.aquanqa.data.api.RetrofitClient
import com.tecsup.aquanqa.data.model.LoggedInUser
import com.tecsup.aquanqa.data.model.LoginRequest
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Clase que maneja la autenticación con credenciales de login y recupera información del usuario.
 */
class LoginDataSource(private val userPreferences: UserPreferences) {

    /**
     * Intenta autenticar al usuario con la API.
     * @param dni DNI del usuario
     * @param password Contraseña del usuario
     * @return Resultado con información del usuario o error
     */
    suspend fun login(dni: String, password: String): Result<LoggedInUser> {
        return withContext(Dispatchers.IO) {
            try {
                val loginRequest = LoginRequest(username = dni, password = password)
                val response = RetrofitClient.apiService.login(loginRequest)
                
                if (response.isSuccessful) {
                    val tokenResponse = response.body()
                    if (tokenResponse != null) {
                        // Guardar tokens en DataStore
                        userPreferences.saveTokens(
                            accessToken = tokenResponse.access,
                            refreshToken = tokenResponse.refresh
                        )
                        userPreferences.saveUserDni(dni)
                        
                        // Crear objeto de usuario autenticado
                        val user = LoggedInUser(
                            userId = dni,
                            displayName = dni // Por ahora usamos el DNI como nombre, luego se puede actualizar con el perfil
                        )
                        
                        Result.Success(user)
                    } else {
                        Result.Error(IOException("Respuesta vacía del servidor"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Result.Error(IOException("Error de autenticación: $errorBody"))
                }
            } catch (e: Exception) {
                Result.Error(IOException("Error al iniciar sesión", e))
            }
        }
    }

    /**
     * Cierra la sesión del usuario
     */
    suspend fun logout() {
        userPreferences.clear()
    }
}