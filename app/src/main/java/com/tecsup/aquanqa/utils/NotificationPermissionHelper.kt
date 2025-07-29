package com.tecsup.aquanqa.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Helper para manejar permisos de notificaciones en Android 13+
 */
class NotificationPermissionHelper(private val activity: AppCompatActivity) {

    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var onPermissionResult: ((Boolean) -> Unit)? = null

    /**
     * Inicializa el launcher de permisos
     * Debe llamarse en onCreate() de la Activity
     */
    fun initialize() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            onPermissionResult?.invoke(isGranted)
        }
    }

    /**
     * Verifica si el permiso de notificaciones está concedido
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // En versiones anteriores a Android 13, el permiso se concede automáticamente
            true
        }
    }

    /**
     * Solicita el permiso de notificaciones si es necesario
     */
    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        onPermissionResult = onResult

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasNotificationPermission()) {
                onResult(true)
            } else {
                permissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onResult(true)
        }
    }

    /**
     * Verifica y solicita permisos si es necesario
     */
    fun checkAndRequestPermission(onResult: (Boolean) -> Unit) {
        if (hasNotificationPermission()) {
            onResult(true)
        } else {
            requestNotificationPermission(onResult)
        }
    }
} 