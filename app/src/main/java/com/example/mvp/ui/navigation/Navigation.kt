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
import com.example.mvp.ui.screens.community.PostDetailsScreen
import com.example.mvp.ui.screens.explore.ExploreScreen
import com.example.mvp.ui.screens.home.HomeScreen
import com.example.mvp.ui.screens.marketplace.CreateListingScreen
import com.example.mvp.ui.screens.marketplace.MarketplaceScreen
import com.example.mvp.ui.screens.marketplace.ProductDetailsScreen
import com.example.mvp.ui.screens.orders.OrderListScreen
import com.example.mvp.ui.screens.profile.EditProfileScreen
import com.example.mvp.ui.screens.profile.ProfileScreen
import com.example.mvp.ui.navigation.Screen
import com.example.mvp.ui.viewmodels.ProductViewModel
import com.example.mvp.ui.viewmodels.OrderViewModel
import com.example.mvp.ui.viewmodels.ProfileViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavBackStackEntry
import com.example.mvp.ui.components.BottomNavBar
import com.example.mvp.ui.components.AppScaffold
import com.example.mvp.ui.screens.farmer.FarmerDashboardScreen
import com.example.mvp.ui.screens.orders.OrderDetailsScreen

/**
 * Composable function for setting up the app's navigation.
 * This function initializes the navigation controller and determines the start destination
 * based on the user's authentication status. It serves as the entry point for the app's navigation graph.
 *
 * @param currentUser The currently authenticated user, if any. Determines the start destination.
 * @param onLogout Callback invoked when the user logs out.
 * @param productViewModel ViewModel for managing product-related data.
 * @param orderViewModel ViewModel for managing order-related data.
 * @param profileViewModel ViewModel for managing profile-related data.
 */
@Composable
fun AppNavigation(
    currentUser: User?,
    onLogout: () -> Unit,
    productViewModel: ProductViewModel,
    orderViewModel: OrderViewModel,
    profileViewModel: ProfileViewModel
) {
    val navController = rememberNavController()
    AppNavHost(navController, currentUser, onLogout, productViewModel, orderViewModel, profileViewModel)
}

/**
 * Composable function for hosting the app's navigation graph.
 * Defines all navigable routes and their corresponding screens. The start destination
 * is determined based on whether a user is logged in.
 *
 * @param navController The NavHostController for managing navigation.
 * @param currentUser The currently authenticated user, if any. Affects navigation logic.
 * @param onLogout Callback invoked when the user logs out.
 * @param productViewModel ViewModel for managing product-related data.
 * @param orderViewModel ViewModel for managing order-related data.
 * @param profileViewModel ViewModel for managing profile-related data.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    currentUser: User?,
    onLogout: () -> Unit,
    productViewModel: ProductViewModel,
    orderViewModel: OrderViewModel,
    profileViewModel: ProfileViewModel
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
                currentUser?.let { user ->
                    ProfileScreen(
                        viewModel = profileViewModel,
                        userId = user.objectId ?: "",
                        onEditProfileClick = { navController.navigate("edit_profile") }
                    )
                } ?: Text("No user data available")
            }
        }
        
        composable("edit_profile") {
            AppScaffold(navController = navController) {
                currentUser?.let { user ->
                    EditProfileScreen(
                        viewModel = profileViewModel,
                        userId = user.objectId ?: "",
                        onSaveClick = { navController.popBackStack() },
                        onCancelClick = { navController.popBackStack() }
                    )
                } ?: Text("No user data available")
            }
        }
        
        composable(Screen.ProductDetails.route) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            AppScaffold(navController = navController) {
                ProductDetailsScreen(
                    viewModel = productViewModel,
                    productId = productId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        
        composable(Screen.Orders.route) {
            AppScaffold(navController = navController) {
                currentUser?.let { user ->
                    OrderListScreen(
                        viewModel = orderViewModel,
                        user = user,
                        isBuyer = !user.isFarmer(),
                        onOrderClick = { order ->
                            navController.navigate(Screen.OrderDetails.createRoute(order.objectId ?: ""))
                        }
                    )
                } ?: Text("No user data available")
            }
        }
        
        composable(Screen.OrderDetails.route) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            AppScaffold(navController = navController) {
                OrderDetailsScreen(
                    viewModel = orderViewModel,
                    orderId = orderId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.FarmerDashboard.route) {
            AppScaffold(navController = navController) {
                FarmerDashboardScreen(
                    productViewModel = productViewModel,
                    orderViewModel = orderViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.PostDetails.route) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            AppScaffold(navController = navController) {
                PostDetailsScreen(
                    postId = postId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}