package com.example.mvp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvp.data.models.User
import com.example.mvp.data.services.AuthService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing app-level state including authentication,
 * app initialization, and navigation control.
 */
class MainViewModel : ViewModel() {

    private val authService = AuthService()

    // App UI state
    sealed class AppState {
        object Loading : AppState()
        object Unauthenticated : AppState()
        data class Authenticated(val user: User) : AppState()
        data class Error(val message: String) : AppState()
    }
    
    // Map auth service state to app state
    val appState: StateFlow<AppState> = authService.authState
        .map { authState ->
            when (authState) {
                is AuthService.AuthState.Initializing -> AppState.Loading
                is AuthService.AuthState.Authenticated -> {
                    val user = authService.currentUser.value
                    if (user != null) {
                        AppState.Authenticated(user)
                    } else {
                        // This shouldn't happen normally, but handling it anyway
                        AppState.Error("User authentication state mismatch")
                    }
                }
                is AuthService.AuthState.Unauthenticated -> AppState.Unauthenticated
                is AuthService.AuthState.Error -> AppState.Error(authState.message)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AppState.Loading
        )

    // Current authenticated user
    val currentUser: StateFlow<User?> = authService.currentUser
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )
    
    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Result<User> {
        return when (val result = authService.login(email, password)) {
            is AuthService.AuthResult.Success -> Result.success(result.user)
            is AuthService.AuthResult.Error -> Result.failure(result.throwable)
        }
    }

    /**
     * Register a new user
     */
    suspend fun register(
        email: String,
        password: String,
        username: String,
        isGeneralUser: Boolean
    ): Result<User> {
        return when (val result = authService.registerUser(email, password, username, isGeneralUser)) {
            is AuthService.AuthResult.Success -> Result.success(result.user)
            is AuthService.AuthResult.Error -> Result.failure(result.throwable)
        }
    }

    /**
     * Reset password for an email
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return authService.resetPassword(email)
    }

    /**
     * Logout the current user
     */
    fun logout() {
        viewModelScope.launch {
            authService.logout()
        }
    }

    /**
     * Check if the current user is a farmer
     */
    fun isFarmer(): Boolean {
        return currentUser.value?.isFarmer() ?: false
    }

    /**
     * Check if the user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return authService.isAuthenticated()
    }
}