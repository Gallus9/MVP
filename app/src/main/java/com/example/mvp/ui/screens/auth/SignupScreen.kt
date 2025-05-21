package com.example.mvp.ui.screens.auth

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mvp.data.models.User
import com.example.mvp.ui.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(
    onSignupSuccess: (User) -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val viewModel: MainViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Signup Screen")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = {
            coroutineScope.launch {
                viewModel.register("username", "password", "username", true)
                    .onSuccess { user ->
                        onSignupSuccess(user)
                    }
                    .onFailure { error ->
                        Log.e("Signup", "Failed: ${error.message}")
                    }
            }
        }) {
            Text("Sign Up")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = onNavigateToLogin) {
            Text("Back to Login")
        }
    }
}