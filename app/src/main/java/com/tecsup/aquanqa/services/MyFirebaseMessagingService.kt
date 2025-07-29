package com.tecsup.aquanqa.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tecsup.aquanqa.MainActivity
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.data.FirebaseManager
import com.tecsup.aquanqa.data.SessionManager
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Servicio de Firebase Cloud Messaging para manejar notificaciones push
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "aquanqa_notifications"
        private const val CHANNEL_NAME = "Notificaciones Aquanqa"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * Maneja la recepción de nuevos tokens FCM
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM recibido")
        
        saveTokenLocally(token)
        sendTokenToServer(token)
    }

    /**
     * Maneja los mensajes recibidos cuando la app está en foreground
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Verificar si el mensaje contiene una notificación
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "Aquanqa",
                body = notification.body ?: "Nueva notificación",
                data = remoteMessage.data
            )
        } ?: run {
            // Si no hay notification payload, crear una con los datos
            val title = remoteMessage.data["titulo"] ?: "Aquanqa"
            val body = remoteMessage.data["mensaje"] ?: "Nueva notificación"
            showNotification(title, body, remoteMessage.data)
        }
    }

    /**
     * Crea el canal de notificaciones para Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones de eventos Aquanqa"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Muestra una notificación local
     */
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Agregar datos extra si es necesario
            data["evento_id"]?.let { eventoId ->
                putExtra("evento_id", eventoId)
                putExtra("from_notification", true)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Guarda el token FCM localmente usando DataStore
     */
    private fun saveTokenLocally(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userPreferences = UserPreferences(applicationContext)
                userPreferences.saveFcmToken(token)
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando token localmente", e)
            }
        }
    }

    /**
     * Envía el token al servidor
     */
    private fun sendTokenToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userPreferences = UserPreferences(applicationContext)
                val sessionManager = SessionManager(applicationContext, userPreferences)
                val firebaseManager = FirebaseManager(applicationContext, userPreferences, sessionManager)
                
                firebaseManager.registerTokenWithServer(token)
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando token al servidor", e)
            }
        }
    }
} 