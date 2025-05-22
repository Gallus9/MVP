package com.example.mvp.ui.screens.orders

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mvp.data.models.Order
import com.example.mvp.data.models.User
import com.example.mvp.ui.viewmodels.OrderViewModel
import com.example.mvp.ui.viewmodels.OrderEffect
import com.example.mvp.ui.viewmodels.OrderEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun OrderListScreen(
    viewModel: OrderViewModel,
    user: User,
    isBuyer: Boolean = true,
    onOrderClick: (Order) -> Unit,
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
                        "Order status updated to ${effect.order.status}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    // Load orders based on whether the user is a buyer or seller
    LaunchedEffect(user, isBuyer) {
        if (isBuyer) {
            viewModel.setEvent(OrderEvent.FetchBuyerOrders(user))
        } else {
            viewModel.setEvent(OrderEvent.FetchSellerOrders(user))
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (state.isLoading && state.orders.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (state.orders.isEmpty()) {
            Text(
                text = "No orders found",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.orders) { order ->
                    OrderCard(
                        order = order,
                        onClick = { onOrderClick(order) }
                    )
                }
            }
        }
        
        if (state.isLoading && state.orders.isNotEmpty()) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = order.product?.title ?: "Unknown Item",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Status: ${order.status}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Quantity: ${order.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = "$${order.price}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}