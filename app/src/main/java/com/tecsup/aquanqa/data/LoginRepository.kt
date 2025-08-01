package com.tecsup.aquanqa.data

import android.content.Context
import com.tecsup.aquanqa.data.model.LoggedInUser
import com.tecsup.aquanqa.data.model.LoginRequest
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Clase que solicita autenticación e información del usuario de la fuente de datos remota y
 * mantiene un caché en memoria con el estado de inicio de sesión y la información de las credenciales del usuario.
 */
class LoginRepository(
    private val dataSource: LoginDataSource,
    private val userPreferences: UserPreferences,
    private val context: Context? = null
) {
    var user: LoggedInUser? = null
        private set

    private val sessionManager: SessionManager? = context?.let { 
        SessionManager(it, userPreferences) 
    }

    val isLoggedIn: Boolean
        get() = user != null

    init {
        user = null
    }

    suspend fun hasAccessToken(): Flow<Boolean> {
        return userPreferences.accessToken.map { token ->
            !token.isNullOrEmpty()
        }
    }

    // Verifica si hay una sesión activa usando SessionManager

    suspend fun isSessionActive(): Boolean {
        return sessionManager?.isSessionActive() ?: false
    }

    // Cierra la sesión del usuario y limpia todos los datos

    suspend fun logout() {
        user = null
        sessionManager?.clearSession() ?: dataSource.logout()
    }

    suspend fun login(dni: String, password: String): Result<LoggedInUser> {
        val loginRequest = LoginRequest(username = dni, password = password)
        val result = dataSource.login(loginRequest)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }

        return result
    }

    // Obtiene un token de acceso válido, refrescándolo si es necesario

    suspend fun getValidAccessToken(): String? {
        return sessionManager?.getValidAccessToken()
    }

    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
    }
}