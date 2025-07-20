package com.tecsup.aquanqa.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tecsup.aquanqa.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel : ViewModel() {

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _currentDate = MutableLiveData<String>()
    val currentDate: LiveData<String> = _currentDate

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _publications = MutableLiveData<List<Publication>>()
    val publications: LiveData<List<Publication>> = _publications

    init {
        loadData()
    }

    private fun loadData() {
        // Cargar nombre de usuario (en una app real, esto vendría de una base de datos o preferencias)
        _userName.value = "Jhessica"

        // Cargar fecha actual formateada
        val dateFormat = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES"))
        _currentDate.value = dateFormat.format(Date()).replaceFirstChar { it.uppercase() }

        // Datos de ejemplo para las categorías
        _categories.value = listOf(
            Category("Todos", R.drawable.ic_home_black_24dp),
            Category("Anuncios", R.drawable.ic_notifications_black_24dp),
            Category("Eventos", R.drawable.ic_dashboard_black_24dp)
        )

        // Datos de ejemplo para las publicaciones
        _publications.value = listOf(
            Publication(
                title = "Visión de Futuro",
                iconResId = R.drawable.ic_dashboard_black_24dp,
                author = Author("Jose Manuel Leturia", "https://i.imgur.com/C4VWHHr.png"),
                date = "11 de julio 08:39 PM",
                content = "Felicitaciones al equipo de tesorería comandado por Nelida Cieza, en la gestión de líneas de crédito a corto plazo con los 4 bancos más importantes del país, cumpliendo la meta del primer semestre 2025. Gracias!!!!",
                team = listOf(
                    TeamMember("Nelida Rosa Cieza", "https://i.imgur.com/A4hW48b.png"),
                    TeamMember("Karina Lady Castro", "https://i.imgur.com/A4hW48b.png"),
                    TeamMember("Camila Briggitt Gonzales", "https://i.imgur.com/A4hW48b.png")
                )
            )
        )
    }
}