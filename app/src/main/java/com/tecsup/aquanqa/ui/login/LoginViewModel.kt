package com.tecsup.aquanqa.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.data.LoginRepository
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.network.InvalidPasswordException
import com.tecsup.aquanqa.data.network.UserNotFoundException
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de login
 * Maneja la lógica de negocio relacionada con la autenticación
 */
class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    /**
     * Constantes para mensajes de error
     */
    companion object {
        const val ERROR_USER_NOT_FOUND = "Usuario no registrado"
        const val ERROR_INVALID_PASSWORD = "Contraseña incorrecta"
        const val ERROR_UNKNOWN = "Error desconocido"
        private const val TAG = "LoginViewModel"
    }

    /**
     * Intenta iniciar sesión con DNI y contraseña
     * @param dni DNI del usuario
     * @param password Contraseña del usuario
     */
    fun login(dni: String, password: String) {
        // Lanzar una corrutina para la operación de red
        viewModelScope.launch {
            try {
                val result = loginRepository.login(dni, password)

                if (result is Result.Success) {
                    Log.d(TAG, "Login exitoso para usuario: ${result.data.displayName}")
                    _loginResult.value = LoginResult(success = LoggedInUserView(displayName = result.data.displayName))
                } else {
                    // Manejar diferentes tipos de errores
                    val exception = (result as Result.Error).exception
                    
                    // Log detallado del error
                    Log.e(TAG, "Error en login. Tipo de excepción: ${exception.javaClass.simpleName}")
                    
                    // Verificar primero si el usuario existe, siguiendo la prioridad indicada
                    val errorMessage = when (exception) {
                        is UserNotFoundException -> {
                            Log.e(TAG, "Usuario no encontrado: $dni")
                            ERROR_USER_NOT_FOUND
                        }
                        is InvalidPasswordException -> {
                            Log.e(TAG, "Contraseña incorrecta para: $dni")
                            ERROR_INVALID_PASSWORD
                        }
                        else -> {
                            Log.e(TAG, "Error desconocido: ${exception.message}")
                            exception.message ?: ERROR_UNKNOWN
                        }
                    }
                    
                    Log.e(TAG, "Error final de login: $errorMessage", exception)
                    _loginResult.value = LoginResult(error = errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción no controlada durante login", e)
                _loginResult.value = LoginResult(error = e.message ?: ERROR_UNKNOWN)
            }
        }
    }

    /**
     * Valida el formulario de login al presionar el botón de login.
     * Actualiza el `loginFormState` con los errores de validación si los hay.
     * @return true si el formulario es válido, false en caso contrario.
     */
    fun validateForm(dni: String, password: String): Boolean {
        val dniError = if (!isDniValid(dni)) R.string.invalid_dni else null
        val passwordError = if (!isPasswordValid(password)) R.string.invalid_password_hint else null

        val isDataValid = dniError == null && passwordError == null

        _loginForm.value = LoginFormState(
            dniError = dniError,
            passwordError = passwordError,
            isDataValid = isDataValid
        )

        return isDataValid
    }

    /**
     * Valida que el DNI tenga el formato correcto
     * @param dni DNI a validar
     * @return true si el DNI es válido, false en caso contrario
     */
    private fun isDniValid(dni: String): Boolean {
        return dni.length == 8 && dni.all { it.isDigit() }
    }

    /**
     * Valida que la contraseña cumpla con los requisitos mínimos
     * @param password Contraseña a validar
     * @return true si la contraseña es válida, false en caso contrario
     */
    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }
}

/**
 * Estado del formulario de login
 * @param dniError ID del recurso de string con el error del DNI, o null si no hay error
 * @param passwordError ID del recurso de string con el error de la contraseña, o null si no hay error
 * @param isDataValid true si los datos son válidos, false en caso contrario
 */
data class LoginFormState(
    val dniError: Int? = null,
    val passwordError: Int? = null,
    val isDataValid: Boolean = false
)

/**
 * Resultado de la autenticación
 * @param success Datos del usuario autenticado, o null si la autenticación falló
 * @param error Mensaje de error, o null si la autenticación fue exitosa
 */
data class LoginResult(
    val success: LoggedInUserView? = null,
    val error: String? = null
)

/**
 * Vista del usuario autenticado
 * @param displayName Nombre a mostrar del usuario
 */
data class LoggedInUserView(
    val displayName: String
) 