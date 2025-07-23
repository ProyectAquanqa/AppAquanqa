package com.tecsup.aquanqa.data.api

import android.content.Context

object ApiClient {
    private var instance: RetrofitClient? = null

    fun getClient(context: Context): RetrofitClient {
        return instance ?: synchronized(this) {
            instance ?: RetrofitClient(context).also { instance = it }
        }
    }

    val chatbotApiService: ChatbotApiService
        get() = instance?.chatbotApiService ?: throw IllegalStateException("ApiClient not initialized")
    
    val apiService: ApiService
        get() = instance?.apiService ?: throw IllegalStateException("ApiClient not initialized")
} 