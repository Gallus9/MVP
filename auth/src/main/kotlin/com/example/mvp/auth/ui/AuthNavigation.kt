package com.example.mvp.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

object AuthDestinations {
    const val LOGIN_ROUTE = "login"
    const val SIGNUP_ROUTE = "signup"
}

/**
 * Adds auth-related destinations to the navigation graph
 */
fun NavGraphBuilder.authNavigation(
    navController: NavHostController,
    onAuthSuccess: () -> Unit
) {
    composable(AuthDestinations.LOGIN_ROUTE) {
        val viewModel = hiltViewModel<AuthViewModel>()
        val state by viewModel.uiState.collectAsState()
        
        // Check if user is already logged in
        LaunchedEffect(state.currentUser) {
            if (state.currentUser != null) {
                onAuthSuccess()
            }
        }
        
        LoginScreen(
            viewModel = viewModel,
            navigateToHome = onAuthSuccess,
            navigateToSignUp = { navController.navigate(AuthDestinations.SIGNUP_ROUTE) }
        )
    }
    
    composable(AuthDestinations.SIGNUP_ROUTE) {
        val viewModel = hiltViewModel<AuthViewModel>()
        
        SignUpScreen(
            viewModel = viewModel,
            navigateToHome = onAuthSuccess,
            navigateToLogin = { navController.navigateUp() }
        )
    }
}

/**
 * Helper function to navigate to the auth flow
 */
fun NavHostController.navigateToAuth() {
    navigate(AuthDestinations.LOGIN_ROUTE) {
        // Clear the back stack when navigating to auth
        popUpTo(0) { inclusive = true }
    }
}