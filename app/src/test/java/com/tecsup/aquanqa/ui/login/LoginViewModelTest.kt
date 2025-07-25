package com.tecsup.aquanqa.ui.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.tecsup.aquanqa.data.LoginRepository
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.network.InvalidPasswordException
import com.tecsup.aquanqa.data.network.UserNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class LoginViewModelTest {

    // Ejecuta tareas de arquitectura inmediatamente
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = TestCoroutineDispatcher()
    
    @Mock
    private lateinit var loginRepository: LoginRepository
    
    @Mock
    private lateinit var loginResultObserver: Observer<LoginResult>
    
    @Captor
    private lateinit var loginResultCaptor: ArgumentCaptor<LoginResult>
    
    private lateinit var loginViewModel: LoginViewModel

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        Dispatchers.setMain(testDispatcher)
        loginViewModel = LoginViewModel(loginRepository)
        loginViewModel.loginResult.observeForever(loginResultObserver)
    }
    
    @After
    fun tearDown() {
        loginViewModel.loginResult.removeObserver(loginResultObserver)
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `when login with non-registered DNI, then show user not registered error`() = testDispatcher.runBlockingTest {
        // Given
        val dni = "12345678"
        val password = "password123"
        Mockito.`when`(loginRepository.login(dni, password))
            .thenReturn(Result.Error(UserNotFoundException()))
        
        // When
        loginViewModel.login(dni, password)
        
        // Then
        Mockito.verify(loginResultObserver).onChanged(loginResultCaptor.capture())
        val loginResult = loginResultCaptor.value
        assert(loginResult.error == LoginViewModel.ERROR_USER_NOT_FOUND)
    }
    
    @Test
    fun `when login with registered DNI but wrong password, then show invalid password error`() = testDispatcher.runBlockingTest {
        // Given
        val dni = "12345678"
        val password = "wrongpassword"
        Mockito.`when`(loginRepository.login(dni, password))
            .thenReturn(Result.Error(InvalidPasswordException()))
        
        // When
        loginViewModel.login(dni, password)
        
        // Then
        Mockito.verify(loginResultObserver).onChanged(loginResultCaptor.capture())
        val loginResult = loginResultCaptor.value
        assert(loginResult.error == LoginViewModel.ERROR_INVALID_PASSWORD)
    }
    
    @Test
    fun `when login with both incorrect, then user not registered error has priority`() = testDispatcher.runBlockingTest {
        // Given
        val dni = "11111111"
        val password = "wrongpassword"
        // Simulating that server prioritizes checking for user first
        Mockito.`when`(loginRepository.login(dni, password))
            .thenReturn(Result.Error(UserNotFoundException()))
        
        // When
        loginViewModel.login(dni, password)
        
        // Then
        Mockito.verify(loginResultObserver).onChanged(loginResultCaptor.capture())
        val loginResult = loginResultCaptor.value
        assert(loginResult.error == LoginViewModel.ERROR_USER_NOT_FOUND)
        // We should NOT see the password error since the DNI check happens first
    }
} 