package com.tecsup.aquanqa

import android.app.Application
import com.tecsup.aquanqa.data.api.ApiClient

class AquanqaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar el ApiClient con el contexto de la aplicación
        ApiClient.getClient(this)
    }
} 