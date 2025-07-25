package com.tecsup.aquanqa.ui.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

/**
 * Fragment base que establece configuraciones comunes para todos los fragments de la aplicación.
 * Todos los fragments deberían extender esta clase en lugar de Fragment directamente.
 */
open class BaseFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Establecer el fondo blanco para todos los fragments
        view.setBackgroundColor(requireContext().getColor(android.R.color.white))
    }
} 