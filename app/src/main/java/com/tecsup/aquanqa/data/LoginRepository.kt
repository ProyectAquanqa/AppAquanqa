package com.tecsup.aquanqa.data

import com.tecsup.aquanqa.data.model.LoggedInUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * Clase que solicita autenticación e información del usuario desde la fuente de datos remota
 * y mantiene un caché en memoria del estado de inicio de sesión y la información de credenciales del usuario.
 */
class LoginRepository(
    private val dataSource: LoginDataSource,
    private val userPreferences: com.tecsup.aquanqa.data.preferences.UserPreferences
) {
    // Caché en memoria del objeto usuario conectado
    var user: LoggedInUser? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    /**
     * Comprueba si hay un token de acceso guardado
     * @return Flow que emite true si hay un token guardado, false en caso contrario
     */
    fun hasAccessToken(): Flow<Boolean> {
        return userPreferences.accessToken.map { token ->
            !token.isNullOrEmpty()
        }
    }

    init {
        // Si las credenciales de usuario se almacenarán en el almacenamiento local, se recomienda cifrarlas
        // @see https://developer.android.com/training/articles/keystore
        user = null
    }

    /**
     * Cierra la sesión del usuario
     */
    suspend fun logout() {
        user = null
        dataSource.logout()
    }

    /**
     * Inicia sesión con DNI y contraseña
     * @param dni DNI del usuario
     * @param password Contraseña del usuario
     * @return Resultado con información del usuario o error
     */
    suspend fun login(dni: String, password: String): Result<LoggedInUser> {
        // Maneja el inicio de sesión
        val result = dataSource.login(dni, password)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }

        return result
    }

    /**
     * Establece el usuario conectado en la caché en memoria
     * @param loggedInUser Usuario conectado
     */
    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
        // Si las credenciales de usuario se almacenarán en el almacenamiento local, se recomienda cifrarlas
        // @see https://developer.android.com/training/articles/keystore
    }
}