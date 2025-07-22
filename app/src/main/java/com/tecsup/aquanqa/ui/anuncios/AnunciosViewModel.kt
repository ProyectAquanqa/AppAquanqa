package com.tecsup.aquanqa.ui.anuncios

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AnunciosViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Esta es la sección de Anuncios"
    }
    val text: LiveData<String> = _text
} 