package com.example.mvp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvp.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.parse.ParseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
    object Success : AuthState()
}

class AuthViewModel : ViewModel() {
    
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            try {
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser != null) {
                    // Check if the user exists in Parse as well
                    val parseUser = ParseUser.getCurrentUser()
                    if (parseUser != null && parseUser is User) {
                        _currentUser.value = parseUser
                        Log.d(TAG, "User is logged in: ${parseUser.email}")
                    } else {
                        // Firebase user exists but not in Parse
                        firebaseAuth.signOut()
                        Log.d(TAG, "Firebase user exists but not in Parse, signing out")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking current user", e)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                
                // Login with Firebase
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUid = authResult.user?.uid
                
                if (firebaseUid != null) {
                    // Login with Parse using username (email) and password
                    withContext(Dispatchers.IO) {
                        try {
                            val parseUser = ParseUser.logIn(email, password)
                            if (parseUser != null && parseUser is User) {
                                _currentUser.value = parseUser
                                _authState.value = AuthState.Success
                                Log.d(TAG, "Login successful for user: ${parseUser.email}")
                            } else {
                                _authState.value = AuthState.Error("Parse login failed")
                                Log.e(TAG, "Parse login failed after Firebase login")
                            }
                        } catch (e: Exception) {
                            _authState.value = AuthState.Error("Parse login error: ${e.message}")
                            Log.e(TAG, "Parse login exception", e)
                            // If Parse login fails, also logout from Firebase for consistency
                            firebaseAuth.signOut()
                        }
                    }
                } else {
                    _authState.value = AuthState.Error("Firebase login failed")
                    Log.e(TAG, "Firebase login failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
                Log.e(TAG, "Login exception", e)
            }
        }
    }

    fun signup(email: String, password: String, role: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
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
                            
                            _currentUser.value = user
                            _authState.value = AuthState.Success
                            Log.d(TAG, "Signup successful for user: $email with role: $role")
                        } catch (e: Exception) {
                            // If Parse signup fails, delete the Firebase user for consistency
                            try {
                                firebaseAuth.currentUser?.delete()?.await()
                                firebaseAuth.signOut()
                            } catch (fbEx: Exception) {
                                Log.e(TAG, "Error cleaning up Firebase user after Parse signup failure", fbEx)
                            }
                            
                            _authState.value = AuthState.Error("Parse signup error: ${e.message}")
                            Log.e(TAG, "Parse signup exception", e)
                        }
                    }
                } else {
                    _authState.value = AuthState.Error("Firebase signup failed")
                    Log.e(TAG, "Firebase signup failed - no UID returned")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Signup failed")
                Log.e(TAG, "Firebase signup exception", e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                
                // Logout from Parse
                withContext(Dispatchers.IO) {
                    ParseUser.logOut()
                }
                
                // Logout from Firebase
                firebaseAuth.signOut()
                
                _currentUser.value = null
                _authState.value = AuthState.Success
                Log.d(TAG, "Logout successful")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Logout failed")
                Log.e(TAG, "Logout exception", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
}