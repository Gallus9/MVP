package com.example.mvp.auth.data

import com.example.mvp.core.base.Resource
import com.example.mvp.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.parse.ParseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Repository responsible for authentication operations
 * integrating Firebase Authentication and Parse
 */
class AuthRepository @Inject constructor() {

    private val firebaseAuth = getFirebaseAuth()
    
    /**
     * Gets the FirebaseAuth instance, can be overridden in tests
     */
    protected open fun getFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    /**
     * Check if user is currently logged in
     */
    suspend fun getCurrentUser(): Resource<User> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val parseUser = ParseUser.getCurrentUser()
                if (parseUser != null && parseUser is User) {
                    Resource.Success(parseUser)
                } else {
                    // Firebase user exists but not in Parse
                    firebaseAuth.signOut()
                    Resource.Error("User session inconsistent")
                }
            } else {
                Resource.Error("No user logged in")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error checking current user")
        }
    }
    
    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Resource<User> {
        return try {
            // Login with Firebase
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUid = authResult.user?.uid
                
            if (firebaseUid != null) {
                // Login with Parse using username (email) and password
                try {
                    val parseUser = withContext(Dispatchers.IO) {
                        ParseUser.logIn(email, password)
                    }
                    
                    if (parseUser != null && parseUser is User) {
                        Resource.Success(parseUser)
                    } else {
                        firebaseAuth.signOut()
                        Resource.Error("Parse login failed")
                    }
                } catch (e: Exception) {
                    // If Parse login fails, also logout from Firebase for consistency
                    firebaseAuth.signOut()
                    Resource.Error("Parse login error: ${e.message}")
                }
            } else {
                Resource.Error("Firebase login failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login failed")
        }
    }
    
    /**
     * Register a new user with email, password and role
     */
    suspend fun register(email: String, password: String, role: String): Resource<User> {
        return try {
            // Create Firebase User
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUid = authResult.user?.uid
                
            if (firebaseUid != null) {
                // Create Parse User
                try {
                    val user = withContext(Dispatchers.IO) {
                        val newUser = User()
                        newUser.username = email
                        newUser.setPassword(password)
                        newUser.email = email
                        newUser.firebaseUid = firebaseUid
                        newUser.roleAsString = role
                        
                        newUser.signUp()
                        newUser
                    }
                    
                    Resource.Success(user)
                } catch (e: Exception) {
                    // If Parse signup fails, delete the Firebase user for consistency
                    try {
                        firebaseAuth.currentUser?.delete()?.await()
                        firebaseAuth.signOut()
                    } catch (fbEx: Exception) {
                        // Log but continue
                    }
                    
                    Resource.Error("Parse signup error: ${e.message}")
                }
            } else {
                Resource.Error("Firebase signup failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Signup failed")
        }
    }
    
    /**
     * Logout user from both Firebase and Parse
     */
    suspend fun logout(): Resource<Unit> {
        return try {
            // Logout from Parse
            withContext(Dispatchers.IO) {
                ParseUser.logOut()
            }
            
            // Logout from Firebase
            firebaseAuth.signOut()
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Logout failed")
        }
    }
}