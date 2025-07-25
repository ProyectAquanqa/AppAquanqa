package com.tecsup.aquanqa.data

import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.model.LoggedInUser
import com.tecsup.aquanqa.data.model.LoginRequest
import com.tecsup.aquanqa.data.model.LoginResponse
import com.tecsup.aquanqa.data.model.UserResponse
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