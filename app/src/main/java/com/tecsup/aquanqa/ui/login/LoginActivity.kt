package com.tecsup.aquanqa.ui.login

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.graphics.Color
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController

import com.tecsup.aquanqa.MainActivity
import com.tecsup.aquanqa.R

/**
 * Actividad para la pantalla de inicio de sesión
 * Maneja la interfaz de usuario para el proceso de autenticación
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var progressBar: ProgressBar
    private lateinit var textDni: TextInputEditText
    private lateinit var textClave: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var dniLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)
        
        // Configurar status bar transparente después de setContentView
        setupTransparentStatusBar()

        // Inicializar ViewModel
        loginViewModel = ViewModelProvider(this, LoginViewModelFactory(this))
            .get(LoginViewModel::class.java)

        // Inicializar vistas
        textDni = findViewById(R.id.txtDni)
        textClave = findViewById(R.id.txtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        dniLayout = findViewById(R.id.dniInputLayout)
        passwordLayout = findViewById(R.id.passwordInputLayout)
        
        // Agregar ProgressBar para mostrar carga
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleLarge)
        progressBar.visibility = View.GONE
        val layout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_container)
        val params = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        params.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        layout.addView(progressBar, params)

        // Observar el estado del formulario de login
        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // Mostrar errores de validación si los hay
            if (loginState.dniError != null) {
                dniLayout.error = getString(loginState.dniError)
            } else {
                dniLayout.error = null
            }

            if (loginState.passwordError != null) {
                passwordLayout.error = getString(loginState.passwordError)
            } else {
                passwordLayout.error = null
            }
        })

        // Observar el resultado del login
        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            // Ocultar el indicador de progreso
            progressBar.visibility = View.GONE
            
            // Habilitar el botón de login y los campos de entrada
            btnLogin.isEnabled = true
            textDni.isEnabled = true
            textClave.isEnabled = true

            // Manejar el resultado del login
            if (loginResult.error != null) {
                // Mostrar mensaje de error específico
                showLoginFailed(loginResult.error)
            }
            
            if (loginResult.success != null) {
                // Iniciar la actividad principal si el login es exitoso
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Cerrar la actividad de login
            }
        })

        // Limpiar errores al empezar a escribir en los campos
        textDni.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Limpiar el error cuando el usuario empieza a escribir
                if (dniLayout.error != null) {
                    dniLayout.error = null
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        textClave.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Limpiar el error cuando el usuario empieza a escribir
                if (passwordLayout.error != null) {
                    passwordLayout.error = null
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Configurar acción para el botón IME Done
        textClave.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else {
                false
            }
        }

        // Configurar click listener para el botón de login
        btnLogin.setOnClickListener {
            attemptLogin()
        }
    }

    /**
     * Intenta iniciar sesión con los datos ingresados
     * Muestra un indicador de progreso y deshabilita los controles durante el proceso
     */
    private fun attemptLogin() {
        val dni = textDni.text.toString()
        val password = textClave.text.toString()

        // Validar el formulario antes de intentar el login
        if (!loginViewModel.validateForm(dni, password)) {
            return // Detener si el formulario no es válido
        }

        // Mostrar indicador de progreso
        progressBar.visibility = View.VISIBLE
        
        // Deshabilitar el botón y los campos de entrada durante el login
        btnLogin.isEnabled = false
        textDni.isEnabled = false
        textClave.isEnabled = false
        
        // Iniciar el proceso de login
        loginViewModel.login(dni, password)
    }

    /**
     * Muestra un mensaje de error específico según el tipo de error de autenticación
     * @param errorMsg El mensaje de error a mostrar
     */
    private fun showLoginFailed(errorMsg: String) {
        // Limpiar errores anteriores
        dniLayout.error = null
        passwordLayout.error = null

        // Determinar qué tipo de mensaje mostrar según el contenido
        when (errorMsg) {
            LoginViewModel.ERROR_USER_NOT_FOUND -> {
                // Mostrar error en el campo de DNI
                dniLayout.error = getString(R.string.error_user_not_found)
                // No verificar la contraseña si el usuario no existe
            }
            LoginViewModel.ERROR_INVALID_PASSWORD -> {
                // Mostrar error en el campo de contraseña
                passwordLayout.error = getString(R.string.error_invalid_password)
            }
            else -> {
                // Para otros errores, mostrar un mensaje genérico
                Snackbar.make(findViewById(android.R.id.content), errorMsg, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Configura el status bar como transparente para diferentes versiones de Android
     */
    private fun setupTransparentStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            window.setDecorFitsSystemWindows(false)
            window.statusBarColor = Color.TRANSPARENT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Android 5.0+ (API 21+)
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
            window.statusBarColor = Color.TRANSPARENT
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        // Restaurar status bar al salir
        restoreStatusBar()
    }

    /**
     * Restaura la configuración normal del status bar
     */
    private fun restoreStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
}
