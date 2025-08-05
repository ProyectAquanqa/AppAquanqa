package com.tecsup.aquanqa.data

import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.model.auth.LoggedInUser
import com.tecsup.aquanqa.data.model.auth.LoginRequest
import com.tecsup.aquanqa.data.model.auth.LoginResponse
import com.tecsup.aquanqa.data.model.user.UserProfile
import com.tecsup.aquanqa.data.model.auth.UserResponse
import com.tecsup.aquanqa.data.network.InvalidPasswordException
import com.tecsup.aquanqa.data.network.UserNotFoundException
import com.tecsup.aquanqa.data.preferences.UserPreferences
import java.io.IOException
import org.json.JSONObject
import android.util.Log

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource(private val userPreferences: UserPreferences) {

    suspend fun login(loginRequest: LoginRequest): Result<LoggedInUser> {
            try {
            val response = ApiClient.apiService.login(loginRequest)
                
                if (response.isSuccessful) {
                val loginResponse: LoginResponse? = response.body()

                if (loginResponse != null) {
                    val userData: UserResponse = loginResponse.user
                    val accessToken: String = loginResponse.access
                    val refreshToken: String = loginResponse.refresh

                    userPreferences.saveTokens(accessToken, refreshToken)
                    userPreferences.saveUserDni(userData.dni)
                    
                    // Obtener el perfil completo del usuario después del login
                    try {
                        val profileResponse = ApiClient.apiService.getUserProfile("Bearer $accessToken")
                        if (profileResponse.isSuccessful && profileResponse.body() != null) {
                            val userProfile = profileResponse.body()!!
                            userPreferences.saveUserProfile(
                                firstName = userProfile.first_name,
                                lastName = userProfile.last_name,
                                photoUrl = userProfile.foto_perfil
                            )
                        } else {
                            // Si falla obtener el perfil, al menos guardamos el first_name del login
                            userPreferences.saveUserProfile(
                                firstName = userData.first_name,
                                lastName = ""
                            )
                        }
                    } catch (e: Exception) {
                        // Si falla obtener el perfil, al menos guarda el first_name del login
                        userPreferences.saveUserProfile(
                            firstName = userData.first_name,
                            lastName = ""
                        )
                    }
                    
                    val loggedInUser = LoggedInUser(
                        userId = userData.id.toString(),
                        displayName = userData.first_name,
                        token = accessToken
                    )
                    return Result.Success(loggedInUser)
                } else {
                    return Result.Error(IOException("Error logging in: Empty response body"))
                }
            } else {
                // MANEJO DE ERRORES BASADO EN CÓDIGO DE ESTADO
                val errorBody = response.errorBody()?.string()
                Log.d("LoginDataSource", "Error response: ${response.code()} - Body: $errorBody")

                return when (response.code()) {
                    404 -> {
                        Log.w("LoginDataSource", "Error 404 detectado: Usuario no encontrado.")
                        Result.Error(UserNotFoundException())
                    }
                    401 -> {
                        Log.w("LoginDataSource", "Error 401 detectado: Contraseña incorrecta.")
                        Result.Error(InvalidPasswordException())
                    }
                    else -> {
                        Log.e("LoginDataSource", "Error no manejado: ${response.code()}")
                        Result.Error(IOException("Error en el login: ${response.message()}"))
                    }
                }
                }
            } catch (e: Exception) {
            Log.e("LoginDataSource", "Login exception: ${e.message}", e)
            return Result.Error(IOException("Error de conexión al intentar iniciar sesión.", e))
            }
        }

    suspend fun logout() {
        userPreferences.clear()
    }
}