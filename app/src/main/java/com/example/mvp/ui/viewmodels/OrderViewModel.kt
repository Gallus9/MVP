package com.example.mvp.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.mvp.core.base.BaseViewModel
import com.example.mvp.core.base.UiEffect
import com.example.mvp.core.base.UiEvent
import com.example.mvp.core.base.UiState
import com.example.mvp.data.models.Order
import com.example.mvp.data.models.ProductListing
import com.example.mvp.data.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume

/**
 * ViewModel for managing order-related data and operations.
 * Handles fetching orders for buyers and sellers, creating new orders,
 * updating order statuses, and fetching specific order details.
 * Extends BaseViewModel to follow the event-state-effect pattern.
 */
class OrderViewModel : BaseViewModel<OrderEvent, OrderState, OrderEffect>() {
    
    companion object {
        private const val TAG = "OrderViewModel"
    }
    
    private val scope: CoroutineScope = object : CoroutineScope {
        override val coroutineContext = Dispatchers.Main + Job()
    }
    
    // Fetch all orders for a user (as buyer)
    private fun fetchBuyerOrders(user: User) {
        scope.launch {
            try {
                setState { copy(isLoading = true) }
                Log.d(TAG, "Fetching buyer orders for user: ${user.username}")
                
                val orders = getBuyerOrders(user)
                setState { copy(orders = orders, isLoading = false) }
                Log.d(TAG, "Successfully fetched ${orders.size} buyer orders")
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { OrderEffect.ShowError(e.message ?: "Failed to fetch orders") }
                Log.e(TAG, "Error fetching buyer orders", e)
            }
        }
    }
    
    // Fetch all orders for a user (as seller)
    private fun fetchSellerOrders(user: User) {
        scope.launch {
            try {
                setState { copy(isLoading = true) }
                Log.d(TAG, "Fetching seller orders for user: ${user.username}")
                
                val orders = getSellerOrders(user)
                setState { copy(orders = orders, isLoading = false) }
                Log.d(TAG, "Successfully fetched ${orders.size} seller orders")
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { OrderEffect.ShowError(e.message ?: "Failed to fetch orders") }
                Log.e(TAG, "Error fetching seller orders", e)
            }
        }
    }
    
    // Get a specific order by ID
    private fun fetchOrderById(orderId: String) {
        scope.launch {
            try {
                setState { copy(isLoading = true) }
                Log.d(TAG, "Fetching order details for ID: $orderId")
                
                val order = getOrderById(orderId)
                if (order != null) {
                    setState { copy(currentOrder = order, isLoading = false) }
                    Log.d(TAG, "Successfully fetched order details")
                } else {
                    setState { copy(isLoading = false) }
                    setEffect { OrderEffect.ShowError("Order not found") }
                    Log.w(TAG, "Order not found for ID: $orderId")
                }
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { OrderEffect.ShowError(e.message ?: "Failed to fetch order details") }
                Log.e(TAG, "Error fetching order details", e)
            }
        }
    }
    
    // Create a new order
    private fun createOrder(product: ProductListing, buyer: User, quantity: Int) {
        scope.launch {
            try {
                setState { copy(isLoading = true) }
                Log.d(TAG, "Creating new order for product: ${product.objectId}")
                
                val order = Order()
                order.product = product
                order.buyer = buyer
                order.seller = product.seller
                order.status = Order.STATUS_PENDING
                order.price = product.price
                order.quantity = quantity
                
                saveOrder(order)
                
                setState { 
                    copy(
                        isLoading = false,
                        orders = orders + order
                    )
                }
                setEffect { OrderEffect.OrderCreated(order) }
                Log.d(TAG, "Order created successfully with ID: ${order.objectId}")
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { OrderEffect.ShowError(e.message ?: "Failed to create order") }
                Log.e(TAG, "Error creating order", e)
            }
        }
    }
    
    // Update order status
    private fun updateOrderStatus(order: Order, newStatus: String) {
        scope.launch {
            try {
                setState { copy(isLoading = true) }
                Log.d(TAG, "Updating order status to $newStatus for order: ${order.objectId}")
                
                order.status = newStatus
                saveOrder(order)
                
                // Update the order in the list
                val updatedOrders = uiState.value.orders.map {
                    if (it.objectId == order.objectId) order else it
                }
                
                setState { 
                    copy(
                        isLoading = false,
                        orders = updatedOrders,
                        currentOrder = if (uiState.value.currentOrder?.objectId == order.objectId) order else uiState.value.currentOrder
                    )
                }
                setEffect { OrderEffect.OrderUpdated(order) }
                Log.d(TAG, "Order status updated successfully")
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                setEffect { OrderEffect.ShowError(e.message ?: "Failed to update order status") }
                Log.e(TAG, "Error updating order status", e)
            }
        }
    }
    
    // Helper coroutine methods
    private suspend fun getBuyerOrders(user: User): List<Order> = suspendCancellableCoroutine { continuation ->
        val query = Order.getQuery()
            .whereEqualTo(Order.KEY_BUYER, user)
            .include(Order.KEY_PRODUCT)
            .include(Order.KEY_SELLER)
            .orderByDescending("createdAt")
        
        query.findInBackground { orders, e ->
            if (e == null) {
                continuation.resume(orders)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private suspend fun getSellerOrders(user: User): List<Order> = suspendCancellableCoroutine { continuation ->
        val query = Order.getQuery()
            .whereEqualTo(Order.KEY_SELLER, user)
            .include(Order.KEY_PRODUCT)
            .include(Order.KEY_BUYER)
            .orderByDescending("createdAt")
        
        query.findInBackground { orders, e ->
            if (e == null) {
                continuation.resume(orders)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private suspend fun getOrderById(orderId: String): Order? = suspendCancellableCoroutine { continuation ->
        val query = Order.getQuery()
            .whereEqualTo("objectId", orderId)
            .include(Order.KEY_PRODUCT)
            .include(Order.KEY_BUYER)
            .include(Order.KEY_SELLER)
        
        query.getFirstInBackground { order, e ->
            if (e == null) {
                continuation.resume(order)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private suspend fun saveOrder(order: Order): Boolean = suspendCancellableCoroutine { continuation ->
        order.saveInBackground { e ->
            if (e == null) {
                continuation.resume(true)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
    
    override fun createInitialState(): OrderState = OrderState()
    
    override fun handleEvent(event: OrderEvent) {
        when (event) {
            is OrderEvent.FetchBuyerOrders -> fetchBuyerOrders(event.user)
            is OrderEvent.FetchSellerOrders -> fetchSellerOrders(event.user)
            is OrderEvent.FetchOrderById -> fetchOrderById(event.orderId)
            is OrderEvent.CreateOrder -> createOrder(event.product, event.buyer, event.quantity)
            is OrderEvent.UpdateOrderStatus -> updateOrderStatus(event.order, event.newStatus)
        }
    }
}

sealed class OrderEvent : UiEvent {
    data class FetchBuyerOrders(val user: User) : OrderEvent()
    data class FetchSellerOrders(val user: User) : OrderEvent()
    data class FetchOrderById(val orderId: String) : OrderEvent()
    data class CreateOrder(val product: ProductListing, val buyer: User, val quantity: Int) : OrderEvent()
    data class UpdateOrderStatus(val order: Order, val newStatus: String) : OrderEvent()
}

data class OrderState(
    val orders: List<Order> = emptyList(),
    val currentOrder: Order? = null,
    val isLoading: Boolean = false
) : UiState

sealed class OrderEffect : UiEffect {
    data class ShowError(val message: String) : OrderEffect()
    data class OrderCreated(val order: Order) : OrderEffect()
    data class OrderUpdated(val order: Order) : OrderEffect()
}