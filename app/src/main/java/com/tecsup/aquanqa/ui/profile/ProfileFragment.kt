package com.tecsup.aquanqa.ui.profile

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.tecsup.aquanqa.R
import com.tecsup.aquanqa.databinding.FragmentProfileBinding

/**
 * Fragment para mostrar el perfil del usuario.
 * Muestra la información del usuario y permite navegar a la pantalla de edición.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel con el Factory
        viewModel = ViewModelProvider(
            requireActivity(), 
            ProfileViewModelFactory(requireActivity().application)
        )[ProfileViewModel::class.java]
        
        // Configurar observadores
        setupObservers()
        
        // Configurar listeners
        setupListeners()
        
        // Cargar datos del perfil
        viewModel.loadUserProfile()
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar perfil al regresar al fragmento (por ejemplo, después de editar)
        viewModel.loadUserProfile()
    }
    
    private fun setupObservers() {
        // Observar cambios en el perfil del usuario
        viewModel.userProfile.observe(viewLifecycleOwner) { userProfile ->
            // Mostrar el nombre completo del usuario
            val fullName = "${userProfile.first_name} ${userProfile.last_name}"
            binding.nameTextView.text = userProfile.first_name
            
            // Mostrar el rol/grupo del usuario
            val role = userProfile.groups?.firstOrNull() ?: "Usuario"
            binding.roleTextView.text = role
            
            // Configurar las filas de detalles del perfil
            binding.fullNameValueTextView.text = fullName
            binding.dniValueTextView.text = userProfile.username
            binding.emailValueTextView.text = userProfile.email ?: getString(R.string.no_email)
            
            // Cargar foto de perfil si existe
            userProfile.foto_perfil?.let { fotoUrl ->
                val imageUrl = if (fotoUrl.startsWith("http")) fotoUrl else viewModel.getBaseUrl() + fotoUrl
                
                Glide.with(requireContext())
                    .load(imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(binding.profileImageView)
            } ?: run {
                // Si no hay foto, mostrar imagen por defecto
                Glide.with(requireContext())
                    .load(R.drawable.ic_person)
                    .apply(RequestOptions.circleCropTransform())
                    .into(binding.profileImageView)
            }
            
            // Cargar firma si existe
            userProfile.firma?.let { firmaUrl ->
                val signatureUrl = if (firmaUrl.startsWith("http")) firmaUrl else viewModel.getBaseUrl() + firmaUrl
                
                Glide.with(requireContext())
                    .load(signatureUrl)
                    .into(binding.signatureImageView)
            } ?: run {
                // Si no hay firma, la vista se queda vacía o con un placeholder
                binding.signatureImageView.setImageResource(android.R.color.transparent)
            }
        }
        
        // Observar errores
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setupListeners() {
        // Configurar botón de edición
        binding.editButton.setOnClickListener {
            // Navegar al fragmento de edición de perfil
            findNavController().navigate(R.id.action_navigation_profile_to_editProfileFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 