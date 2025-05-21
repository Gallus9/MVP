package com.example.mvp.ui.navigation

/**
 * Defines all navigation routes for the application.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Home : Screen("home")
    object Marketplace : Screen("marketplace")
    object Explore : Screen("explore")
    object CreateListing : Screen("create_listing")
    object Community : Screen("community")
    object Profile : Screen("profile")
    object Cart : Screen("cart")
    
    // Detail screens with parameters
    object ProductDetails : Screen("product_details/{productId}") {
        fun createRoute(productId: String) = "product_details/$productId"
    }
    
    // Order screens
    object Orders : Screen("orders")
    object OrderDetails : Screen("order_details/{orderId}") {
        fun createRoute(orderId: String) = "order_details/$orderId"
    }
}