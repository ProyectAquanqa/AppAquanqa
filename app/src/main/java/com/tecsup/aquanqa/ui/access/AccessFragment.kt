package com.tecsup.aquanqa.ui.access

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.tecsup.aquanqa.databinding.FragmentAccessBinding

class AccessFragment : Fragment() {

    private var _binding: FragmentAccessBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val accessViewModel =
            ViewModelProvider(this).get(AccessViewModel::class.java)

        _binding = FragmentAccessBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textAccess
        accessViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 