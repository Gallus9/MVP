package com.example.mvp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.mvp.ui.navigation.Screen

@Composable
fun BottomNavBar(
    navController: NavController
) {
    val items = listOf(
        NavItem(
            name = "Home",
            route = Screen.Home.route,
            icon = Icons.Filled.Home
        ),
        NavItem(
            name = "Market",
            route = Screen.Marketplace.route,
            icon = Icons.AutoMirrored.Filled.List
        ),
        NavItem(
            name = "Community",
            route = Screen.CommunityFeed.route,
            icon = Icons.Filled.Info
        ),
        NavItem(
            name = "Profile",
            route = Screen.Profile.route,
            icon = Icons.Filled.Person
        ),
        NavItem(
            name = "Cart",
            route = Screen.Cart.route,
            icon = Icons.Filled.ShoppingCart
        )
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.name) },
                label = { Text(text = item.name) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class NavItem(
    val name: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)