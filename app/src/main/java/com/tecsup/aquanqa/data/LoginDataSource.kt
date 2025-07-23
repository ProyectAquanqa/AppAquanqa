package com.tecsup.aquanqa.data

import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.model.LoggedInUser
import com.tecsup.aquanqa.data.model.LoginRequest
import com.tecsup.aquanqa.data.model.LoginResponse
import com.tecsup.aquanqa.data.model.UserResponse
import com.tecsup.aquanqa.data.preferences.UserPreferences
import java.io.IOException

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
                    // Descomponer la respuesta para mayor claridad
                    val userData: UserResponse = loginResponse.user
                    val accessToken: String = loginResponse.access
                    val refreshToken: String = loginResponse.refresh

                    // Guardar tokens y DNI del usuario
                    userPreferences.saveTokens(accessToken, refreshToken)
                    userPreferences.saveUserDni(userData.dni)
                    
                    val loggedInUser = LoggedInUser(
                        userId = userData.id.toString(),
                        displayName = userData.firstName,
                        token = accessToken
                    )
                    return Result.Success(loggedInUser)
                } else {
                    return Result.Error(IOException("Error logging in: Empty response body"))
                }
            } else {
                return Result.Error(IOException("Error logging in: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
            return Result.Error(IOException("Error logging in", e))
            }
        }

    suspend fun logout() {
        userPreferences.clear()
    }
}