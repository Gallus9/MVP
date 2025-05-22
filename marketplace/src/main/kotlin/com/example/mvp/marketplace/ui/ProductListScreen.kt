package com.example.mvp.marketplace.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.mvp.marketplace.domain.model.Product
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProductListScreen(
    viewModel: MarketplaceViewModel,
    onProductClick: (Product) -> Unit,
    onAddProductClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle UI effects
    LaunchedEffect(key1 = true) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is MarketplaceEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MarketplaceEffect.ProductCreated -> {
                    Toast.makeText(
                        context, 
                        "Product ${effect.product.name} created successfully", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProductClick
            ) {
                Text("+")
            }
        }
    ) { contentPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            if (state.isLoading && state.products.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.products.isEmpty()) {
                // Show empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No products found",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    if (state.filteredCategory != null || state.searchQuery != null) {
                        Button(
                            onClick = { viewModel.setEvent(MarketplaceEvent.LoadProducts()) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Clear filters")
                        }
                    } else {
                        Button(
                            onClick = onAddProductClick,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Add your first product")
                        }
                    }
                }
            } else {
                // Show products grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.products) { product ->
                        ProductCard(
                            product = product,
                            onClick = { onProductClick(product) }
                        )
                    }
                }
            }
            
            // Show loading indicator when refreshing
            if (state.isLoading && state.products.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            // Product image
            if (product.images.isNotEmpty()) {
                AsyncImage(
                    model = product.images.first(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Image")
                }
            }
            
            // Product details
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "$${product.price}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                if (product.isOrganic) {
                    Text(
                        text = "Organic",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}