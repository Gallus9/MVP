package com.example.mvp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.mvp.data.models.ProductListing
import com.example.mvp.ui.viewmodels.ProductListState
import com.example.mvp.ui.viewmodels.ProductViewModel

@Composable
fun FeaturedProducts(
    navController: NavController,
    productViewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val productListState by productViewModel.productListState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        productViewModel.fetchProducts(10)
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        when (val state = productListState) {
            is ProductListState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ProductListState.Success -> {
                if (state.products.isEmpty()) {
                    Text(
                        "No products available",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.products) { product ->
                            ProductCard(
                                product = product,
                                onClick = {
                                    navController.navigate(
                                        "product_details/${product.objectId}"
                                    )
                                }
                            )
                        }
                    }
                }
            }
            is ProductListState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            else -> { /* Idle state, do nothing */ }
        }
    }
}