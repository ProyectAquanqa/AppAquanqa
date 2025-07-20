package com.tecsup.aquanqa.ui.access

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AccessViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Access Fragment"
    }
    val text: LiveData<String> = _text
} 