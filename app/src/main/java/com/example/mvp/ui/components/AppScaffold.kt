package com.example.mvp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * A scaffold component that wraps content with a bottom navigation bar.
 * Use this for screens that should display the bottom navigation.
 */
@Composable
fun AppScaffold(
    navController: NavController,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = { BottomNavBar(navController = navController) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content()
        }
    }
}