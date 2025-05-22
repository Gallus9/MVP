```
package com.example.mvp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.mvp.core.base.BaseViewModel
import com.example.mvp.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.parse.ParseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.util.Log

class AuthViewModel : BaseViewModel<AuthEvent, AuthState, AuthEffect>() {
    
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true) }
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser != null) {
                    // Check if the user exists in Parse as well
                    val parseUser = ParseUser.getCurrentUser()
                    if (parseUser != null && parseUser is User) {
                        setState { copy(currentUser = parseUser, isLoading = false) }
                        Log.d(TAG, "User is logged in: ${parseUser.email}")
                    } else {
                        // Firebase user exists but not in Parse
                        firebaseAuth.signOut()
                        setState { copy(isLoading = false) }
                        Log.d(TAG, "Firebase user exists but not in Parse, signing out")
                    }
                } else {
                    setState { copy(isLoading = false) }
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
                // Login with Firebase
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUid = authResult.user?.uid
                
                if (firebaseUid != null) {
                    // Login with Parse using username (email) and password
                    withContext(Dispatchers.IO) {
                        try {
                            val parseUser = ParseUser.logIn(email, password)
                            if (parseUser != null && parseUser is User) {
                                setState { copy(currentUser = parseUser, isLoading = false) }
                                setEffect { AuthEffect.LoginSuccess }
                                Log.d(TAG, "Login successful for user: ${parseUser.email}")
                            } else {
                                setState { copy(isLoading = false) }
                                setEffect { AuthEffect.ShowError("Parse login failed") }
                                Log.e(TAG, "Parse login failed after Firebase login")
                            }
                        } catch (e: Exception) {
                            setState { copy(isLoading = false) }
                            setEffect { AuthEffect.ShowError("Parse login error: ${e.message}") }
                            Log.e(TAG, "Parse login exception", e)
                            // If Parse login fails, also logout from Firebase for consistency
                            firebaseAuth.signOut()
                        }
                    }
                } else {
                    setState { copy(isLoading = false) }
                    setEffect { AuthEffect.ShowError("Firebase login failed") }
                    Log.e(TAG, "Firebase login failed")
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
                
                // Create Firebase User
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUid = authResult.user?.uid
                
                if (firebaseUid != null) {
                    // Create Parse User
                    withContext(Dispatchers.IO) {
                        try {
                            val user = User()
                            user.username = email
                            user.setPassword(password)
                            user.email = email
                            user.firebaseUid = firebaseUid
                            user.roleAsString = role
                            
                            user.signUp()
                            
                            setState { copy(currentUser = user, isLoading = false) }
                            setEffect { AuthEffect.SignupSuccess }
                            Log.d(TAG, "Signup successful for user: $email with role: $role")
                        } catch (e: Exception) {
                            // If Parse signup fails, delete the Firebase user for consistency
                            try {
                                firebaseAuth.currentUser?.delete()?.await()
                                firebaseAuth.signOut()
                            } catch (fbEx: Exception) {
                                Log.e(TAG, "Error cleaning up Firebase user after Parse signup failure", fbEx)
                            }
                            
                            setState { copy(isLoading = false) }
                            setEffect { AuthEffect.ShowError("Parse signup error: ${e.message}") }
                            Log.e(TAG, "Parse signup exception", e)
                        }
                    }
                } else {
                    setState { copy(isLoading = false) }
                    setEffect { AuthEffect.ShowError("Firebase signup failed") }
                    Log.e(TAG, "Firebase signup failed - no UID returned")
                }
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { AuthEffect.ShowError(e.message ?: "Signup failed") }
                Log.e(TAG, "Firebase signup exception", e)
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            try {
                setState { copy(isLoading = true) }
                // Logout from Parse
                withContext(Dispatchers.IO) {
                    ParseUser.logOut()
                }
                
                // Logout from Firebase
                firebaseAuth.signOut()
                
                setState { copy(currentUser = null, isLoading = false) }
                setEffect { AuthEffect.LogoutSuccess }
                Log.d(TAG, "Logout successful")
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
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
}

sealed class AuthEvent : com.example.mvp.core.base.UiEvent {
    data class Login(val email: String, val password: String) : AuthEvent()
    data class Signup(val email: String, val password: String, val role: String) : AuthEvent()
    object Logout : AuthEvent()
}

data class AuthState(
    val currentUser: User? = null,
    val isLoading: Boolean = false
) : com.example.mvp.core.base.UiState

sealed class AuthEffect : com.example.mvp.core.base.UiEffect {
    object LoginSuccess : AuthEffect()
    object SignupSuccess : AuthEffect()
    object LogoutSuccess : AuthEffect()
    data class ShowError(val message: String) : AuthEffect()
}
```