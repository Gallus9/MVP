package com.example.mvp.ui.screens.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mvp.data.models.Post
import com.example.mvp.ui.components.PostCard
import com.example.mvp.ui.viewmodels.CommunityUiState
import com.example.mvp.ui.viewmodels.CommunityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedScreen(
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToPostDetails: (Post) -> Unit = {},
    onBackClick: () -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    
    // Load more posts when scrolling to the end
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 3
            }
        }
    }
    
    LaunchedEffect(loadMore.value) {
        if (loadMore.value && uiState is CommunityUiState.Success) {
            viewModel.loadMorePosts()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community Feed") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreatePost) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new post"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is CommunityUiState.Loading -> {
                    // Show loading indicator
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                is CommunityUiState.Success -> {
                    val posts = (uiState as CommunityUiState.Success).posts
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = posts,
                            key = { post -> post.objectId }
                        ) { post ->
                            PostCard(
                                post = post,
                                onPostClick = onNavigateToPostDetails,
                                onLikeClick = { viewModel.togglePostLike(it) },
                                onCommentClick = onNavigateToPostDetails
                            )
                        }
                        
                        // Footer loading indicator
                        if (uiState is CommunityUiState.LoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
                
                is CommunityUiState.LoadingMore -> {
                    val posts = (uiState as CommunityUiState.LoadingMore).posts
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = posts,
                            key = { post -> post.objectId }
                        ) { post ->
                            PostCard(
                                post = post,
                                onPostClick = onNavigateToPostDetails,
                                onLikeClick = { viewModel.togglePostLike(it) },
                                onCommentClick = onNavigateToPostDetails
                            )
                        }
                        
                        // Footer loading indicator
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
                
                is CommunityUiState.Empty -> {
                    // Show empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No posts yet",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Be the first to share with the community!",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onNavigateToCreatePost) {
                                Text("Create Post")
                            }
                        }
                    }
                }
                
                is CommunityUiState.Error -> {
                    // Show error state
                    val errorMessage = (uiState as CommunityUiState.Error).message
                    
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Oops! Something went wrong.",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadFeed() }) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }
        }
    }
}