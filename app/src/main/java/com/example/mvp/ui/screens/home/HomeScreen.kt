package com.example.mvp.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mvp.ui.components.FeaturedProducts
import androidx.navigation.NavController
import com.example.mvp.ui.viewmodels.ProductViewModel

@Composable
fun HomeScreen(navController: NavController, productViewModel: ProductViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "Welcome to MVP Marketplace")
        FeaturedProducts(
            navController = navController,
            productViewModel = productViewModel
        )
    }
}