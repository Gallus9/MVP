package com.example.mvp.marketplace.ui

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object MarketplaceDestinations {
    const val PRODUCT_LIST_ROUTE = "product_list"
    const val PRODUCT_DETAILS_ROUTE = "product_details/{productId}"
    const val ADD_PRODUCT_ROUTE = "add_product"
    
    // Helper function to generate the product details route with a specific ID
    fun productDetailsRoute(productId: String) = "product_details/$productId"
}

/**
 * Adds marketplace-related destinations to the navigation graph
 */
fun NavGraphBuilder.marketplaceNavigation(
    navController: NavController,
    onProductSelected: (String) -> Unit = { navController.navigate(MarketplaceDestinations.productDetailsRoute(it)) }
) {
    composable(MarketplaceDestinations.PRODUCT_LIST_ROUTE) {
        val viewModel = hiltViewModel<MarketplaceViewModel>()
        
        ProductListScreen(
            viewModel = viewModel,
            onProductClick = { product -> onProductSelected(product.id) },
            onAddProductClick = { navController.navigate(MarketplaceDestinations.ADD_PRODUCT_ROUTE) }
        )
    }
    
    // Other marketplace routes can be added here
}

/**
 * Helper functions for navigating to marketplace screens
 */
fun NavController.navigateToMarketplace() {
    navigate(MarketplaceDestinations.PRODUCT_LIST_ROUTE)
}