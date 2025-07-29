package com.tecsup.aquanqa.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.api.ApiClient
import com.tecsup.aquanqa.data.model.Anuncio
import com.tecsup.aquanqa.data.model.Category
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.tecsup.aquanqa.databinding.FragmentHomeBinding
import com.tecsup.aquanqa.ui.anuncios.AnunciosAdapterWrapper
import com.tecsup.aquanqa.ui.anuncios.createAnunciosAdapter
import com.tecsup.aquanqa.ui.base.BaseFragment
import com.tecsup.aquanqa.utils.DateUtils
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var eventsAdapter: AnunciosAdapterWrapper

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initializeViewModel()
        setupRecyclerViews()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun initializeViewModel() {
        val userPreferences = UserPreferences(requireContext())
        val apiClient = ApiClient.getClient(requireContext())
        val apiService = apiClient.apiService
        val repository = HomeRepository(apiService, userPreferences)
        
        viewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(repository)
        )[HomeViewModel::class.java]
    }

    override fun setupUI() {
        super.setupUI()
        binding.tvDate.text = viewModel.currentDateSpanish
    }

    private fun setupRecyclerViews() {
        categoryAdapter = CategoryAdapter { category ->
            viewModel.onCategorySelected(category)
        }
        
        binding.rvCategories.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
        
        eventsAdapter = createAnunciosAdapter { anuncio ->
            // TODO: Handle event click
        }
        
        binding.rvPublications.apply {
            adapter = eventsAdapter.getAdapter()
            layoutManager = LinearLayoutManager(requireContext())
        }
    }



    override fun setupObservers() {
        viewModel.userFirstName.observe(viewLifecycleOwner) { firstName ->
            binding.tvUserName.text = "¡Hola, $firstName!"
            binding.tvDate.text = viewModel.currentDateSpanish
        }
        
        viewModel.categoriesState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    categoryAdapter.submitList(result.data)
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), "Error cargando categorías", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {
                    // Loading state
                }
            }
        }
        
        viewModel.eventsState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    eventsAdapter.submitList(result.data)
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), "Error cargando eventos", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {
                    // Loading state
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.selectedCategory.collect { category ->
                category?.let { 
                    categoryAdapter.setSelectedCategory(it.id)
                }
            }
        }
    }

    fun refreshData() {
        viewModel.refreshData()
    }
}

/**
 * Factory para crear instancias de HomeViewModel con dependencias manuales.
 */
class HomeViewModelFactory(
    private val repository: HomeRepository
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}