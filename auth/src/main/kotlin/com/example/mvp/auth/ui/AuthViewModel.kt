package com.example.mvp.auth.ui

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.mvp.auth.data.AuthRepository
import com.example.mvp.core.base.BaseViewModel
import com.example.mvp.core.base.Resource
import com.example.mvp.core.base.UiEffect
import com.example.mvp.core.base.UiEvent
import com.example.mvp.core.base.UiState
import com.example.mvp.data.models.User
import kotlinx.coroutines.launch
import javax.inject.Inject

class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    initialState: AuthState = AuthState()
) : BaseViewModel<AuthEvent, AuthState, AuthEffect>() {
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
    
    init {
        setState { initialState }
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true) }
                Log.d(TAG, "Checking current user")
                
                when (val result = authRepository.getCurrentUser()) {
                    is Resource.Success -> {
                        setState { copy(currentUser = result.data, isLoading = false) }
                        Log.d(TAG, "User is logged in: ${result.data.email}")
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        Log.d(TAG, "No user logged in: ${result.message}")
                    }
                    is Resource.Loading -> {
                        // This shouldn't happen as getCurrentUser doesn't return Loading
                    }
                }
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { AuthEffect.ShowError("Error checking user: ${e.message}") }
                Log.e(TAG, "Error checking current user", e)
            }
        }
    }

    private fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true) }
                Log.d(TAG, "Attempting login for user: $email")
                
                when (val result = authRepository.login(email, password)) {
                    is Resource.Success -> {
                        setState { copy(currentUser = result.data, isLoading = false) }
                        setEffect { AuthEffect.LoginSuccess }
                        Log.d(TAG, "Login successful for user: ${result.data.email}")
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        setEffect { AuthEffect.ShowError(result.message) }
                        Log.e(TAG, "Login failed: ${result.message}")
                    }
                    is Resource.Loading -> {
                        // This shouldn't happen as login doesn't return Loading
                    }
                }
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { AuthEffect.ShowError(e.message ?: "Login failed") }
                Log.e(TAG, "Login exception", e)
            }
        }
    }

    private fun signup(email: String, password: String, role: String) {
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true) }
                Log.d(TAG, "Starting signup process with role: $role")
                
                when (val result = authRepository.register(email, password, role)) {
                    is Resource.Success -> {
                        setState { copy(currentUser = result.data, isLoading = false) }
                        setEffect { AuthEffect.SignupSuccess }
                        Log.d(TAG, "Signup successful for user: $email with role: $role")
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        setEffect { AuthEffect.ShowError(result.message) }
                        Log.e(TAG, "Signup failed: ${result.message}")
                    }
                    is Resource.Loading -> {
                        // This shouldn't happen as register doesn't return Loading
                    }
                }
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { AuthEffect.ShowError(e.message ?: "Signup failed") }
                Log.e(TAG, "Signup exception", e)
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true) }
                Log.d(TAG, "Logging out user")
                
                when (val result = authRepository.logout()) {
                    is Resource.Success -> {
                        setState { copy(currentUser = null, isLoading = false) }
                        setEffect { AuthEffect.LogoutSuccess }
                        Log.d(TAG, "Logout successful")
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        setEffect { AuthEffect.ShowError(result.message) }
                        Log.e(TAG, "Logout failed: ${result.message}")
                    }
                    is Resource.Loading -> {
                        // This shouldn't happen as logout doesn't return Loading
                    }
                }
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { AuthEffect.ShowError(e.message ?: "Logout failed") }
                Log.e(TAG, "Logout exception", e)
            }
        }
    }
    
    override fun createInitialState(): AuthState = AuthState()
    
    override fun handleEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.Login -> login(event.email, event.password)
            is AuthEvent.Signup -> signup(event.email, event.password, event.role)
            is AuthEvent.Logout -> logout()
        }
    }
}

sealed class AuthEvent : UiEvent {
    data class Login(val email: String, val password: String) : AuthEvent()
    data class Signup(val email: String, val password: String, val role: String) : AuthEvent()
    object Logout : AuthEvent()
}

data class AuthState(
    val currentUser: User? = null,
    val isLoading: Boolean = false
) : UiState

sealed class AuthEffect : UiEffect {
    object LoginSuccess : AuthEffect()
    object SignupSuccess : AuthEffect()
    object LogoutSuccess : AuthEffect()
    data class ShowError(val message: String) : AuthEffect()
}