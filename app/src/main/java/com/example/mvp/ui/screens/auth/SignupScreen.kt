package com.example.mvp.ui.screens.auth

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mvp.data.models.User
import com.example.mvp.ui.viewmodels.AuthState
import com.example.mvp.ui.viewmodels.AuthViewModel

@Composable
fun SignupScreen(
    onSignupSuccess: (User) -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val viewModel: AuthViewModel = viewModel()
    val authState by viewModel.authState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(User.ROLE_GENERAL_USER) }
    var errorMessage by remember { mutableStateOf("") }
    
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            onSignupSuccess(user)
        }
    }
    
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Error -> {
                errorMessage = (authState as AuthState.Error).message
                Log.e("SignupScreen", "Auth Error: $errorMessage")
            }
            else -> {}
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sign Up for Rooster Enthusiasts",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Select Role",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { role = User.ROLE_GENERAL_USER },
                modifier = Modifier.weight(1f),
                colors = if (role == User.ROLE_GENERAL_USER) 
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) 
                else 
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("General User")
            }
            
            Button(
                onClick = { role = User.ROLE_FARMER },
                modifier = Modifier.weight(1f),
                colors = if (role == User.ROLE_FARMER) 
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) 
                else 
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Farmer")
            }
            
            Button(
                onClick = { role = User.ROLE_ENTHUSIAST },
                modifier = Modifier.weight(1f),
                colors = if (role == User.ROLE_ENTHUSIAST) 
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) 
                else 
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Enthusiast")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (validate(email, password, name)) {
                    errorMessage = ""
                    viewModel.signup(email, password, role)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign Up")
            }
        }
        
        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Already have an account? Log In")
        }
    }
}

private fun validate(email: String, password: String, name: String): Boolean {
    return when {
        email.isBlank() -> false.also { Log.d("Signup", "Validation failed: Email is blank") }
        !email.contains('@') -> false.also { Log.d("Signup", "Validation failed: Invalid email format") }
        password.length < 6 -> false.also { Log.d("Signup", "Validation failed: Password too short") }
        name.isBlank() -> false.also { Log.d("Signup", "Validation failed: Name is blank") }
        else -> true
    }
}