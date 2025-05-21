package com.example.mvp.data.repositories

import android.util.Log
import com.example.mvp.data.models.Comment
import com.example.mvp.data.models.Media
import com.example.mvp.data.models.Post
import com.example.mvp.data.models.User
import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import com.parse.SaveCallback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CommunityRepository @Inject constructor(private val mediaRepository: MediaRepository) {
    
    companion object {
        private const val TAG = "CommunityRepository"
        private const val PAGE_SIZE = 10
    }
    
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()
    
    /**
     * Get community feed posts (newest first)
     */
    suspend fun getPosts(page: Int = 0): Result<List<Post>> {
        return try {
            val query = Post.getQuery()
                .include(Post.KEY_AUTHOR)
                .orderByDescending("createdAt")
                .setLimit(PAGE_SIZE)
                .setSkip(page * PAGE_SIZE)
                
            val results = suspendCancellableCoroutine<List<Post>> { continuation ->
                query.findInBackground { objects, e ->
                    if (e == null) {
                        continuation.resume(objects)
                    } else {
                        continuation.resumeWithException(e)
                    }
                }
            }
            
            if (page == 0) {
                _posts.value = results
            } else {
                _posts.value = _posts.value + results
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching posts", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get featured posts
     */
    suspend fun getFeaturedPosts(): Result<List<Post>> {
        return try {
            val query = Post.getQuery()
                .include(Post.KEY_AUTHOR)
                .whereEqualTo(Post.KEY_IS_FEATURED, true)
                .orderByDescending("createdAt")
                .setLimit(5)
                
            val results = suspendCancellableCoroutine<List<Post>> { continuation ->
                query.findInBackground { objects, e ->
                    if (e == null) {
                        continuation.resume(objects)
                    } else {
                        continuation.resumeWithException(e)
                    }
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching featured posts", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create a new community post
     */
    suspend fun createPost(
        title: String?,
        content: String,
        mediaFiles: List<File> = emptyList(),
        tags: List<String> = emptyList()
    ): Result<Post> {
        return try {
            val currentUser = ParseUser.getCurrentUser() as? User 
                ?: return Result.failure(Exception("User not logged in"))
                
            val post = Post()
            post.author = currentUser
            post.content = content
            post.title = title
            post.likesCount = 0
            post.commentsCount = 0
            post.tags = tags
            post.isFeatured = false
            
            // Save post first
            post.save()
            
            // Upload and associate media if any
            if (mediaFiles.isNotEmpty()) {
                mediaFiles.forEach { file ->
                    val mediaResult = mediaRepository.uploadMedia(
                        file = file,
                        caption = title ?: "",
                        mediaType = Media.TYPE_IMAGE
                    )
                    
                    mediaResult.getOrNull()?.let { media ->
                        post.addMedia(media)
                    }
                }
            }
            
            Result.success(post)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating post", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get comments for a post
     */
    suspend fun getComments(post: Post, page: Int = 0): Result<List<Comment>> {
        return try {
            val query = Comment.getQuery()
                .include(Comment.KEY_AUTHOR)
                .whereEqualTo(Comment.KEY_POST, post)
                .orderByDescending("createdAt")
                .setLimit(PAGE_SIZE)
                .setSkip(page * PAGE_SIZE)
                
            val results = suspendCancellableCoroutine<List<Comment>> { continuation ->
                query.findInBackground { objects, e ->
                    if (e == null) {
                        continuation.resume(objects)
                    } else {
                        continuation.resumeWithException(e)
                    }
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching comments", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add a comment to a post
     */
    suspend fun addComment(post: Post, content: String): Result<Comment> {
        return try {
            val currentUser = ParseUser.getCurrentUser() as? User 
                ?: return Result.failure(Exception("User not logged in"))
                
            val comment = Comment()
            comment.author = currentUser
            comment.post = post
            comment.content = content
            comment.likesCount = 0
            
            // Save comment
            comment.save()
            
            // Update post's comment count
            post.commentsCount = post.commentsCount + 1
            post.save()
            
            Result.success(comment)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment", e)
            Result.failure(e)
        }
    }
    
    /**
     * Like or unlike a post
     */
    suspend fun togglePostLike(post: Post): Result<Post> {
        return try {
            val currentUser = ParseUser.getCurrentUser() as? User 
                ?: return Result.failure(Exception("User not logged in"))
            
            // For simplicity, we're just incrementing/decrementing the like count
            // In a real app, you'd track which users liked which posts in a separate table
            
            // For demo purposes, let's toggle: if divisible by 2, it means user already liked it
            if (post.likesCount % 2 == 0) {
                post.likesCount += 1 // Like
            } else {
                post.likesCount -= 1 // Unlike
            }
            
            post.save()
            
            // Update the post in our local state flow
            val updatedPosts = _posts.value.map { 
                if (it.objectId == post.objectId) post else it 
            }
            _posts.value = updatedPosts
            
            Result.success(post)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling post like", e)
            Result.failure(e)
        }
    }
}