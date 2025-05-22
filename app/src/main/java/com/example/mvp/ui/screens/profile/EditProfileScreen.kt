package com.example.mvp.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.mvp.data.models.User
import com.example.mvp.ui.viewmodels.ProfileViewModel
import com.example.mvp.ui.viewmodels.ProfileEffect
import com.example.mvp.ui.viewmodels.ProfileEvent
import kotlinx.coroutines.flow.collectLatest

/**
 * Composable function for editing a user's profile.
 * This screen allows users to update their profile information including username, email,
 * and profile picture. Changes are saved via events sent to the ProfileViewModel.
 *
 * @param viewModel The ProfileViewModel instance for managing profile data and events.
 * @param userId The unique identifier of the user whose profile is being edited.
 * @param onSaveClick Callback invoked when the profile is successfully updated and saved.
 * @param onCancelClick Callback invoked when the user cancels the editing process.
 * @param modifier Modifier for styling or positioning the composable.
 */
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    userId: String,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle UI effects
    LaunchedEffect(key1 = true) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ProfileEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ProfileEffect.ProfileUpdated -> {
                    Toast.makeText(
                        context,
                        "Profile updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    onSaveClick()
                }
                else -> {}
            }
        }
    }
    
    // Load user profile
    LaunchedEffect(userId) {
        viewModel.setEvent(ProfileEvent.FetchUserProfile(userId))
    }
    
    // State for form fields
    var username by remember { mutableStateOf(state.user?.username ?: "") }
    var email by remember { mutableStateOf(state.user?.email ?: "") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Update form fields when user data is loaded
    LaunchedEffect(state.user) {
        state?.user?.let { user ->
            username = user.username ?: ""
            email = user.email ?: ""
        }
    }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (state.isLoading && state.user == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (state.user == null) {
            Text(
                text = "User not found",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                // Profile Image Section
                val currentImageUrl = state.user!!.profileImage?.file?.url
                if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "New Profile Image",
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                } else if (currentImageUrl != null && currentImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = currentImageUrl,
                        contentDescription = "Current Profile Image",
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.user!!.username?.firstOrNull()?.toString()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }
                
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Change Profile Picture")
                }
                
                // Form Fields
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onCancelClick
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            state.user?.let { user ->
                                viewModel.setEvent(
                                    ProfileEvent.UpdateProfileDetails(
                                        user = user,
                                        username = username,
                                        email = email
                                    )
                                )
                                if (profileImageUri != null) {
                                    // Convert Uri to ByteArray for image upload
                                    val inputStream = context.contentResolver.openInputStream(profileImageUri!!)
                                    val imageData = inputStream?.readBytes()
                                    inputStream?.close()
                                    if (imageData != null) {
                                        viewModel.setEvent(
                                            ProfileEvent.UpdateProfileImage(
                                                user = user,
                                                imageData = imageData
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
        
        if (state.isLoading && state.user != null) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }
}