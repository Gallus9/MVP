package com.example.mvp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvp.data.models.Feedback
import com.example.mvp.data.models.Media
import com.example.mvp.data.models.User
import com.parse.ParseFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Error(val message: String) : ProfileState()
    data class Success(val user: User) : ProfileState()
}

sealed class FeedbackListState {
    object Idle : FeedbackListState()
    object Loading : FeedbackListState()
    data class Error(val message: String) : FeedbackListState()
    data class Success(val feedbacks: List<Feedback>) : FeedbackListState()
}

class ProfileViewModel : ViewModel() {
    
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()
    
    private val _feedbackState = MutableStateFlow<FeedbackListState>(FeedbackListState.Idle)
    val feedbackState: StateFlow<FeedbackListState> = _feedbackState.asStateFlow()
    
    // Get user profile
    fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading
                
                val user = getUserById(userId)
                if (user != null) {
                    _profileState.value = ProfileState.Success(user)
                } else {
                    _profileState.value = ProfileState.Error("User not found")
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to fetch user profile")
            }
        }
    }
    
    // Update profile image
    fun updateProfileImage(user: User, imageData: ByteArray) {
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading
                
                // Create media for profile image
                val media = Media()
                val parseFile = ParseFile("profile_${System.currentTimeMillis()}.jpg", imageData)
                parseFile.saveInBackground()
                
                media.file = parseFile
                media.owner = user
                media.mediaType = Media.TYPE_IMAGE
                media.saveInBackground()
                
                // Update user with profile image
                user.profileImage = media
                saveUser(user)
                
                _profileState.value = ProfileState.Success(user)
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to update profile image")
            }
        }
    }
    
    // Get user feedback
    fun fetchUserFeedback(user: User) {
        viewModelScope.launch {
            try {
                _feedbackState.value = FeedbackListState.Loading
                
                val feedbacks = getUserFeedbacks(user)
                _feedbackState.value = FeedbackListState.Success(feedbacks)
            } catch (e: Exception) {
                _feedbackState.value = FeedbackListState.Error(e.message ?: "Failed to fetch user feedback")
            }
        }
    }
    
    // Give feedback to a user
    fun giveFeedback(fromUser: User, toUser: User, rating: Int, comment: String?, orderId: String? = null) {
        viewModelScope.launch {
            try {
                val feedback = Feedback()
                feedback.fromUser = fromUser
                feedback.toUser = toUser
                feedback.rating = rating
                feedback.comment = comment
                
                if (orderId != null) {
                    val order = getOrderById(orderId)
                    feedback.order = order
                }
                
                saveFeedback(feedback)
                
                // Refresh feedbacks
                fetchUserFeedback(toUser)
            } catch (e: Exception) {
                _feedbackState.value = FeedbackListState.Error(e.message ?: "Failed to give feedback")
            }
        }
    }
    
    // Helper coroutine methods
    private suspend fun getUserById(userId: String): User? = suspendCancellableCoroutine { continuation ->
        val query = com.parse.ParseQuery.getQuery(User::class.java)
            .whereEqualTo("objectId", userId)
        
        query.getFirstInBackground { user, e ->
            if (e == null) {
                continuation.resume(user)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private suspend fun getUserFeedbacks(user: User): List<Feedback> = suspendCancellableCoroutine { continuation ->
        val query = Feedback.getQuery()
            .whereEqualTo(Feedback.KEY_TO_USER, user)
            .include(Feedback.KEY_FROM_USER)
            .include(Feedback.KEY_ORDER)
            .orderByDescending("createdAt")
        
        query.findInBackground { feedbacks, e ->
            if (e == null) {
                continuation.resume(feedbacks)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private suspend fun saveUser(user: User): Boolean = suspendCancellableCoroutine { continuation ->
        user.saveInBackground { e ->
            if (e == null) {
                continuation.resume(true)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private suspend fun saveFeedback(feedback: Feedback): Boolean = suspendCancellableCoroutine { continuation ->
        feedback.saveInBackground { e ->
            if (e == null) {
                continuation.resume(true)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private suspend fun getOrderById(orderId: String): com.example.mvp.data.models.Order? = suspendCancellableCoroutine { continuation ->
        val query = com.example.mvp.data.models.Order.getQuery()
            .whereEqualTo("objectId", orderId)
        
        query.getFirstInBackground { order, e ->
            if (e == null) {
                continuation.resume(order)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
}