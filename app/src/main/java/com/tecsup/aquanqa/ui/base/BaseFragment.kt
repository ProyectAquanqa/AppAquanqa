package com.tecsup.aquanqa.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Fragment base que establece configuraciones comunes para todos los fragments de la aplicación.
 * Proporciona manejo automático del ViewBinding y métodos de utilidad comunes.
 * 
 * @param VB El tipo de ViewBinding que usa el fragment
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    /**
     * Metodo abstracto que debe ser implementado por los fragments hijos
     * para inflar su layout específico
     */
    protected abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /**
     * Metodo opcional para configurar la UI después de que se cree la vista
     */
    protected open fun setupUI() {}

    /**
     * Metodo opcional para configurar observadores de datos
     */
    protected open fun setupObservers() {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = getViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Establecer el fondo blanco para todos los fragments
        view.setBackgroundColor(requireContext().getColor(android.R.color.white))
        
        // Llamar a los métodos de configuración
        setupUI()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Metodo de utilidad para mostrar mensajes Toast
     */
    protected fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), message, duration).show()
    }

    /**
     * Metodo de utilidad para mostrar los mensajes de error diferentes que se encuentren
     */
    protected fun showError(error: String) {
        if (error.isNotEmpty()) {
            showToast(error, Toast.LENGTH_LONG)
        }
    }
} 