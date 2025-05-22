package com.example.mvp.auth.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    navigateToHome: () -> Unit,
    navigateToLogin: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle UI effects
    LaunchedEffect(key1 = true) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AuthEffect.SignupSuccess -> {
                    navigateToHome()
                }
                is AuthEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                else -> {} // Other effects not relevant for this screen
            }
        }
    }

    // UI state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("GeneralUser") } // Default to general user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sign Up",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            isError = password != confirmPassword && confirmPassword.isNotEmpty(),
            supportingText = {
                if (password != confirmPassword && confirmPassword.isNotEmpty()) {
                    Text("Passwords do not match")
                }
            }
        )
        
        // Role selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Select Role:", Modifier.padding(end = 16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = role == "GeneralUser",
                    onClick = { role = "GeneralUser" }
                )
                Text("Buyer", Modifier.padding(start = 4.dp, end = 16.dp))
                
                RadioButton(
                    selected = role == "Farmer",
                    onClick = { role = "Farmer" }
                )
                Text("Farmer", Modifier.padding(start = 4.dp))
            }
        }

        Button(
            onClick = {
                if (password == confirmPassword) {
                    viewModel.setEvent(AuthEvent.Signup(email, password, role))
                } else {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = email.isNotEmpty() && 
                    password.isNotEmpty() && 
                    confirmPassword.isNotEmpty() && 
                    password == confirmPassword && 
                    !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign Up")
            }
        }

        TextButton(
            onClick = navigateToLogin,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Already have an account? Log In")
        }
    }
}