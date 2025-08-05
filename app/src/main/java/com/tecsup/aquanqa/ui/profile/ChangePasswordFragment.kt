package com.tecsup.aquanqa.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputLayout
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.databinding.BottomSheetChangePasswordBinding
import com.tecsup.aquanqa.data.model.user.PasswordChangeData

/**
 * Bottom Sheet Fragment para cambiar la contraseña del usuario.
 *
 * Características principales:
 * - Validación de contraseña actual
 * - Validación de nueva contraseña y confirmación
 * - Callback para actualizar el campo original
 * - Interfaz Material Design con estilos reutilizados
 */
class ChangePasswordFragment : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "PasswordChangeBottomSheet"
        private const val MIN_PASSWORD_LENGTH = 6

        /**
         * Crea una nueva instancia del bottom sheet
         */
        fun newInstance(): ChangePasswordFragment {
            return ChangePasswordFragment()
        }
    }

    private var _binding: BottomSheetChangePasswordBinding? = null
    private val binding get() = _binding!!

    // Callback para notificar cuando se guarda la nueva contraseña
    private var onPasswordSavedListener: ((PasswordChangeData) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupPasswordValidation()
    }

    /**
     * Establece el listener para cuando se guarda la contraseña
     */
    fun setOnPasswordSavedListener(listener: (PasswordChangeData) -> Unit) {
        onPasswordSavedListener = listener
    }

    private fun setupListeners() {
        // Configurar botón guardar
        binding.savePasswordButton.setOnClickListener {
            if (validateAllFields()) {
                val currentPassword = binding.currentPasswordEditText.text.toString().trim()
                val newPassword = binding.newPasswordEditText.text.toString().trim()

                // Crear objeto con los datos del cambio de contraseña
                val passwordChangeData = PasswordChangeData(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )

                // Notificar al listener
                onPasswordSavedListener?.invoke(passwordChangeData)

                // Mostrar mensaje de éxito
                Toast.makeText(
                    requireContext(),
                    "Contraseña actualizada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                // Cerrar el bottom sheet
                dismiss()
            }
        }
    }

    private fun setupPasswordValidation() {
        // Validación en tiempo real para nueva contraseña
        binding.newPasswordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateNewPassword()
            }
        }

        // Validación en tiempo real para confirmar contraseña
        binding.confirmPasswordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validatePasswordConfirmation()
            }
        }
    }

    /**
     * Valida todos los campos del formulario
     */
    private fun validateAllFields(): Boolean {
        var isValid = true

        // Validar contraseña actual
        if (!validateCurrentPassword()) {
            isValid = false
        }

        // Validar nueva contraseña
        if (!validateNewPassword()) {
            isValid = false
        }

        // Validar confirmación de contraseña
        if (!validatePasswordConfirmation()) {
            isValid = false
        }

        return isValid
    }

    /**
     * Valida la contraseña actual
     */
    private fun validateCurrentPassword(): Boolean {
        val currentPassword = binding.currentPasswordEditText.text.toString().trim()

        return when {
            currentPassword.isEmpty() -> {
                setInputLayoutError(binding.currentPasswordInputLayout, "La contraseña actual es requerida")
                false
            }
            else -> {
                clearInputLayoutError(binding.currentPasswordInputLayout)
                true
            }
        }
    }

    /**
     * Valida la nueva contraseña
     */
    private fun validateNewPassword(): Boolean {
        val newPassword = binding.newPasswordEditText.text.toString().trim()

        return when {
            newPassword.isEmpty() -> {
                setInputLayoutError(binding.newPasswordInputLayout, "La nueva contraseña es requerida")
                false
            }
            newPassword.length < MIN_PASSWORD_LENGTH -> {
                setInputLayoutError(
                    binding.newPasswordInputLayout,
                    "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres"
                )
                false
            }
            else -> {
                clearInputLayoutError(binding.newPasswordInputLayout)
                true
            }
        }
    }

    /**
     * Valida la confirmación de contraseña
     */
    private fun validatePasswordConfirmation(): Boolean {
        val newPassword = binding.newPasswordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        return when {
            confirmPassword.isEmpty() -> {
                setInputLayoutError(binding.confirmPasswordInputLayout, "Confirma la nueva contraseña")
                false
            }
            newPassword != confirmPassword -> {
                setInputLayoutError(binding.confirmPasswordInputLayout, "Las contraseñas no coinciden")
                false
            }
            else -> {
                clearInputLayoutError(binding.confirmPasswordInputLayout)
                true
            }
        }
    }

    /**
     * Establece un mensaje de error en un TextInputLayout
     */
    private fun setInputLayoutError(inputLayout: TextInputLayout, message: String) {
        inputLayout.error = message
        inputLayout.isErrorEnabled = true
    }

    /**
     * Limpia el mensaje de error de un TextInputLayout
     */
    private fun clearInputLayoutError(inputLayout: TextInputLayout) {
        inputLayout.error = null
        inputLayout.isErrorEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}