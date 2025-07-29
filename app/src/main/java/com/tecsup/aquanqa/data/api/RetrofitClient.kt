package com.tecsup.aquanqa.data.api

import android.content.Context
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient(private val context: Context) {

    private val userPreferences = UserPreferences(context)

    // Interceptor avanzado para manejar autenticación y renovación de tokens
    private val authInterceptor = AuthInterceptor(context)

    // Configurar interceptor de logging con máximo detalle
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Cliente HTTP con tiempos de espera más largos para carga de archivos
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(ApiConfig.TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)     // Tiempo de lectura más largo
        .writeTimeout(60, TimeUnit.SECONDS)    // Tiempo de escritura más largo para subidas
        .build()

    // Cliente Retrofit configurado
    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // Servicios API expuestos
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val chatbotApiService: ChatbotApiService by lazy {
        retrofit.create(ChatbotApiService::class.java)
    }
} 