package com.tecsup.aquanqa.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Clase para configurar y proporcionar instancias de Retrofit
 */
object RetrofitClient {
    
    /**
     * Crea y configura un cliente OkHttp con interceptores para logging
     */
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(ApiConfig.TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.TIMEOUT, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Crea y configura una instancia de Retrofit
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Proporciona una implementación de la interfaz ApiService
     */
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
} 