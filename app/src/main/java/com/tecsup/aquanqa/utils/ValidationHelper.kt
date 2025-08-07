package com.tecsup.aquanqa.utils

import android.util.Patterns

/**
 * Utilidad para validaciones de formularios con mensajes claros para el usuario.
 */
object ValidationHelper {

    private const val MIN_PASSWORD_LENGTH = 6

    /**
     * Valida si un email tiene el formato correcto.
     * @param email El email a validar
     * @return Pair<Boolean, String> - (esValido, mensajeError)
     */
    fun validateEmail(email: String): Pair<Boolean, String> {
        return when {
            email.isBlank() -> Pair(false, "El email es obligatorio")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> 
                Pair(false, "Ingresa un email válido (ejemplo: usuario@correo.com)")
            else -> Pair(true, "")
        }
    }

    /**
     * Valida si una contraseña cumple con los requisitos mínimos.
     * @param password La contraseña a validar
     * @return Pair<Boolean, String> - (esValida, mensajeError)
     */
    fun validatePassword(password: String): Pair<Boolean, String> {
        return when {
            password.isBlank() -> Pair(false, "La contraseña es obligatoria")
            password.length < MIN_PASSWORD_LENGTH -> 
                Pair(false, "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres")
            else -> Pair(true, "")
        }
    }

    /**
     * Valida si dos contraseñas coinciden.
     * @param password La contraseña original
     * @param confirmPassword La confirmación de contraseña
     * @return Pair<Boolean, String> - (coinciden, mensajeError)
     */
    fun validatePasswordConfirmation(password: String, confirmPassword: String): Pair<Boolean, String> {
        return when {
            confirmPassword.isBlank() -> Pair(false, "Confirma tu nueva contraseña")
            password != confirmPassword -> Pair(false, "Las contraseñas no coinciden")
            else -> Pair(true, "")
        }
    }

    /**
     * Valida si la contraseña actual no está vacía.
     * @param currentPassword La contraseña actual
     * @return Pair<Boolean, String> - (esValida, mensajeError)
     */
    fun validateCurrentPassword(currentPassword: String): Pair<Boolean, String> {
        return when {
            currentPassword.isBlank() -> Pair(false, "Ingresa tu contraseña actual")
            else -> Pair(true, "")
        }
    }
}