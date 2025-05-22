package com.example.mvp.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.mvp.data.models.User
import com.example.mvp.data.models.Feedback
import com.example.mvp.ui.viewmodels.ProfileViewModel
import com.example.mvp.ui.viewmodels.ProfileEffect
import com.example.mvp.ui.viewmodels.ProfileEvent
import kotlinx.coroutines.flow.collectLatest

/**
 * Composable function for displaying a user's profile information.
 * This screen shows user details such as username, email, role, and profile image,
 * along with a list of feedback received by the user. It also provides an option
 * to edit the profile.
 *
 * @param viewModel The ProfileViewModel instance for managing profile data and events.
 * @param userId The unique identifier of the user whose profile is being displayed.
 * @param onEditProfileClick Callback invoked when the user chooses to edit their profile.
 * @param modifier Modifier for styling or positioning the composable.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    userId: String,
    onEditProfileClick: () -> Unit,
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
                }
                is ProfileEffect.FeedbackGiven -> {
                    Toast.makeText(
                        context,
                        "Feedback submitted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    // Load user profile
    LaunchedEffect(userId) {
        viewModel.setEvent(ProfileEvent.FetchUserProfile(userId))
        state.user?.let {
            viewModel.setEvent(ProfileEvent.FetchUserFeedback(it))
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
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    ProfileHeader(
                        user = state.user!!,
                        onEditClick = onEditProfileClick
                    )
                }
                
                item {
                    Text(
                        text = "Feedback",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                
                if (state.isLoadingFeedback) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .wrapContentWidth(align = Alignment.CenterHorizontally)
                        )
                    }
                } else if (state.feedbacks.isEmpty()) {
                    item {
                        Text(
                            text = "No feedback yet",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    items(state.feedbacks) { feedback ->
                        FeedbackCard(feedback = feedback)
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

/**
 * Composable for displaying the user profile header with basic information.
 * This includes the profile image, username, email, role, and an edit profile button.
 *
 * @param user The User object containing profile data.
 * @param onEditClick Callback invoked when the user clicks the "Edit Profile" button.
 * @param modifier Modifier for styling or positioning the composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileHeader(
    user: User,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile image
            val profileImage = user.profileImage
            val profileImageUrl = profileImage?.file?.url
            if (profileImage != null && profileImageUrl != null && profileImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 8.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.username?.firstOrNull()?.toString()?.uppercase() ?: "U",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }
            
            Text(
                text = user.username ?: "Unknown User",
                style = MaterialTheme.typography.headlineMedium
            )
            
            val email = user.email
            if (email != null && email.isNotEmpty()) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Text(
                text = "Role: ${user.roleAsString}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Button(
                onClick = onEditClick,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Edit Profile")
            }
        }
    }
}

/**
 * Composable for displaying a single feedback card.
 * This includes the feedback sender's username, rating, and comment.
 *
 * @param feedback The Feedback object containing the feedback data.
 * @param modifier Modifier for styling or positioning the composable.
 */
@Composable
fun FeedbackCard(
    feedback: Feedback,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "From: ${feedback.fromUser?.username ?: "Anonymous"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Rating: ${feedback.rating}/5",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            val comment = feedback.comment
            if (comment != null && comment.isNotEmpty()) {
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}