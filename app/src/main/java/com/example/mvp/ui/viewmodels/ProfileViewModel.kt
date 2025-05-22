package com.example.mvp.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.mvp.core.base.BaseViewModel
import com.example.mvp.core.base.UiEffect
import com.example.mvp.core.base.UiEvent
import com.example.mvp.core.base.UiState
import com.example.mvp.data.models.Feedback
import com.example.mvp.data.models.Media
import com.example.mvp.data.models.Order
import com.example.mvp.data.models.User
import com.parse.ParseFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ProfileViewModel : BaseViewModel<ProfileEvent, ProfileState, ProfileEffect>() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }
    
    private val scope: CoroutineScope = object : CoroutineScope {
        override val coroutineContext = Dispatchers.Main + Job()
    }
    
    // Get user profile
    private fun fetchUserProfile(userId: String) {
        scope.launch {
            try {
                setState { copy(isLoading = true) }
                Log.d(TAG, "Fetching user profile for ID: $userId")
                
                val user = getUserById(userId)
                if (user != null) {
                    setState { copy(user = user, isLoading = false) }
                    Log.d(TAG, "Successfully fetched user profile: ${user.username}")
                } else {
                    setState { copy(isLoading = false) }
                    setEffect { ProfileEffect.ShowError("User not found") }
                    Log.w(TAG, "User not found for ID: $userId")
                }
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { ProfileEffect.ShowError(e.message ?: "Failed to fetch user profile") }
                Log.e(TAG, "Error fetching user profile", e)
            }
        }
    }
    
    // Update profile image
    private fun updateProfileImage(user: User, imageData: ByteArray) {
        scope.launch {
            try {
                setState { copy(isLoading = true) }
                Log.d(TAG, "Updating profile image for user: ${user.username}")
                
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
                
                setState { copy(user = user, isLoading = false) }
                setEffect { ProfileEffect.ProfileUpdated(user) }
                Log.d(TAG, "Profile image updated successfully")
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { ProfileEffect.ShowError(e.message ?: "Failed to update profile image") }
                Log.e(TAG, "Error updating profile image", e)
            }
        }
    }
    
    // Get user feedback
    private fun fetchUserFeedback(user: User) {
        scope.launch {
            try {
                setState { copy(isLoadingFeedback = true) }
                Log.d(TAG, "Fetching feedback for user: ${user.username}")
                
                val feedbacks = getUserFeedbacks(user)
                setState { copy(feedbacks = feedbacks, isLoadingFeedback = false) }
                Log.d(TAG, "Successfully fetched ${feedbacks.size} feedbacks")
            } catch (e: Exception) {
                setState { copy(isLoadingFeedback = false) }
                setEffect { ProfileEffect.ShowError(e.message ?: "Failed to fetch user feedback") }
                Log.e(TAG, "Error fetching user feedback", e)
            }
        }
    }
    
    // Give feedback to a user
    private fun giveFeedback(fromUser: User, toUser: User, rating: Int, comment: String?, orderId: String? = null) {
        scope.launch {
            try {
                setState { copy(isLoadingFeedback = true) }
                Log.d(TAG, "Giving feedback to user: ${toUser.username}")
                
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
                
                // Get updated feedbacks
                val updatedFeedbacks = getUserFeedbacks(toUser)
                setState { 
                    copy(
                        feedbacks = updatedFeedbacks,
                        isLoadingFeedback = false
                    )
                }
                setEffect { ProfileEffect.FeedbackGiven(feedback) }
                Log.d(TAG, "Feedback given successfully")
            } catch (e: Exception) {
                setState { copy(isLoadingFeedback = false) }
                setEffect { ProfileEffect.ShowError(e.message ?: "Failed to give feedback") }
                Log.e(TAG, "Error giving feedback", e)
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
    
    private suspend fun getOrderById(orderId: String): Order? = suspendCancellableCoroutine { continuation ->
        val query = Order.getQuery()
            .whereEqualTo("objectId", orderId)
        
        query.getFirstInBackground { order, e ->
            if (e == null) {
                continuation.resume(order)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
    
    override fun createInitialState(): ProfileState = ProfileState()
    
    override fun handleEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.FetchUserProfile -> fetchUserProfile(event.userId)
            is ProfileEvent.UpdateProfileImage -> updateProfileImage(event.user, event.imageData)
            is ProfileEvent.FetchUserFeedback -> fetchUserFeedback(event.user)
            is ProfileEvent.GiveFeedback -> giveFeedback(
                event.fromUser,
                event.toUser,
                event.rating,
                event.comment,
                event.orderId
            )
        }
    }
}

sealed class ProfileEvent : UiEvent {
    data class FetchUserProfile(val userId: String) : ProfileEvent()
    data class UpdateProfileImage(val user: User, val imageData: ByteArray) : ProfileEvent()
    data class FetchUserFeedback(val user: User) : ProfileEvent()
    data class GiveFeedback(
        val fromUser: User,
        val toUser: User,
        val rating: Int,
        val comment: String?,
        val orderId: String? = null
    ) : ProfileEvent()
}

data class ProfileState(
    val user: User? = null,
    val feedbacks: List<Feedback> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingFeedback: Boolean = false
) : UiState

sealed class ProfileEffect : UiEffect {
    data class ShowError(val message: String) : ProfileEffect()
    data class ProfileUpdated(val user: User) : ProfileEffect()
    data class FeedbackGiven(val feedback: Feedback) : ProfileEffect()
}