package com.example.mvp.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mvp.data.models.User
import com.example.mvp.ui.screens.auth.LoginScreen
import com.example.mvp.ui.screens.auth.SignupScreen
import com.example.mvp.ui.screens.cart.CartScreen
import com.example.mvp.ui.screens.community.CommunityFeedScreen
import com.example.mvp.ui.screens.community.CreatePostScreen
import com.example.mvp.ui.screens.explore.ExploreScreen
import com.example.mvp.ui.screens.home.HomeScreen
import com.example.mvp.ui.screens.marketplace.CreateListingScreen
import com.example.mvp.ui.screens.marketplace.MarketplaceScreen
import com.example.mvp.ui.navigation.Screen
import com.example.mvp.ui.viewmodels.ProductDetailState
import com.example.mvp.ui.viewmodels.ProductViewModel
import com.example.mvp.ui.viewmodels.OrderViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavBackStackEntry
import com.example.mvp.ui.components.BottomNavBar
import com.example.mvp.ui.components.AppScaffold

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
            AppScaffold(navController = navController) {
                HomeScreen(navController, productViewModel)
            }
        }
        
        composable(Screen.Marketplace.route) {
            AppScaffold(navController = navController) {
                MarketplaceScreen(navController)
            }
        }
        
        composable(Screen.Explore.route) {
            AppScaffold(navController = navController) {
                ExploreScreen(navController)
            }
        }
        
        composable(Screen.CreateListing.route) {
            CreateListingScreen(navController)
        }
        
        composable(Screen.CommunityFeed.route) {
            AppScaffold(navController = navController) {
                CommunityFeedScreen(
                    onNavigateToCreatePost = { navController.navigate(Screen.CreatePost.route) },
                    onNavigateToPostDetails = { post -> 
                        navController.navigate(Screen.PostDetails.createRoute(post.objectId))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        
        composable(Screen.CreatePost.route) {
            CreatePostScreen(
                onBackClick = { navController.popBackStack() },
                onPostCreated = { 
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Cart.route) {
            AppScaffold(navController = navController) {
                CartScreen(navController)
            }
        }
        
        composable(Screen.Profile.route) {
            AppScaffold(navController = navController) {
                Text("Profile Placeholder")
            }
        }
        
        composable(Screen.ProductDetails.route) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            Text("Product Details for $productId")
            // TODO: Implement ProductDetailsScreen with traceability info
        }
        
        composable(Screen.Orders.route) {
            Text("Orders")
        }
        
        composable(Screen.OrderDetails.route) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            Text("Order Details for $orderId")
            // Once implemented, replace with:
            // OrderDetailsScreen(navController, orderViewModel, orderId)
        }

        composable(Screen.FarmerDashboard.route) {
            Text("Farmer Dashboard Placeholder")
            // TODO: Implement Farmer Dashboard with metrics like active listings and total orders
        }

        composable(Screen.PostDetails.route) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            Text("Post Details for $postId") 
            // TODO: Implement PostDetailScreen
        }
    }
}