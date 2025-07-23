package com.tecsup.aquanqa.data

import com.tecsup.aquanqa.data.model.LoggedInUser
import com.tecsup.aquanqa.data.model.LoginRequest
import com.tecsup.aquanqa.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
class LoginRepository(
    private val dataSource: LoginDataSource,
    private val userPreferences: UserPreferences
) {
    var user: LoggedInUser? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    init {
        user = null
    }

    suspend fun hasAccessToken(): Flow<Boolean> {
        return userPreferences.accessToken.map { token ->
            !token.isNullOrEmpty()
        }
    }

    suspend fun logout() {
        user = null
        dataSource.logout()
    }

    suspend fun login(dni: String, password: String): Result<LoggedInUser> {
        val loginRequest = LoginRequest(username = dni, password = password)
        val result = dataSource.login(loginRequest)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }

        return result
    }

    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
    }
}