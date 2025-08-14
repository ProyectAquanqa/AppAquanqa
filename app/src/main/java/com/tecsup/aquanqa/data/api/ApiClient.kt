package com.tecsup.aquanqa.data.api

import android.content.Context

/**
 * Singleton que gestiona instancias de RetrofitClient y proporciona
 * acceso a los servicios API de la aplicación.
 */
object ApiClient {

    private var instance: RetrofitClient? = null

    /**
     * Obtiene o crea una instancia de RetrofitClient.
     * 
     * @param context Contexto de la aplicación
     * @return Instancia de RetrofitClient
     */
    fun getClient(context: Context): RetrofitClient {
        return instance ?: synchronized(this) {
            instance ?: RetrofitClient(context).also { instance = it }
        }
    }

    /**
     * Acceso al servicio de chatbot.
     * @throws IllegalStateException Si ApiClient no ha sido inicializado
     */
    val chatbotApiService: ChatbotApiService
        get() = instance?.chatbotApiService ?: throw IllegalStateException("ApiClient not initialized")
    
    /**
     * Acceso al servicio API principal.
     * @throws IllegalStateException Si ApiClient no ha sido inicializado
     */
    val apiService: ApiService
        get() = instance?.apiService ?: throw IllegalStateException("ApiClient not initialized")
}