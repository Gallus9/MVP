```
```
package com.example.mvp.ui.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.mvp.core.base.BaseViewModel
import com.example.mvp.core.base.Resource
import com.example.mvp.data.models.Comment
import com.example.mvp.data.models.Post
import com.example.mvp.data.repositories.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val communityRepository: CommunityRepository
) : BaseViewModel<CommunityEvent, CommunityState, CommunityEffect>() {

    private val TAG = "CommunityViewModel"

    private var currentPage = 0
    
    init {
        loadFeed()
        loadFeaturedPosts()
    }
    
    /**
     * Load feed posts
     */
    private fun loadFeed() {
        viewModelScope.launch {
            setState { copy(feedState = Resource.Loading) }
            try {
                val result = communityRepository.getPosts(0)
                result.fold(
                    onSuccess = { posts ->
                        currentPage = 0
                        setState { copy(
                            feedState = if (posts.isEmpty()) Resource.Success(emptyList()) else Resource.Success(posts),
                            isEmptyFeed = posts.isEmpty()
                        ) }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error loading feed: ${error.message}", error)
                        setState { copy(feedState = Resource.Error(error.message ?: "Failed to load posts")) }
                        setEffect { CommunityEffect.ShowError(error.message ?: "Failed to load posts") }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading feed: ${e.message}", e)
                setState { copy(feedState = Resource.Error(e.message ?: "Unknown error")) }
                setEffect { CommunityEffect.ShowError(e.message ?: "Unknown error") }
            }
        }
    }
    
    /**
     * Load more posts (pagination)
     */
    private fun loadMorePosts() {
        val currentFeedState = uiState.value.feedState
        if (currentFeedState is Resource.Loading) return
        
        viewModelScope.launch {
            if (currentFeedState is Resource.Success) {
                setState { copy(feedState = Resource.Loading) }
                currentPage++
                
                try {
                    val result = communityRepository.getPosts(currentPage)
                    result.fold(
                        onSuccess = { newPosts ->
                            val allPosts = currentFeedState.data + newPosts
                            setState { copy(feedState = Resource.Success(allPosts)) }
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Error loading more posts: ${error.message}", error)
                            setState { copy(feedState = currentFeedState) } // Revert to previous state
                            setEffect { CommunityEffect.ShowError(error.message ?: "Failed to load more posts") }
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception loading more posts: ${e.message}", e)
                    setState { copy(feedState = currentFeedState) } // Revert to previous state
                    setEffect { CommunityEffect.ShowError(e.message ?: "Failed to load more posts") }
                }
            }
        }
    }
    
    /**
     * Load featured posts
     */
    private fun loadFeaturedPosts() {
        viewModelScope.launch {
            try {
                val result = communityRepository.getFeaturedPosts()
                result.fold(
                    onSuccess = { posts ->
                        setState { copy(featuredPosts = posts) }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error loading featured posts: ${error.message}", error)
                        setEffect { CommunityEffect.ShowError(error.message ?: "Failed to load featured posts") }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading featured posts: ${e.message}", e)
                setEffect { CommunityEffect.ShowError(e.message ?: "Failed to load featured posts") }
            }
        }
    }
    
    /**
     * Create a new post
     */
    private fun createPost(title: String?, content: String, mediaFiles: List<File> = emptyList(), tags: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                val result = communityRepository.createPost(title, content, mediaFiles, tags)
                result.fold(
                    onSuccess = { post ->
                        setEffect { CommunityEffect.PostCreated }
                        // Refresh feed to show new post
                        loadFeed()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error creating post: ${error.message}", error)
                        setEffect { CommunityEffect.ShowError(error.message ?: "Failed to create post") }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating post: ${e.message}", e)
                setEffect { CommunityEffect.ShowError(e.message ?: "Failed to create post") }
            }
        }
    }
    
    /**
     * Select a post to view its details
     */
    private fun selectPost(post: Post) {
        setState { copy(selectedPost = post) }
        loadComments(post)
    }
    
    /**
     * Clear selected post
     */
    private fun clearSelectedPost() {
        setState { copy(selectedPost = null, comments = emptyList()) }
    }
    
    /**
     * Load comments for a post
     */
    private fun loadComments(post: Post) {
        viewModelScope.launch {
            try {
                val result = communityRepository.getComments(post, 0)
                result.fold(
                    onSuccess = { newComments ->
                        setState { copy(comments = newComments) }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error loading comments: ${error.message}", error)
                        setEffect { CommunityEffect.ShowError(error.message ?: "Failed to load comments") }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading comments: ${e.message}", e)
                setEffect { CommunityEffect.ShowError(e.message ?: "Failed to load comments") }
            }
        }
    }
    
    /**
     * Add a comment to the current selected post
     */
    private fun addComment(content: String) {
        val post = uiState.value.selectedPost ?: return
        
        viewModelScope.launch {
            try {
                val result = communityRepository.addComment(post, content)
                result.fold(
                    onSuccess = { comment ->
                        // Refresh comments
                        loadComments(post)
                        // Update selected post with new comment count without direct mutation
                        val updatedPost = post.copy() // Assuming Post is a data class, create a copy
                        setState { copy(selectedPost = updatedPost) }
                        setEffect { CommunityEffect.CommentAdded }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error adding comment: ${error.message}", error)
                        setEffect { CommunityEffect.ShowError(error.message ?: "Failed to add comment") }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception adding comment: ${e.message}", e)
                setEffect { CommunityEffect.ShowError(e.message ?: "Failed to add comment") }
            }
        }
    }
    
    /**
     * Like or unlike a post
     */
    private fun togglePostLike(post: Post) {
        viewModelScope.launch {
            try {
                val result = communityRepository.togglePostLike(post)
                result.fold(
                    onSuccess = { updatedPost ->
                        // If this is the selected post, update it
                        if (uiState.value.selectedPost?.objectId == updatedPost.objectId) {
                            setState { copy(selectedPost = updatedPost) }
                        }
                        // Update feed if the post is in the list
                        val currentFeed = uiState.value.feedState
                        if (currentFeed is Resource.Success) {
                            val updatedFeed = currentFeed.data.map {
                                if (it.objectId == updatedPost.objectId) updatedPost else it
                            }
                            setState { copy(feedState = Resource.Success(updatedFeed)) }
                        }
                        // Update featured posts if the post is there
                        val currentFeatured = uiState.value.featuredPosts
                        val updatedFeatured = currentFeatured.map {
                            if (it.objectId == updatedPost.objectId) updatedPost else it
                        }
                        if (currentFeatured != updatedFeatured) {
                            setState { copy(featuredPosts = updatedFeatured) }
                        }
                        setEffect { CommunityEffect.LikeToggled }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error toggling like: ${error.message}", error)
                        setEffect { CommunityEffect.ShowError(error.message ?: "Failed to toggle like") }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception toggling like: ${e.message}", e)
                setEffect { CommunityEffect.ShowError(e.message ?: "Failed to toggle like") }
            }
        }
    }
    
    override fun createInitialState(): CommunityState = CommunityState()
    
    override fun handleEvent(event: CommunityEvent) {
        when (event) {
            is CommunityEvent.LoadFeed -> loadFeed()
            is CommunityEvent.LoadMorePosts -> loadMorePosts()
            is CommunityEvent.LoadFeaturedPosts -> loadFeaturedPosts()
            is CommunityEvent.CreatePost -> createPost(event.title, event.content, event.mediaFiles, event.tags)
            is CommunityEvent.SelectPost -> selectPost(event.post)
            is CommunityEvent.ClearSelectedPost -> clearSelectedPost()
            is CommunityEvent.LoadComments -> event.post?.let { loadComments(it) }
            is CommunityEvent.AddComment -> addComment(event.content)
            is CommunityEvent.TogglePostLike -> togglePostLike(event.post)
        }
    }
}

sealed class CommunityEvent : com.example.mvp.core.base.UiEvent {
    object LoadFeed : CommunityEvent()
    object LoadMorePosts : CommunityEvent()
    object LoadFeaturedPosts : CommunityEvent()
    data class CreatePost(val title: String?, val content: String, val mediaFiles: List<File> = emptyList(), val tags: List<String> = emptyList()) : CommunityEvent()
    data class SelectPost(val post: Post) : CommunityEvent()
    object ClearSelectedPost : CommunityEvent()
    data class LoadComments(val post: Post) : CommunityEvent()
    data class AddComment(val content: String) : CommunityEvent()
    data class TogglePostLike(val post: Post) : CommunityEvent()
}

data class CommunityState(
    val feedState: Resource<List<Post>> = Resource.Success(emptyList()),
    val isEmptyFeed: Boolean = false,
    val featuredPosts: List<Post> = emptyList(),
    val selectedPost: Post? = null,
    val comments: List<Comment> = emptyList()
) : com.example.mvp.core.base.UiState

sealed class CommunityEffect : com.example.mvp.core.base.UiEffect {
    object PostCreated : CommunityEffect()
    object CommentAdded : CommunityEffect()
    object LikeToggled : CommunityEffect()
    data class ShowError(val message: String) : CommunityEffect()
}
```
```