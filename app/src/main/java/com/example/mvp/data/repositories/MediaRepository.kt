package com.example.mvp.data.repositories

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.mvp.data.models.Media
import com.example.mvp.data.models.ProductListing
import com.example.mvp.data.models.User
import com.parse.ParseFile
import com.parse.ParseQuery
import com.parse.ParseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class MediaRepository {
    private val TAG = "MediaRepository"

    /**
     * Upload an image from a file URI
     */
    suspend fun uploadImageFromUri(
        context: Context,
        uri: Uri,
        caption: String? = null,
        product: ProductListing? = null,
        mediaType: String = Media.TYPE_IMAGE
    ): Result<Media> = withContext(Dispatchers.IO) {
        try {
            // Get the file bytes from the URI
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileBytes = inputStream?.readBytes()
            inputStream?.close()

            if (fileBytes == null) {
                return@withContext Result.failure(IOException("Could not read file"))
            }

            // Create a filename
            val filename = "image_${System.currentTimeMillis()}.jpg"
            
            // Create ParseFile
            val parseFile = ParseFile(filename, fileBytes)
            parseFile.save()

            // Create Media object
            val media = Media()
            media.file = parseFile
            media.owner = ParseUser.getCurrentUser() as User
            media.caption = caption
            media.mediaType = mediaType
            
            if (product != null) {
                media.listing = product
            }

            // Save media object
            media.save()
            
            // If this is for a product listing, add it to the relation
            if (product != null) {
                product.addImage(media)
                product.save()
            }
            
            return@withContext Result.success(media)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Upload media from a file
     */
    suspend fun uploadMedia(
        file: File,
        caption: String? = null,
        product: ProductListing? = null,
        mediaType: String = Media.TYPE_IMAGE
    ): Result<Media> = withContext(Dispatchers.IO) {
        try {
            // Read file bytes
            val inputStream = FileInputStream(file)
            val fileBytes = inputStream.readBytes()
            inputStream.close()
            
            // Create a filename
            val filename = "media_${System.currentTimeMillis()}_${file.name}"
            
            // Create ParseFile
            val parseFile = ParseFile(filename, fileBytes)
            parseFile.save()

            // Create Media object
            val media = Media()
            media.file = parseFile
            media.owner = ParseUser.getCurrentUser() as User
            media.caption = caption
            media.mediaType = mediaType
            
            if (product != null) {
                media.listing = product
            }

            // Save media object
            media.save()
            
            // If this is for a product listing, add it to the relation
            if (product != null) {
                product.addImage(media)
                product.save()
            }
            
            return@withContext Result.success(media)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading media from file: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Upload an image from a bitmap
     */
    suspend fun uploadImageFromBitmap(
        bitmap: Bitmap,
        caption: String? = null,
        product: ProductListing? = null,
        quality: Int = 80,
        mediaType: String = Media.TYPE_IMAGE
    ): Result<Media> = withContext(Dispatchers.IO) {
        try {
            // Convert bitmap to bytes
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            val fileBytes = byteArrayOutputStream.toByteArray()

            // Create a filename
            val filename = "image_${System.currentTimeMillis()}.jpg"
            
            // Create ParseFile
            val parseFile = ParseFile(filename, fileBytes)
            parseFile.save()

            // Create Media object
            val media = Media()
            media.file = parseFile
            media.owner = ParseUser.getCurrentUser() as User
            media.caption = caption
            media.mediaType = mediaType
            
            if (product != null) {
                media.listing = product
            }

            // Save media object
            media.save()
            
            // If this is for a product listing, add it to the relation
            if (product != null) {
                product.addImage(media)
                product.save()
            }
            
            return@withContext Result.success(media)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image from bitmap: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Get media items for a product listing
     */
    suspend fun getMediaForProduct(product: ProductListing): Result<List<Media>> = withContext(Dispatchers.IO) {
        try {
            val query = Media.getQuery()
            query.whereEqualTo(Media.KEY_LISTING, product)
            query.include(Media.KEY_OWNER)
            query.orderByDescending("createdAt")
            
            val media = query.find()
            return@withContext Result.success(media)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting media for product: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Get media items for a user (profile images)
     */
    suspend fun getMediaForUser(user: User): Result<List<Media>> = withContext(Dispatchers.IO) {
        try {
            val query = Media.getQuery()
            query.whereEqualTo(Media.KEY_OWNER, user)
            query.orderByDescending("createdAt")
            
            val media = query.find()
            return@withContext Result.success(media)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting media for user: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Delete media item
     */
    suspend fun deleteMedia(media: Media): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Remove from product listing if it's associated
            val listing = media.listing
            if (listing != null) {
                listing.removeImage(media)
                listing.save()
            }
            
            // Delete the media object
            media.delete()
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting media: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Set user profile image
     */
    suspend fun setUserProfileImage(user: User, media: Media): Result<User> = withContext(Dispatchers.IO) {
        try {
            user.profileImage = media
            user.save()
            return@withContext Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting profile image: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
}