package com.tecsup.aquanqa.ui.beneficios

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BeneficiosViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Este es el fragmento de Beneficios"
    }
    val text: LiveData<String> = _text
} 