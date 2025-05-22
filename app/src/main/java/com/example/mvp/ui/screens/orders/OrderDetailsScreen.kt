package com.example.mvp.ui.screens.orders

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mvp.data.models.Order
import com.example.mvp.ui.viewmodels.OrderViewModel
import com.example.mvp.ui.viewmodels.OrderEffect
import com.example.mvp.ui.viewmodels.OrderEvent
import com.parse.ParseUser
import kotlinx.coroutines.flow.collectLatest

/**
 * Composable function for displaying the details of a specific order.
 * This screen shows order information such as product title, status, quantity, price,
 * and buyer/seller details. For sellers, it provides options to update the order status.
 *
 * @param viewModel The OrderViewModel instance for managing order data and events.
 * @param orderId The unique identifier of the order to display.
 * @param onBackClick Callback invoked when the user navigates back from this screen.
 * @param modifier Modifier for styling or positioning the composable.
 */
@Composable
fun OrderDetailsScreen(
    viewModel: OrderViewModel,
    orderId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle UI effects
    LaunchedEffect(key1 = true) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is OrderEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is OrderEffect.OrderCreated -> {
                    Toast.makeText(
                        context,
                        "Order for ${effect.order.product?.title ?: "item"} created",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is OrderEffect.OrderUpdated -> {
                    Toast.makeText(
                        context,
                        "Order status updated to ${effect.order.status ?: "Unknown"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    // Load order details
    LaunchedEffect(orderId) {
        viewModel.setEvent(OrderEvent.FetchOrderById(orderId))
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { contentPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            if (state.isLoading && state.currentOrder == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.currentOrder == null) {
                Text(
                    text = "Order not found",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                state.currentOrder?.let { order ->
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = order.product?.title ?: "Unknown Item",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        
                        Text(
                            text = "Order ID: ${order.objectId ?: "N/A"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Text(
                            text = "Status: ${order.status ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Text(
                            text = "Quantity: ${order.quantity}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Text(
                            text = "Price: $${order.price}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        order.buyer?.let { buyer ->
                            Text(
                                text = "Buyer: ${buyer.username ?: "Unknown"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        order.seller?.let { seller ->
                            Text(
                                text = "Seller: ${seller.username ?: "Unknown"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        // Add status update options for sellers
                        val currentUser = ParseUser.getCurrentUser()
                        if (currentUser != null && order.seller != null && currentUser.getObjectId() == order.seller!!.getObjectId()) {
                            var expanded by remember { mutableStateOf(false) }
                            val statusOptions = listOf("Pending", "Processing", "Shipped", "Delivered", "Cancelled")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                Button(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Update Status")
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    statusOptions.forEach { status ->
                                        DropdownMenuItem(
                                            text = { Text(status) },
                                            onClick = {
                                                viewModel.setEvent(OrderEvent.UpdateOrderStatus(order, status))
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // TODO: Add traceability information if available
                    }
                }
            }
            
            if (state.isLoading && state.currentOrder != null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}