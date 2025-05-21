package com.example.mvp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvp.data.models.Order
import com.example.mvp.data.models.ProductListing
import com.example.mvp.data.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume

sealed class OrderListState {
    object Idle : OrderListState()
    object Loading : OrderListState()
    data class Error(val message: String) : OrderListState()
    data class Success(val orders: List<Order>) : OrderListState()
}

sealed class OrderDetailState {
    object Idle : OrderDetailState()
    object Loading : OrderDetailState()
    data class Error(val message: String) : OrderDetailState()
    data class Success(val order: Order) : OrderDetailState()
}

sealed class OrderCreateState {
    object Idle : OrderCreateState()
    object Loading : OrderCreateState()
    data class Error(val message: String) : OrderCreateState()
    data class Success(val order: Order) : OrderCreateState()
}

class OrderViewModel : ViewModel() {
    
    private val _orderListState = MutableStateFlow<OrderListState>(OrderListState.Idle)
    val orderListState: StateFlow<OrderListState> = _orderListState.asStateFlow()
    
    private val _orderDetailState = MutableStateFlow<OrderDetailState>(OrderDetailState.Idle)
    val orderDetailState: StateFlow<OrderDetailState> = _orderDetailState.asStateFlow()
    
    private val _orderCreateState = MutableStateFlow<OrderCreateState>(OrderCreateState.Idle)
    val orderCreateState: StateFlow<OrderCreateState> = _orderCreateState.asStateFlow()
    
    // Fetch all orders for a user (as buyer)
    fun fetchBuyerOrders(user: User) {
        viewModelScope.launch {
            try {
                _orderListState.value = OrderListState.Loading
                
                val orders = getBuyerOrders(user)
                _orderListState.value = OrderListState.Success(orders)
            } catch (e: Exception) {
                _orderListState.value = OrderListState.Error(e.message ?: "Failed to fetch orders")
            }
        }
    }
    
    // Fetch all orders for a user (as seller)
    fun fetchSellerOrders(user: User) {
        viewModelScope.launch {
            try {
                _orderListState.value = OrderListState.Loading
                
                val orders = getSellerOrders(user)
                _orderListState.value = OrderListState.Success(orders)
            } catch (e: Exception) {
                _orderListState.value = OrderListState.Error(e.message ?: "Failed to fetch orders")
            }
        }
    }
    
    // Get a specific order by ID
    fun fetchOrderById(orderId: String) {
        viewModelScope.launch {
            try {
                _orderDetailState.value = OrderDetailState.Loading
                
                val order = getOrderById(orderId)
                if (order != null) {
                    _orderDetailState.value = OrderDetailState.Success(order)
                } else {
                    _orderDetailState.value = OrderDetailState.Error("Order not found")
                }
            } catch (e: Exception) {
                _orderDetailState.value = OrderDetailState.Error(e.message ?: "Failed to fetch order details")
            }
        }
    }
    
    // Create a new order
    fun createOrder(product: ProductListing, buyer: User, quantity: Int) {
        viewModelScope.launch {
            try {
                _orderCreateState.value = OrderCreateState.Loading
                
                val order = Order()
                order.product = product
                order.buyer = buyer
                order.seller = product.seller
                order.status = Order.STATUS_PENDING
                order.price = product.price
                order.quantity = quantity
                
                saveOrder(order)
                
                _orderCreateState.value = OrderCreateState.Success(order)
                
                // Refresh orders list
                fetchBuyerOrders(buyer)
            } catch (e: Exception) {
                _orderCreateState.value = OrderCreateState.Error(e.message ?: "Failed to create order")
            }
        }
    }
    
    // Update order status
    fun updateOrderStatus(order: Order, newStatus: String) {
        viewModelScope.launch {
            try {
                _orderDetailState.value = OrderDetailState.Loading
                
                order.status = newStatus
                saveOrder(order)
                
                _orderDetailState.value = OrderDetailState.Success(order)
            } catch (e: Exception) {
                _orderDetailState.value = OrderDetailState.Error(e.message ?: "Failed to update order status")
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
}