package com.example.mvp.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mvp.data.models.User
import com.example.mvp.ui.screens.auth.LoginScreen
import com.example.mvp.ui.screens.auth.SignupScreen
import com.example.mvp.ui.screens.cart.CartScreen
import com.example.mvp.ui.screens.community.CommunityScreen
import com.example.mvp.ui.screens.explore.ExploreScreen
import com.example.mvp.ui.screens.home.HomeScreen
import com.example.mvp.ui.screens.marketplace.CreateListingScreen
import com.example.mvp.ui.screens.marketplace.MarketplaceScreen
import com.example.mvp.ui.screens.profile.ProfileScreen
import com.example.mvp.ui.navigation.Screen
import com.example.mvp.ui.viewmodels.ProductViewModel
import com.example.mvp.ui.viewmodels.OrderViewModel

@Composable
fun AppNavigation(
    currentUser: User?,
    onLogout: () -> Unit,
    productViewModel: ProductViewModel,
    orderViewModel: OrderViewModel
) {
    val navController = rememberNavController()
    AppNavHost(navController, currentUser, onLogout, productViewModel, orderViewModel)
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    currentUser: User?,
    onLogout: () -> Unit,
    productViewModel: ProductViewModel,
    orderViewModel: OrderViewModel
) {
    val startDestination = if (currentUser == null) Screen.Login.route else Screen.Home.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { user ->
                    val destination = if (user.isFarmer()) Screen.Home.route else Screen.Marketplace.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                }
            )
        }
        
        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = { user ->
                    val destination = if (user.isFarmer()) Screen.Home.route else Screen.Marketplace.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(navController, productViewModel)
        }
        
        composable(Screen.Marketplace.route) {
            MarketplaceScreen(navController)
        }
        
        composable(Screen.Explore.route) {
            ExploreScreen(navController)
        }
        
        composable(Screen.CreateListing.route) {
            CreateListingScreen(navController)
        }
        
        composable(Screen.Community.route) {
            CommunityScreen(navController)
        }
        
        composable(Screen.Cart.route) {
            CartScreen(navController)
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                navController = navController,
                onLogout = onLogout
            )
        }
        
        composable(Screen.ProductDetails.route) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            // Placeholder for ProductDetailsScreen
            Text("Product Details for $productId")
            // Once implemented, replace with:
            // ProductDetailsScreen(navController, productViewModel, productId)
        }
        
        composable(Screen.Orders.route) {
            Text("Orders")
            // Once implemented, replace with:
            // OrdersScreen(navController, orderViewModel)
        }
        
        composable(Screen.OrderDetails.route) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            Text("Order Details for $orderId")
            // Once implemented, replace with:
            // OrderDetailsScreen(navController, orderViewModel, orderId)
        }
    }
}