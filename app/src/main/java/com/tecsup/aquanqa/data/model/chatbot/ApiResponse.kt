package com.tecsup.aquanqa.data.model.chatbot

import com.google.gson.annotations.SerializedName

/**
 * Wrapper para las respuestas del backend que incluye status y data
 */
data class ApiResponse<T>(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("data")
    val data: T?,
    
    @SerializedName("error")
    val error: String?
)