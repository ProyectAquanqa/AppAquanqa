package com.tecsup.aquanqa.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tecsup.aquanqa.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar mensaje de bienvenida con el nombre del usuario
        homeViewModel.userName.observe(viewLifecycleOwner) { userName ->
            binding.textWelcome.text = "¡Hola, $userName!"
        }

        // Configurar la fecha actual
        homeViewModel.currentDate.observe(viewLifecycleOwner) { currentDate ->
            binding.textWelcomeDate.text = currentDate
        }

        // Configurar adaptadores para los RecyclerViews
        homeViewModel.categories.observe(viewLifecycleOwner) { categories ->
            binding.recyclerViewCategories.adapter = CategoryAdapter(categories)
        }

        homeViewModel.publications.observe(viewLifecycleOwner) { publications ->
            binding.recyclerViewPublications.adapter = PublicationAdapter(publications)
        }

        // Configurar click listener para el icono de notificaciones
        binding.notificationIcon.setOnClickListener {
            // TODO: Implementar navegación a notificaciones o mostrar panel de notificaciones
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}