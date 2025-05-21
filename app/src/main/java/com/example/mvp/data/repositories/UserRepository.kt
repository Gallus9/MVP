package com.example.mvp.data.repositories

import android.util.Log
import com.example.mvp.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.parse.ParseException
import com.parse.ParseQuery
import com.parse.ParseRole
import com.parse.ParseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository {
    private val TAG = "UserRepository"
    private val firebaseAuth = FirebaseAuth.getInstance()

    /**
     * Register a new user with both Firebase Auth and Parse.
     * This creates a synchronized identity across both platforms.
     */
    suspend fun registerUser(
        email: String,
        password: String,
        username: String,
        isGeneralUser: Boolean = true
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Create Firebase Auth account
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Step 2: Create Parse User
                val parseUser = User()
                parseUser.username = username
                parseUser.email = email
                parseUser.firebaseUid = firebaseUser.uid

                // Set role based on user type
                val roleName = if (isGeneralUser) User.ROLE_GENERAL_USER else User.ROLE_FARMER
                parseUser.roleAsString = roleName

                // Save user to Parse
                parseUser.signUp()

                // Set up ACL for the user
                setupUserAcl(parseUser, roleName)

                return@withContext Result.success(parseUser)
            } else {
                return@withContext Result.failure(Exception("Firebase user creation failed"))
            }
        } catch (e: Exception) {
            // If Parse user creation fails, delete the Firebase user to avoid orphaned accounts
            firebaseAuth.currentUser?.delete()
            Log.e(TAG, "Error registering user: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Login with Firebase and fetch the corresponding Parse user.
     */
    suspend fun loginUser(email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                // Step 1: Authenticate with Firebase
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    // Step 2: Find corresponding Parse user by Firebase UID
                    val parseUser = getUserByFirebaseUid(firebaseUser.uid)

                    if (parseUser != null) {
                        // Login to Parse with session token from the user object
                        ParseUser.become(parseUser.sessionToken)
                        return@withContext Result.success(parseUser as User)
                    } else {
                        return@withContext Result.failure(Exception("Parse user not found for Firebase UID"))
                    }
                } else {
                    return@withContext Result.failure(Exception("Firebase authentication failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error logging in: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }

    /**
     * Find Parse user by Firebase UID.
     */
    private suspend fun getUserByFirebaseUid(firebaseUid: String): User? =
        withContext(Dispatchers.IO) {
            try {
                val query = ParseQuery.getQuery(User::class.java)
                query.whereEqualTo(User.KEY_FIREBASE_UID, firebaseUid)
                return@withContext query.first as? User
            } catch (e: ParseException) {
                Log.e(TAG, "Error getting user by Firebase UID: ${e.message}", e)
                return@withContext null
            }
        }

    /**
     * Get the current authenticated user from Parse.
     */
    fun getCurrentUser(): User? {
        return ParseUser.getCurrentUser() as? User
    }

    /**
     * Get the current authenticated Firebase user.
     */
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    /**
     * Log out from both Firebase and Parse.
     */
    fun logout() {
        firebaseAuth.signOut()
        ParseUser.logOut()
    }

    /**
     * Set up ACL for a new user.
     */
    private suspend fun setupUserAcl(user: User, roleName: String) = withContext(Dispatchers.IO) {
        try {
            // Ensure role exists
            val role = getOrCreateRole(roleName)

            // Add user to the role
            role.users.add(user)
            role.saveInBackground()

            // Update user's role pointer if needed
            if (user.roleAsPointer == null) {
                user.roleAsPointer = role
                user.saveInBackground()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up user ACL: ${e.message}", e)
        }
    }

    /**
     * Get or create a Parse Role.
     */
    private suspend fun getOrCreateRole(roleName: String): ParseRole = withContext(Dispatchers.IO) {
        val query = ParseQuery.getQuery(ParseRole::class.java)
        query.whereEqualTo("name", roleName)

        try {
            return@withContext query.first
        } catch (e: ParseException) {
            // Create role if it doesn't exist
            val role = ParseRole(roleName)
            role.save()
            return@withContext role
        }
    }
}