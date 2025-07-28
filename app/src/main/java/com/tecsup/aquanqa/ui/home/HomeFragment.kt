package com.tecsup.aquanqa.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import com.tecsup.aquanqa.databinding.FragmentHomeBinding
import com.tecsup.aquanqa.ui.base.BaseFragment

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }
}