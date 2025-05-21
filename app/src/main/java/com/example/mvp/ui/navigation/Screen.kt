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
    object Profile : Screen("profile")
    object Cart : Screen("cart")
    object FarmerDashboard : Screen("farmer_dashboard")
    
    // Community screens
    object CommunityFeed : Screen("community_feed")
    object CreatePost : Screen("create_post")
    object PostDetails : Screen("post_details/{postId}") {
        fun createRoute(postId: String) = "post_details/$postId"
    }
    
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