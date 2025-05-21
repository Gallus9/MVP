package com.example.mvp.data.repositories

import android.util.Log
import com.example.mvp.data.models.Feedback
import com.example.mvp.data.models.Order
import com.example.mvp.data.models.ProductFeedback
import com.example.mvp.data.models.ProductListing
import com.example.mvp.data.models.User
import com.parse.ParseACL
import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseRole
import com.parse.ParseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeedbackRepository {
    private val TAG = "FeedbackRepository"

    /**
     * Submit user-to-user feedback after a transaction.
     * Only allows feedback between users who have completed an order together.
     */
    suspend fun submitUserFeedback(
        toUser: User,
        rating: Int,
        comment: String?,
        relatedOrder: Order?
    ): Result<Feedback> = withContext(Dispatchers.IO) {
        try {
            val currentUser = ParseUser.getCurrentUser() as? User
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            // Verify rating is in valid range
            if (rating < 1 || rating > 5) {
                return@withContext Result.failure(Exception("Rating must be between 1 and 5"))
            }

            // Check if there's a legitimate order between the two users
            if (relatedOrder == null) {
                // If no order provided, verify that there is at least one completed order
                val query = Order.getQuery()
                query.whereEqualTo(Order.KEY_BUYER, currentUser)
                query.whereEqualTo(Order.KEY_SELLER, toUser)
                query.whereEqualTo(Order.KEY_STATUS, Order.STATUS_COMPLETED)

                // Check if current user was a buyer in a completed order
                val buyerOrders = query.count()

                // Check if current user was a seller in a completed order
                val sellerQuery = Order.getQuery()
                sellerQuery.whereEqualTo(Order.KEY_SELLER, currentUser)
                sellerQuery.whereEqualTo(Order.KEY_BUYER, toUser)
                sellerQuery.whereEqualTo(Order.KEY_STATUS, Order.STATUS_COMPLETED)

                val sellerOrders = sellerQuery.count()

                if (buyerOrders == 0 && sellerOrders == 0) {
                    return@withContext Result.failure(
                        Exception("You must complete a transaction with this user before leaving feedback")
                    )
                }
            } else {
                // If order provided, verify it's completed and involves both users
                if (relatedOrder.status != Order.STATUS_COMPLETED) {
                    return@withContext Result.failure(
                        Exception("Can only leave feedback on completed orders")
                    )
                }

                val orderInvolvesUsers = (relatedOrder.buyer?.objectId == currentUser.objectId &&
                        relatedOrder.seller?.objectId == toUser.objectId) ||
                        (relatedOrder.seller?.objectId == currentUser.objectId &&
                                relatedOrder.buyer?.objectId == toUser.objectId)

                if (!orderInvolvesUsers) {
                    return@withContext Result.failure(
                        Exception("The provided order does not involve both users")
                    )
                }
            }

            // Check if feedback already exists for this order
            if (relatedOrder != null) {
                val existingQuery = Feedback.getQuery()
                existingQuery.whereEqualTo(Feedback.KEY_FROM_USER, currentUser)
                existingQuery.whereEqualTo(Feedback.KEY_TO_USER, toUser)
                existingQuery.whereEqualTo(Feedback.KEY_ORDER, relatedOrder)

                try {
                    val existing = existingQuery.first
                    return@withContext Result.failure(
                        Exception("You have already left feedback for this order")
                    )
                } catch (e: ParseException) {
                    // No feedback found, we can proceed
                }
            }

            val feedback = Feedback()
            feedback.fromUser = currentUser
            feedback.toUser = toUser
            feedback.rating = rating
            feedback.comment = comment
            feedback.order = relatedOrder

            // Set up ACL
            val acl = ParseACL()

            // The feedback submitter can read/write
            acl.setReadAccess(currentUser, true)
            acl.setWriteAccess(currentUser, true)

            // The recipient can read but not modify
            acl.setReadAccess(toUser, true)

            // Make feedback publicly readable
            try {
                val generalUserRole = ParseQuery.getQuery(ParseRole::class.java)
                    .whereEqualTo("name", User.ROLE_GENERAL_USER)
                    .first
                acl.setRoleReadAccess(generalUserRole.name, true)

                val farmerRole = ParseQuery.getQuery(ParseRole::class.java)
                    .whereEqualTo("name", User.ROLE_FARMER)
                    .first
                acl.setRoleReadAccess(farmerRole.name, true)
            } catch (e: ParseException) {
                Log.e(TAG, "Error finding roles: ${e.message}")
            }

            feedback.acl = acl
            feedback.save()

            return@withContext Result.success(feedback)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting user feedback: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Submit feedback for a product.
     * Only allows feedback for products the user has purchased.
     */
    suspend fun submitProductFeedback(
        product: ProductListing,
        rating: Int,
        comment: String?
    ): Result<ProductFeedback> = withContext(Dispatchers.IO) {
        try {
            val currentUser = ParseUser.getCurrentUser() as? User
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            // Verify rating is in valid range
            if (rating < 1 || rating > 5) {
                return@withContext Result.failure(Exception("Rating must be between 1 and 5"))
            }

            // Verify user has purchased this product
            val orderQuery = Order.getQuery()
            orderQuery.whereEqualTo(Order.KEY_BUYER, currentUser)
            orderQuery.whereEqualTo(Order.KEY_PRODUCT, product)
            orderQuery.whereEqualTo(Order.KEY_STATUS, Order.STATUS_COMPLETED)

            if (orderQuery.count() == 0) {
                return@withContext Result.failure(
                    Exception("You must purchase and complete an order for this product before leaving feedback")
                )
            }

            // Check if user already left feedback for this product
            val existingQuery = ProductFeedback.getQuery()
            existingQuery.whereEqualTo(ProductFeedback.KEY_USER, currentUser)
            existingQuery.whereEqualTo(ProductFeedback.KEY_PRODUCT, product)

            try {
                val existing = existingQuery.first
                return@withContext Result.failure(
                    Exception("You have already left feedback for this product")
                )
            } catch (e: ParseException) {
                // No feedback found, we can proceed
            }

            val feedback = ProductFeedback()
            feedback.user = currentUser
            feedback.product = product
            feedback.rating = rating
            feedback.comment = comment

            // Set up ACL
            val acl = ParseACL()

            // The feedback submitter can read/write
            acl.setReadAccess(currentUser, true)
            acl.setWriteAccess(currentUser, true)

            // The product seller can read
            product.seller?.let { seller ->
                acl.setReadAccess(seller, true)
            }

            // Make feedback publicly readable
            try {
                val generalUserRole = ParseQuery.getQuery(ParseRole::class.java)
                    .whereEqualTo("name", User.ROLE_GENERAL_USER)
                    .first
                acl.setRoleReadAccess(generalUserRole.name, true)

                val farmerRole = ParseQuery.getQuery(ParseRole::class.java)
                    .whereEqualTo("name", User.ROLE_FARMER)
                    .first
                acl.setRoleReadAccess(farmerRole.name, true)
            } catch (e: ParseException) {
                Log.e(TAG, "Error finding roles: ${e.message}")
            }

            feedback.acl = acl
            feedback.save()

            return@withContext Result.success(feedback)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting product feedback: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get feedback for a specific user.
     */
    suspend fun getUserFeedback(
        user: User,
        limit: Int = 20,
        skip: Int = 0
    ): Result<List<Feedback>> =
        withContext(Dispatchers.IO) {
            try {
                val query = Feedback.getQuery()
                query.whereEqualTo(Feedback.KEY_TO_USER, user)
                query.include(Feedback.KEY_FROM_USER)
                query.include(Feedback.KEY_ORDER)
                query.orderByDescending(ParseObject.KEY_CREATED_AT)
                query.setLimit(limit)
                query.setSkip(skip)

                val feedbacks = query.find()
                return@withContext Result.success(feedbacks)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user feedback: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }

    /**
     * Get feedback for a specific product.
     */
    suspend fun getProductFeedback(
        product: ProductListing,
        limit: Int = 20,
        skip: Int = 0
    ): Result<List<ProductFeedback>> = withContext(Dispatchers.IO) {
        try {
            val query = ProductFeedback.getQuery()
            query.whereEqualTo(ProductFeedback.KEY_PRODUCT, product)
            query.include(ProductFeedback.KEY_USER)
            query.orderByDescending(ParseObject.KEY_CREATED_AT)
            query.setLimit(limit)
            query.setSkip(skip)

            val feedbacks = query.find()
            return@withContext Result.success(feedbacks)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting product feedback: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Calculate average rating for a user.
     */
    suspend fun calculateUserAverageRating(user: User): Result<Double> =
        withContext(Dispatchers.IO) {
            try {
                val query = Feedback.getQuery()
                query.whereEqualTo(Feedback.KEY_TO_USER, user)

                var totalRating = 0
                var count = 0

                val feedbacks = query.find()
                for (feedback in feedbacks) {
                    feedback.rating?.let {
                        totalRating += it.toInt()
                        count++
                    }
                }

                val averageRating = if (count > 0) totalRating.toDouble() / count else 0.0
                return@withContext Result.success(averageRating)
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating user average rating: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }

    /**
     * Calculate average rating for a product.
     */
    suspend fun calculateProductAverageRating(product: ProductListing): Result<Double> =
        withContext(Dispatchers.IO) {
            try {
                val query = ProductFeedback.getQuery()
                query.whereEqualTo(ProductFeedback.KEY_PRODUCT, product)

                var totalRating = 0
                var count = 0

                val feedbacks = query.find()
                for (feedback in feedbacks) {
                    feedback.rating?.let {
                        totalRating += it.toInt()
                        count++
                    }
                }

                val averageRating = if (count > 0) totalRating.toDouble() / count else 0.0
                return@withContext Result.success(averageRating)
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating product average rating: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }
}