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

    private val authInterceptor = Interceptor { chain ->
        val token = runBlocking { userPreferences.accessToken.first() }
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "${ApiConfig.TOKEN_PREFIX}$token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .addInterceptor(authInterceptor)
            .connectTimeout(ApiConfig.TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.TIMEOUT, TimeUnit.SECONDS)
            .build()

    private val retrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
        .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val chatbotApiService: ChatbotApiService by lazy {
        retrofit.create(ChatbotApiService::class.java)
    }
} 