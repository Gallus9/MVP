package com.example.mvp.ui

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.mvp.core.base.BaseViewModel
import com.example.mvp.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.parse.ParseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : BaseViewModel<MainEvent, MainState, MainEffect>() {
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    init {
        Log.d(TAG, "Initializing MainViewModel")
        checkCurrentUser()
    }
    
    private fun checkCurrentUser() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Checking current user")
                setState { copy(isLoading = true) }
                val parseUser = ParseUser.getCurrentUser()
                if (parseUser != null) {
                    if (parseUser is User) {
                        Log.d(TAG, "Current user found: ${parseUser.username}")
                        setState { copy(currentUser = parseUser, isLoading = false) }
                    } else {
                        Log.w(TAG, "Current user is not a User subclass")
                        setState { copy(isLoading = false) }
                    }
                } else {
                    Log.d(TAG, "No current user found")
                    setState { copy(isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking current user", e)
                setState { copy(isLoading = false) }
                setEffect { MainEffect.ShowError("Error fetching user: ${e.message}") }
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Logging out user")
                setState { copy(isLoading = true) }
                // Logout from Parse
                withContext(Dispatchers.IO) {
                    try {
                        ParseUser.logOut()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error logging out from Parse", e)
                    }
                }
                
                // Logout from Firebase
                try {
                    FirebaseAuth.getInstance().signOut()
                } catch (e: Exception) {
                    Log.e(TAG, "Error logging out from Firebase", e)
                }
                
                // Clear current user regardless of logout errors
                setState { copy(currentUser = null, isLoading = false) }
                setEffect { MainEffect.NavigateToLogin }
                Log.d(TAG, "User logged out successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout", e)
                // Still set current user to null even if errors occurred
                setState { copy(currentUser = null, isLoading = false) }
                setEffect { MainEffect.ShowError("Logout failed: ${e.message}") }
            }
        }
    }
    
    override fun createInitialState(): MainState = MainState()
    
    override fun handleEvent(event: MainEvent) {
        when (event) {
            is MainEvent.Logout -> logout()
        }
    }
}

sealed class MainEvent : com.example.mvp.core.base.UiEvent {
    object Logout : MainEvent()
}

data class MainState(
    val currentUser: User? = null,
    val isLoading: Boolean = false
) : com.example.mvp.core.base.UiState

sealed class MainEffect : com.example.mvp.core.base.UiEffect {
    object NavigateToLogin : MainEffect()
    data class ShowError(val message: String) : MainEffect()
}