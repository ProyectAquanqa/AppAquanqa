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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

import com.tecsup.aquanqa.MainActivity
import com.tecsup.aquanqa.R

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
        
        // Configurar la barra de estado para que sea transparente con íconos oscuros
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        
        setContentView(R.layout.activity_login)

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
        val layout = findViewById<View>(R.id.login_container).parent as androidx.constraintlayout.widget.ConstraintLayout
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

            // Deshabilitar el botón de login a menos que los datos sean válidos
            btnLogin.isEnabled = loginState.isDataValid

            // Mostrar errores de validación si los hay
            if (loginState.dniError != null) {
                dniLayout.error = loginState.dniError
            } else {
                dniLayout.error = null
            }

            if (loginState.passwordError != null) {
                passwordLayout.error = loginState.passwordError
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
                // Mostrar mensaje de error
                showLoginFailed(loginResult.error)
            }
            
            if (loginResult.success != null) {
                // Iniciar la actividad principal si el login es exitoso
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Cerrar la actividad de login
            }
        })

        // Configurar TextWatcher para validar los datos mientras se escriben
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                loginViewModel.loginDataChanged(
                    textDni.text.toString(),
                    textClave.text.toString()
                )
            }
        }
        
        textDni.addTextChangedListener(afterTextChangedListener)
        textClave.addTextChangedListener(afterTextChangedListener)
        
        // Configurar acción para el botón IME Done
        textClave.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && btnLogin.isEnabled) {
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
     */
    private fun attemptLogin() {
        // Mostrar indicador de progreso
        progressBar.visibility = View.VISIBLE
        
        // Deshabilitar el botón y los campos de entrada durante el login
        btnLogin.isEnabled = false
        textDni.isEnabled = false
        textClave.isEnabled = false
        
        // Iniciar el proceso de login
        loginViewModel.login(
            textDni.text.toString(),
            textClave.text.toString()
        )
    }

    /**
     * Muestra un mensaje de error al fallar el login
     */
    private fun showLoginFailed(errorMsg: String) {
        Toast.makeText(applicationContext, "Error de autenticación: $errorMsg", Toast.LENGTH_LONG).show()
    }
}
