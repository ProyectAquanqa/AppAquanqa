package com.tecsup.aquanqa.data

import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.model.auth.LoggedInUser
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.tecsup.aquanqa.data.api.ApiService
import com.tecsup.aquanqa.data.api.ApiConfig
import com.tecsup.aquanqa.data.model.auth.LoginRequest
import com.tecsup.aquanqa.data.model.auth.LoginResponse
import com.tecsup.aquanqa.data.model.user.UserProfile
import com.tecsup.aquanqa.data.model.auth.UserResponse
import com.tecsup.aquanqa.data.network.InvalidPasswordException
import com.tecsup.aquanqa.data.network.UserNotFoundException
import com.tecsup.aquanqa.data.network.NetworkException
import com.tecsup.aquanqa.data.network.ServerUnavailableException
import com.tecsup.aquanqa.data.preferences.UserPreferences
import java.io.IOException
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.net.ConnectException
import org.json.JSONObject
import android.util.Log

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource(private val userPreferences: UserPreferences) {

    // Cliente especializado para login con timeouts cortos para fallar rápido
    private val quickLoginClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)    // Solo 5 segundos para conectar al servidor
        .readTimeout(15, TimeUnit.SECONDS)      // Solo 15 segundos para leer
        .writeTimeout(10, TimeUnit.SECONDS)     // Solo 10 segundos para escribir
        .retryOnConnectionFailure(false)        // Sin reintentos automáticos
        .build()

    private val quickLoginService = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(quickLoginClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    suspend fun login(loginRequest: LoginRequest): Result<LoggedInUser> {
            try {

            val response = quickLoginService.login(loginRequest)
            Log.d("LoginDataSource", "Respuesta recibida: ${response.code()}")
                
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
                        val profileResponse = quickLoginService.getUserProfile("Bearer $accessToken")
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
                Log.d("LoginDataSource", "Error de login - Código: ${response.code()}, Body: $errorBody")

                return when (response.code()) {
                    400, 401 -> {
                        // Analizar el mensaje específico del backend (puede ser 400 o 401)
                        val errorMessage = errorBody?.lowercase() ?: ""
                        Log.d("LoginDataSource", "Analizando error ${response.code()}: $errorMessage")
                        
                        when {
                            errorMessage.contains("usuario no registrado") || 
                            errorMessage.contains("cuenta desactivada") -> {
                                Log.d("LoginDataSource", "Usuario no registrado o desactivado")
                                Result.Error(UserNotFoundException())
                            }
                            errorMessage.contains("contraseña incorrecta") -> {
                                Log.d("LoginDataSource", "Contraseña incorrecta")
                                Result.Error(InvalidPasswordException())
                            }
                            else -> {
                                Log.w("LoginDataSource", "Mensaje de error no específico: $errorMessage")
                                Result.Error(InvalidPasswordException()) // Fallback seguro
                            }
                        }
                    }
                    404 -> {
                        Log.d("LoginDataSource", "Error 404: Usuario no encontrado")
                        Result.Error(UserNotFoundException())
                    }
                    else -> {
                        Log.e("LoginDataSource", "Error HTTP ${response.code()}: ${response.message()}")
                        Result.Error(IOException("Ocurrió un problema durante el inicio de sesión. Intenta nuevamente"))
                    }
                }
                }
            } catch (e: Exception) {
            Log.e("LoginDataSource", "Error de red en login: ${e.javaClass.simpleName} - ${e.message}")
            
            // Clasificar errores de red
            return when (e) {
                is UnknownHostException, is ConnectException, is java.net.NoRouteToHostException -> {
                    Log.d("LoginDataSource", "Sin conexión a internet")
                    Result.Error(NetworkException())
                }
                is SocketTimeoutException, is java.net.PortUnreachableException -> {
                    Log.d("LoginDataSource", "Servidor no disponible")
                    Result.Error(ServerUnavailableException())
                }
                is IOException -> {
                    val message = e.message?.lowercase() ?: ""
                    if (message.contains("failed to connect") || message.contains("connection refused") || message.contains("network is unreachable")) {
                        Result.Error(NetworkException())
                    } else if (message.contains("timeout")) {
                        Result.Error(ServerUnavailableException())
                    } else {
                        Result.Error(NetworkException())
                    }
                }
                else -> {
                    Log.e("LoginDataSource", "Error inesperado", e)
                    Result.Error(IOException("Error inesperado al intentar iniciar sesión.", e))
                }
            }
            }
        }

    suspend fun logout() {
        userPreferences.clear()
    }
}