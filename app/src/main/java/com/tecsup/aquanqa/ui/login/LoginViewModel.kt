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
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de login
 */
class LoginViewModel(private val loginRepository: LoginRepository, private val context: Context) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    /**
     * Intenta iniciar sesión con DNI y contraseña
     */
    fun login(dni: String, password: String) {
        // Lanzar una corrutina para la operación de red
        viewModelScope.launch {
            try {
                val result = loginRepository.login(dni, password)

                if (result is Result.Success) {
                    _loginResult.value = LoginResult(success = LoggedInUserView(displayName = result.data.displayName))
                } else {
                    val error = (result as Result.Error).exception.message ?: "Error desconocido"
                    Log.e("LoginViewModel", "Error de login: $error")
                    _loginResult.value = LoginResult(error = error)
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Excepción durante login", e)
                _loginResult.value = LoginResult(error = e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Valida el formulario de login
     */
    fun loginDataChanged(dni: String, password: String) {
        if (!isDniValid(dni)) {
            _loginForm.value = LoginFormState(dniError = context.getString(R.string.invalid_dni))
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = context.getString(R.string.invalid_password_hint))
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // Validación simple de DNI
    private fun isDniValid(dni: String): Boolean {
        return dni.length == 8 && dni.all { it.isDigit() }
    }

    // Validación simple de contraseña
    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }
}

/**
 * Estado del formulario de login
 */
data class LoginFormState(
    val dniError: String? = null,
    val passwordError: String? = null,
    val isDataValid: Boolean = false
)

/**
 * Resultado de la autenticación
 */
data class LoginResult(
    val success: LoggedInUserView? = null,
    val error: String? = null
)

/**
 * Vista del usuario autenticado
 */
data class LoggedInUserView(
    val displayName: String
) 