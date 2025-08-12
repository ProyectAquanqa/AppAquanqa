package com.tecsup.aquanqa.ui.event

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.model.content.Anuncio
import com.tecsup.aquanqa.data.repository.AnunciosRepository
import kotlinx.coroutines.launch

class EventDetailViewModel(
    private val repository: AnunciosRepository
) : ViewModel() {

    private val _eventoState = MutableLiveData<Result<Anuncio>>()
    val eventoState: LiveData<Result<Anuncio>> = _eventoState

    fun loadEvento(eventoId: Int) {
        viewModelScope.launch {
            _eventoState.value = Result.Loading
            when (val result = repository.getEventoById(eventoId)) {
                is Result.Success -> _eventoState.value = result
                is Result.Error -> _eventoState.value = result
                is Result.Loading -> { /* ignore */ }
            }
        }
    }
}



