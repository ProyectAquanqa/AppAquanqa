package com.tecsup.aquanqa.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tecsup.aquanqa.data.LoginDataSource
import com.tecsup.aquanqa.data.LoginRepository
import com.tecsup.aquanqa.data.preferences.UserPreferences

/**
 * Fábrica para crear una instancia de LoginViewModel con las dependencias necesarias
 */
class LoginViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            val userPreferences = UserPreferences(context.applicationContext)
            val loginDataSource = LoginDataSource(userPreferences)
            val loginRepository = LoginRepository(
                dataSource = loginDataSource, 
                userPreferences = userPreferences,
                context = context.applicationContext
            )
            return LoginViewModel(loginRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 