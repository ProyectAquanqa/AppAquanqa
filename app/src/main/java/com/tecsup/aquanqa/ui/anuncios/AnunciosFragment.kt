package com.tecsup.aquanqa.ui.anuncios

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tecsup.aquanqa.databinding.FragmentAnunciosBinding
import com.tecsup.aquanqa.ui.base.BaseFragment

/**
 * Fragmento que muestra la lista de anuncios.
 *
 * Sigue el patrón MVVM, observando los datos expuestos por `AnunciosViewModel`
 * y actualizando la UI en consecuencia. La UI consiste en un `RecyclerView`
 * que se puebla con los datos de los anuncios.
 */
class AnunciosFragment : BaseFragment<FragmentAnunciosBinding>() {

    // Instancia del ViewModel, delegada a la gestión del ciclo de vida del fragmento.
    private val viewModel: AnunciosViewModel by viewModels()

    private lateinit var adapter: AnunciosAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAnunciosBinding {
        return FragmentAnunciosBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        super.setupUI()
        setupRecyclerView()
    }

    override fun setupObservers() {
        super.setupObservers()
        observeViewModel()
    }

    /**
     * Inicializa el RecyclerView con un adapter vacío.
     */
    private fun setupRecyclerView() {
        adapter = AnunciosAdapter(emptyList())
        binding.rvAnuncios.adapter = adapter
    }

    /**
     * Configura los observadores para los LiveData del ViewModel.
     *
     * - `anuncios`: Cuando la lista de anuncios cambia, se actualiza el adapter.
     * - `isLoading`: Muestra u oculta una vista de carga (a implementar).
     * - `error`: Muestra un mensaje de error si la carga falla.
     */
    private fun observeViewModel() {
        viewModel.anuncios.observe(viewLifecycleOwner) { anuncios ->
            // Actualizar el adapter con la nueva lista de anuncios.
            adapter.updateData(anuncios)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Aquí puedes mostrar u ocultar un ProgressBar
            // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            showError(errorMsg)
        }
    }
} 