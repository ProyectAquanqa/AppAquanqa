package com.tecsup.aquanqa.ui.beneficios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tecsup.aquanqa.databinding.FragmentBeneficiosBinding

class BeneficiosFragment : Fragment() {

    private var _binding: FragmentBeneficiosBinding? = null
    // Esta propiedad solo es válida entre onCreateView y onDestroyView.
    private val binding get() = _binding!!

    private val beneficiosViewModel: BeneficiosViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBeneficiosBinding.inflate(inflater, container, false)

        beneficiosViewModel.text.observe(viewLifecycleOwner) { newText ->
            binding.textBeneficios.text = newText
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 