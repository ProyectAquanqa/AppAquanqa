package com.tecsup.aquanqa.ui.profile

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.tecsup.aquanqa.R

/**
 * Helper class para manejar campos del perfil de manera reutilizable
 */
object ProfileFieldHelper {
    
    /**
     * Configura un campo del perfil usando el layout reutilizable
     */
    fun setupProfileField(
        fieldView: View,
        label: String,
        value: String,
        @DrawableRes iconRes: Int? = null,
        showActionIcon: Boolean = false
    ) {
        // Configurar textos
        fieldView.findViewById<TextView>(R.id.fieldLabel)?.text = label.uppercase()
        fieldView.findViewById<TextView>(R.id.fieldValue)?.text = value
        
        // Configurar icono del campo
        val fieldIcon = fieldView.findViewById<ImageView>(R.id.fieldIcon)
        if (iconRes != null) {
            fieldIcon?.visibility = View.VISIBLE
            fieldIcon?.setImageResource(iconRes)
        } else {
            fieldIcon?.visibility = View.GONE
        }
        

    }
    
    /**
     * Data class para representar un campo del perfil
     */
    data class ProfileField(
        val label: String,
        val value: String,
        @DrawableRes val iconRes: Int? = null,
        val showActionIcon: Boolean = false,
        val isClickable: Boolean = false
    )
    
    /**
     * Configuración predefinida de campos del perfil
     */
    object FieldConfigs {
        fun getPersonalInfoFields(
            fullName: String,
            dni: String,
            email: String
        ): List<ProfileField> = listOf(
            ProfileField(
                label = "Nombres Completos",
                value = fullName,
                iconRes = R.drawable.ic_person_outline
            ),
            ProfileField(
                label = "DNI",
                value = dni,
                iconRes = R.drawable.ic_dni
            ),
            ProfileField(
                label = "Email",
                value = email,
                iconRes = R.drawable.ic_email,
                showActionIcon = true,
                isClickable = true
            )
        )
        
        fun getSecurityFields(hasPassword: Boolean): List<ProfileField> = listOf(
            ProfileField(
                label = "Contraseña",
                value = if (hasPassword) "••••••••" else "No configurada",
                iconRes = R.drawable.ic_lock_outline,
                showActionIcon = true,
                isClickable = true
            )
        )
    }
}

/**
 * Extension function para facilitar la configuración de campos
 */
fun View.setupAsProfileField(
    label: String,
    value: String,
    @DrawableRes iconRes: Int? = null,
    showActionIcon: Boolean = false
) {
    ProfileFieldHelper.setupProfileField(this, label, value, iconRes, showActionIcon)
}