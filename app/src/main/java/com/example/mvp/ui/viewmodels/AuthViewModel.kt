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
        val parseUser = ParseUser.getCurrentUser()
        if (parseUser != null && parseUser is User) {
            _currentUser.value = parseUser
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
                    val parseUser = ParseUser.logIn(email, password)
                    
                    if (parseUser != null) {
                        _currentUser.value = parseUser as User
                        _authState.value = AuthState.Success
                    } else {
                        _authState.value = AuthState.Error("Parse login failed")
                    }
                } else {
                    _authState.value = AuthState.Error("Firebase login failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signup(email: String, password: String, role: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                
                // Create Firebase User
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUid = authResult.user?.uid
                
                if (firebaseUid != null) {
                    // Create Parse User
                    val user = User()
                    user.username = email
                    user.setPassword(password)
                    user.email = email
                    user.firebaseUid = firebaseUid
                    user.roleAsString = role
                    
                    user.signUp()
                    
                    _currentUser.value = user
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Firebase signup failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Signup failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                
                // Logout from Parse
                ParseUser.logOut()
                
                // Logout from Firebase
                firebaseAuth.signOut()
                
                _currentUser.value = null
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Logout failed")
            }
        }
    }
}