package com.example.mvp.data.services

import android.util.Log
import com.example.mvp.data.models.User
import com.example.mvp.data.repositories.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.parse.ParseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Service class that handles authentication operations,
 * integrating Firebase Auth with Parse User management.
 */
class AuthService {
    private val TAG = "AuthService"
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()
    
    // Current user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Authentication state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Initialize auth state
        checkCurrentAuthState()
    }

    /**
     * Authentication state enum
     */
    sealed class AuthState {
        object Initializing : AuthState()
        object Authenticated : AuthState()
        object Unauthenticated : AuthState()
        data class Error(val message: String) : AuthState()
    }

    /**
     * Result class for auth operations
     */
    sealed class AuthResult {
        data class Success(val user: User) : AuthResult()
        data class Error(val throwable: Throwable) : AuthResult()
    }

    /**
     * Check the current authentication state
     */
    private fun checkCurrentAuthState() {
        val firebaseUser = firebaseAuth.currentUser
        val parseUser = ParseUser.getCurrentUser()

        if (firebaseUser != null && parseUser != null) {
            _currentUser.value = parseUser as User
            _authState.value = AuthState.Authenticated
        } else {
            // Log out of both services to ensure sync
            logout()
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * Register a new user with email and password
     */
    suspend fun registerUser(
        email: String, 
        password: String, 
        username: String,
        isGeneralUser: Boolean = true
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val result = userRepository.registerUser(email, password, username, isGeneralUser)
            
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated
                    AuthResult.Success(user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Registration failed")
                    AuthResult.Error(exception)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user: ${e.message}", e)
            _authState.value = AuthState.Error(e.message ?: "Registration failed")
            AuthResult.Error(e)
        }
    }

    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val result = userRepository.loginUser(email, password)
            
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated
                    AuthResult.Success(user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Login failed")
                    AuthResult.Error(exception)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error logging in: ${e.message}", e)
            _authState.value = AuthState.Error(e.message ?: "Login failed")
            AuthResult.Error(e)
        }
    }

    /**
     * Logout from both Firebase and Parse
     */
    fun logout() {
        firebaseAuth.signOut()
        ParseUser.logOut()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }

    /**
     * Reset password for an email
     */
    suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting password: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get the current Firebase user
     */
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    /**
     * Get the current Parse user
     */
    fun getCurrentParseUser(): User? {
        return ParseUser.getCurrentUser() as? User
    }

    /**
     * Check if the user is authenticated in both Firebase and Parse
     */
    fun isAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null && ParseUser.getCurrentUser() != null
    }
}