package com.example.mvp.ui.screens.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mvp.ui.viewmodels.CommunityViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onBackClick: () -> Unit = {},
    onPostCreated: () -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var isCreatingPost by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Post") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("What's on your mind?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                minLines = 5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. rooster, breeding, tips") }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Add tags to help others discover your post.",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Media attachment (placeholder for now)
            OutlinedButton(
                onClick = { /* TODO: Implement media picker */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add, 
                    contentDescription = "Attach media"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Photos")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Button(
                    onClick = {
                        if (isContentValid(content)) {
                            isCreatingPost = true
                            
                            // Process tags - split by comma and trim spaces
                            val tagsList = tags.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            
                            viewModel.createPost(
                                title = title.takeIf { it.isNotEmpty() },
                                content = content,
                                tags = tagsList
                            )
                            
                            // Simulate post creation delay
                            coroutineScope.launch {
                                delay(1000)
                                isCreatingPost = false
                                onPostCreated()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isContentValid(content) && !isCreatingPost
                ) {
                    if (isCreatingPost) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Post"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Post")
                }
            }
        }
    }
}

private fun isContentValid(content: String): Boolean {
    return content.isNotBlank() && content.length >= 10
}