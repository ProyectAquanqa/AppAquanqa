package com.tecsup.aquanqa.ui.anuncios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.tecsup.aquanqa.databinding.FragmentAnunciosBinding

class AnunciosFragment : Fragment() {

    private var _binding: FragmentAnunciosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val anunciosViewModel =
            ViewModelProvider(this).get(AnunciosViewModel::class.java)

        _binding = FragmentAnunciosBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textAnuncios
        anunciosViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 