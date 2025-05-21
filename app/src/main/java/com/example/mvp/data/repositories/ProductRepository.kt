package com.example.mvp.data.repositories

import android.util.Log
import com.example.mvp.data.models.Media
import com.example.mvp.data.models.ProductListing
import com.example.mvp.data.models.User
import com.parse.ParseACL
import com.parse.ParseException
import com.parse.ParseFile
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseRole
import com.parse.ParseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ProductRepository {
    private val TAG = "ProductRepository"

    /**
     * Create a new product listing with proper ACL.
     */
    suspend fun createProductListing(
        title: String,
        description: String,
        price: Number,
        isTraceable: Boolean,
        traceId: String? = null,
        seller: User
    ): Result<ProductListing> = withContext(Dispatchers.IO) {
        try {
            val product = ProductListing()
            product.title = title
            product.description = description
            product.price = price
            product.isTraceable = isTraceable
            if (!traceId.isNullOrEmpty()) {
                product.traceId = traceId
            }
            product.seller = seller

            // Set up ACL for the listing
            // Only the seller can write, all users can read
            val acl = ParseACL()
            val currentUser = ParseUser.getCurrentUser()
            if (currentUser != null) {
                acl.setReadAccess(currentUser, true)
                acl.setWriteAccess(currentUser, true)
            } else {
                throw Exception("User not authenticated")
            }

            // Grant read access to all General users
            try {
                val generalUserRole = ParseQuery.getQuery(ParseRole::class.java)
                    .whereEqualTo("name", User.ROLE_GENERAL_USER)
                    .first
                acl.setRoleReadAccess(generalUserRole.name, true)
            } catch (e: ParseException) {
                Log.e(TAG, "Error finding general user role: ${e.message}")
            }

            // Grant read access to all Farmer users
            try {
                val farmerRole = ParseQuery.getQuery(ParseRole::class.java)
                    .whereEqualTo("name", User.ROLE_FARMER)
                    .first
                acl.setRoleReadAccess(farmerRole.name, true)
            } catch (e: ParseException) {
                Log.e(TAG, "Error finding farmer role: ${e.message}")
            }

            // Set the ACL on the product
            product.acl = acl

            product.save()

            return@withContext Result.success(product)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating product listing: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Add image to a product listing.
     * This creates a Media object and links it to the product.
     */
    suspend fun addImageToProduct(
        product: ProductListing,
        imageFile: File,
        caption: String? = null
    ): Result<Media> = withContext(Dispatchers.IO) {
        try {
            val currentUser = ParseUser.getCurrentUser() as? User
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            // Verify current user is the seller
            if (product.seller?.objectId != currentUser.objectId) {
                return@withContext Result.failure(Exception("Only the seller can add images"))
            }

            val media = Media()
            media.file = ParseFile(imageFile)
            media.owner = currentUser
            media.listing = product
            media.caption = caption
            media.mediaType = Media.TYPE_IMAGE

            // Set the ACL - should match product listing ACL
            val acl = ParseACL()
            acl.setReadAccess(ParseUser.getCurrentUser(), true)
            acl.setWriteAccess(ParseUser.getCurrentUser(), true)

            // Grant read access to all users
            try {
                val generalUserRole = ParseQuery.getQuery(ParseRole::class.java)
                    .whereEqualTo("name", User.ROLE_GENERAL_USER)
                    .first
                acl.setRoleReadAccess(generalUserRole.name, true)
            } catch (e: ParseException) {
                Log.e(TAG, "Error finding general user role: ${e.message}")
            }

            try {
                val farmerRole = ParseQuery.getQuery(ParseRole::class.java)
                    .whereEqualTo("name", User.ROLE_FARMER)
                    .first
                acl.setRoleReadAccess(farmerRole.name, true)
            } catch (e: ParseException) {
                Log.e(TAG, "Error finding farmer role: ${e.message}")
            }

            media.acl = acl
            media.save()

            // Add media to product's images relation
            product.addImage(media)
            product.save()

            return@withContext Result.success(media)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding image to product: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get product listings with pagination.
     * Can be filtered by seller if needed.
     */
    suspend fun getProductListings(
        limit: Int = 20,
        skip: Int = 0,
        seller: User? = null
    ): Result<List<ProductListing>> = withContext(Dispatchers.IO) {
        try {
            val query = ProductListing.getQuery()
            query.include(ProductListing.KEY_SELLER) // Include seller details
            query.orderByDescending(ParseObject.KEY_CREATED_AT) // Newest first

            if (seller != null) {
                query.whereEqualTo(ProductListing.KEY_SELLER, seller)
            }

            query.setLimit(limit)
            query.setSkip(skip)

            val products = query.find()
            return@withContext Result.success(products)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching product listings: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get a single product listing by ID.
     */
    suspend fun getProductById(productId: String): Result<ProductListing> =
        withContext(Dispatchers.IO) {
            try {
                val query = ProductListing.getQuery()
                query.include(ProductListing.KEY_SELLER) // Include seller details
                // Use direct get method since we're already in a background thread with withContext
                val product = query.get(productId)
                return@withContext Result.success(product)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching product by ID: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }

    /**
     * Delete a product listing if current user is the seller.
     */
    suspend fun deleteProduct(product: ProductListing): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = ParseUser.getCurrentUser() as? User
                    ?: return@withContext Result.failure(Exception("User not authenticated"))

                // Verify current user is the seller
                if (product.seller?.objectId != currentUser.objectId) {
                    return@withContext Result.failure(Exception("Only the seller can delete this listing"))
                }

                // Delete the product
                product.delete()

                // Optional: delete associated media objects
                val mediaQuery = Media.getQuery()
                mediaQuery.whereEqualTo(Media.KEY_LISTING, product)
                val mediaList = mediaQuery.find()

                ParseObject.deleteAll(mediaList)

                return@withContext Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting product: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }
}