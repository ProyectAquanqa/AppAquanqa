package com.tecsup.aquanqa.ui.beneficios

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tecsup.aquanqa.databinding.FragmentBeneficiosBinding
import com.tecsup.aquanqa.ui.base.BaseFragment

class BeneficiosFragment : BaseFragment<FragmentBeneficiosBinding>() {

    private val beneficiosViewModel: BeneficiosViewModel by viewModels()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBeneficiosBinding {
        return FragmentBeneficiosBinding.inflate(inflater, container, false)
    }

    override fun setupObservers() {
        super.setupObservers()
        
        beneficiosViewModel.text.observe(viewLifecycleOwner) { newText ->
            binding.textBeneficios.text = newText
        }
    }
} 