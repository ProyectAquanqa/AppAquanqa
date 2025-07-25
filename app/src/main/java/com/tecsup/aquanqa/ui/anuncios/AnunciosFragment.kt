package com.tecsup.aquanqa.ui.anuncios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tecsup.aquanqa.data.model.Anuncio
import com.tecsup.aquanqa.databinding.FragmentAnunciosBinding

/**
 * Fragmento que muestra la lista de anuncios.
 *
 * Sigue el patrón MVVM, observando los datos expuestos por `AnunciosViewModel`
 * y actualizando la UI en consecuencia. La UI consiste en un `RecyclerView`
 * que se puebla con los datos de los anuncios.
 */
class AnunciosFragment : Fragment() {

    private var _binding: FragmentAnunciosBinding? = null
    private val binding get() = _binding!!

    // Instancia del ViewModel, delegada a la gestión del ciclo de vida del fragmento.
    private val viewModel: AnunciosViewModel by viewModels()

    private lateinit var adapter: AnunciosAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnunciosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar el adapter del RecyclerView
        setupRecyclerView()

        // Observar los cambios en los datos del ViewModel
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
            if (errorMsg.isNotEmpty()) {
                // Mostrar un mensaje de error al usuario.
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 