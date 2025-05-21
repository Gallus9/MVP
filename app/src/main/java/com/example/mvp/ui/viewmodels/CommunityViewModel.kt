package com.example.mvp.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvp.data.models.Comment
import com.example.mvp.data.models.Post
import com.example.mvp.data.repositories.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val TAG = "CommunityViewModel"

    private val _uiState = MutableStateFlow<CommunityUiState>(CommunityUiState.Loading)
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()
    
    private val _featuredPosts = MutableStateFlow<List<Post>>(emptyList())
    val featuredPosts: StateFlow<List<Post>> = _featuredPosts.asStateFlow()
    
    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost.asStateFlow()
    
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()
    
    private var currentPage = 0
    
    init {
        loadFeed()
        loadFeaturedPosts()
    }
    
    /**
     * Load feed posts
     */
    fun loadFeed() {
        viewModelScope.launch {
            _uiState.value = CommunityUiState.Loading
            
            try {
                val result = communityRepository.getPosts(0)
                result.fold(
                    onSuccess = { posts ->
                        currentPage = 0
                        _uiState.value = if (posts.isEmpty()) {
                            CommunityUiState.Empty
                        } else {
                            CommunityUiState.Success(posts)
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error loading feed: ${error.message}", error)
                        _uiState.value = CommunityUiState.Error(error.message ?: "Failed to load posts")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading feed: ${e.message}", e)
                _uiState.value = CommunityUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Load more posts (pagination)
     */
    fun loadMorePosts() {
        if (_uiState.value is CommunityUiState.Loading) return
        
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is CommunityUiState.Success) {
                _uiState.value = CommunityUiState.LoadingMore(currentState.posts)
                currentPage++
                
                try {
                    val result = communityRepository.getPosts(currentPage)
                    result.fold(
                        onSuccess = { newPosts ->
                            val allPosts = currentState.posts + newPosts
                            _uiState.value = CommunityUiState.Success(allPosts)
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Error loading more posts: ${error.message}", error)
                            _uiState.value = currentState // Revert to previous state
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception loading more posts: ${e.message}", e)
                    _uiState.value = currentState // Revert to previous state
                }
            }
        }
    }
    
    /**
     * Load featured posts
     */
    fun loadFeaturedPosts() {
        viewModelScope.launch {
            try {
                val result = communityRepository.getFeaturedPosts()
                result.fold(
                    onSuccess = { posts ->
                        _featuredPosts.value = posts
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error loading featured posts: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading featured posts: ${e.message}", e)
            }
        }
    }
    
    /**
     * Create a new post
     */
    fun createPost(title: String?, content: String, mediaFiles: List<File> = emptyList(), tags: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                val result = communityRepository.createPost(title, content, mediaFiles, tags)
                result.fold(
                    onSuccess = { post ->
                        // Refresh feed to show new post
                        loadFeed()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error creating post: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating post: ${e.message}", e)
            }
        }
    }
    
    /**
     * Select a post to view its details
     */
    fun selectPost(post: Post) {
        _selectedPost.value = post
        loadComments(post)
    }
    
    /**
     * Clear selected post
     */
    fun clearSelectedPost() {
        _selectedPost.value = null
        _comments.value = emptyList()
    }
    
    /**
     * Load comments for a post
     */
    fun loadComments(post: Post) {
        viewModelScope.launch {
            try {
                val result = communityRepository.getComments(post, 0)
                result.fold(
                    onSuccess = { newComments ->
                        _comments.value = newComments
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error loading comments: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading comments: ${e.message}", e)
            }
        }
    }
    
    /**
     * Add a comment to the current selected post
     */
    fun addComment(content: String) {
        val post = _selectedPost.value ?: return
        
        viewModelScope.launch {
            try {
                val result = communityRepository.addComment(post, content)
                result.fold(
                    onSuccess = { comment ->
                        // Refresh comments
                        loadComments(post)
                        
                        // Update selected post with new comment count
                        _selectedPost.value = post.apply { commentsCount++ }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error adding comment: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception adding comment: ${e.message}", e)
            }
        }
    }
    
    /**
     * Like or unlike a post
     */
    fun togglePostLike(post: Post) {
        viewModelScope.launch {
            try {
                val result = communityRepository.togglePostLike(post)
                result.fold(
                    onSuccess = { updatedPost ->
                        // If this is the selected post, update it
                        if (_selectedPost.value?.objectId == updatedPost.objectId) {
                            _selectedPost.value = updatedPost
                        }
                        
                        // The repository already updates the posts flow
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error toggling like: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception toggling like: ${e.message}", e)
            }
        }
    }
}

/**
 * UI state for the community feed
 */
sealed class CommunityUiState {
    object Loading : CommunityUiState()
    data class LoadingMore(val posts: List<Post>) : CommunityUiState()
    data class Success(val posts: List<Post>) : CommunityUiState()
    object Empty : CommunityUiState()
    data class Error(val message: String) : CommunityUiState()
}