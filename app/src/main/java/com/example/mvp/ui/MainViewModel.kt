package com.example.mvp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvp.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.parse.ParseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    init {
        checkCurrentUser()
    }
    
    private fun checkCurrentUser() {
        viewModelScope.launch {
            try {
                val parseUser = ParseUser.getCurrentUser()
                if (parseUser != null && parseUser is User) {
                    _currentUser.value = parseUser
                }
            } catch (e: Exception) {
                // Error checking current user
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                // Logout from Parse
                withContext(Dispatchers.IO) {
                    ParseUser.logOut()
                }
                
                // Logout from Firebase
                FirebaseAuth.getInstance().signOut()
                
                // Clear current user
                _currentUser.value = null
            } catch (e: Exception) {
                // Error during logout
            }
        }
    }
}