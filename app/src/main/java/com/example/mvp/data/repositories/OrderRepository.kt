package com.example.mvp.data.repositories

import android.util.Log
import com.example.mvp.data.models.Order
import com.example.mvp.data.models.ProductListing
import com.example.mvp.data.models.User
import com.parse.ParseACL
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrderRepository {
    private val TAG = "OrderRepository"

    /**
     * Create a new order with proper ACL.
     * Orders should be readable/writable by both buyer and seller.
     */
    suspend fun createOrder(
        product: ProductListing,
        quantity: Int,
        price: Number? = null
    ): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val currentUser = ParseUser.getCurrentUser() as? User
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val seller = product.seller
                ?: return@withContext Result.failure(Exception("Product has no seller"))

            // Check if current user is not the seller
            if (currentUser.objectId == seller.objectId) {
                return@withContext Result.failure(Exception("Sellers cannot order their own products"))
            }

            val order = Order()
            order.buyer = currentUser
            order.seller = seller
            order.product = product
            order.status = Order.STATUS_PENDING
            order.quantity = quantity

            // Use product price by default, or override if specified
            order.price = price ?: product.price
                    ?: return@withContext Result.failure(Exception("Product has no price"))

            // Set up ACL - both buyer and seller can read/write
            val acl = ParseACL()

            // Grant access to buyer
            acl.setReadAccess(currentUser, true)
            acl.setWriteAccess(currentUser, true)

            // Grant access to seller
            acl.setReadAccess(seller, true)
            acl.setWriteAccess(seller, true)

            order.acl = acl
            order.save()

            return@withContext Result.success(order)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating order: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Update order status if current user is buyer or seller.
     */
    suspend fun updateOrderStatus(
        order: Order,
        newStatus: String
    ): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val currentUser = ParseUser.getCurrentUser() as? User
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            // Check if current user is buyer or seller
            if (order.buyer?.objectId != currentUser.objectId &&
                order.seller?.objectId != currentUser.objectId
            ) {
                return@withContext Result.failure(
                    Exception("Only buyer or seller can update order status")
                )
            }

            // Validate status transition
            when (newStatus) {
                Order.STATUS_CONFIRMED -> {
                    if (order.status != Order.STATUS_PENDING || currentUser.objectId != order.seller?.objectId) {
                        return@withContext Result.failure(
                            Exception("Only seller can confirm a pending order")
                        )
                    }
                }

                Order.STATUS_SHIPPED -> {
                    if (order.status != Order.STATUS_CONFIRMED || currentUser.objectId != order.seller?.objectId) {
                        return@withContext Result.failure(
                            Exception("Only seller can mark a confirmed order as shipped")
                        )
                    }
                }

                Order.STATUS_DELIVERED -> {
                    if (order.status != Order.STATUS_SHIPPED || currentUser.objectId != order.buyer?.objectId) {
                        return@withContext Result.failure(
                            Exception("Only buyer can mark a shipped order as delivered")
                        )
                    }
                }

                Order.STATUS_COMPLETED -> {
                    if (order.status != Order.STATUS_DELIVERED || currentUser.objectId != order.buyer?.objectId) {
                        return@withContext Result.failure(
                            Exception("Only buyer can mark a delivered order as completed")
                        )
                    }
                }

                Order.STATUS_CANCELLED -> {
                    // Both buyer and seller can cancel an order, but only in certain states
                    if (order.status == Order.STATUS_COMPLETED) {
                        return@withContext Result.failure(
                            Exception("Cannot cancel a completed order")
                        )
                    }
                }

                else -> {
                    return@withContext Result.failure(Exception("Invalid order status: $newStatus"))
                }
            }

            // Update the status
            order.status = newStatus
            order.save()

            return@withContext Result.success(order)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order status: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get orders for current user (as buyer or seller).
     */
    suspend fun getOrders(
        asBuyer: Boolean = true,
        limit: Int = 20,
        skip: Int = 0
    ): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            val currentUser = ParseUser.getCurrentUser() as? User
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val query = Order.getQuery()

            // Query as buyer or seller
            if (asBuyer) {
                query.whereEqualTo(Order.KEY_BUYER, currentUser)
            } else {
                query.whereEqualTo(Order.KEY_SELLER, currentUser)
            }

            // Include related objects
            query.include(Order.KEY_PRODUCT)
            query.include(Order.KEY_BUYER)
            query.include(Order.KEY_SELLER)

            // Sort by newest first
            query.orderByDescending(ParseObject.KEY_CREATED_AT)

            // Pagination
            query.setLimit(limit)
            query.setSkip(skip)

            val orders = query.find()
            return@withContext Result.success(orders)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching orders: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get a specific order by ID.
     */
    suspend fun getOrderById(orderId: String): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val currentUser = ParseUser.getCurrentUser() as? User
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val query = Order.getQuery()

            // Include related objects
            query.include(Order.KEY_PRODUCT)
            query.include(Order.KEY_BUYER)
            query.include(Order.KEY_SELLER)

            val order = query.get(orderId)

            // Check if current user is buyer or seller
            if (order.buyer?.objectId != currentUser.objectId &&
                order.seller?.objectId != currentUser.objectId
            ) {
                return@withContext Result.failure(
                    Exception("Access denied: You are not a participant in this order")
                )
            }

            return@withContext Result.success(order)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching order by ID: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
}