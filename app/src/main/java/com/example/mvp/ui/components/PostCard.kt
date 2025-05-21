package com.example.mvp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mvp.data.models.Post
import com.example.mvp.data.models.User
import com.parse.ParseFile
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Add

@Composable
fun PostCard(
    post: Post,
    onPostClick: (Post) -> Unit = {},
    onLikeClick: (Post) -> Unit = {},
    onCommentClick: (Post) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = { onPostClick(post) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Author row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Author image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(post.author?.profileImage?.file?.url ?: "")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Author image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Author info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    post.author?.let { author ->
                        Text(
                            text = author.username ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    post.createdAt?.let { createdAt ->
                        val dateFormat = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
                        Text(
                            text = dateFormat.format(createdAt),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                IconButton(onClick = { /* TODO: Show options menu */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Post options"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Title (if any)
            post.title?.let { title ->
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Content
            post.content?.let { content ->
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onLikeClick(post) }) {
                    val isLiked = post.likesCount % 2 != 0
                    
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like post",
                        tint = if (isLiked) MaterialTheme.colorScheme.error else LocalContentColor.current
                    )
                }
                
                Text(
                    text = "${post.likesCount}",
                    style = MaterialTheme.typography.labelMedium
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                IconButton(onClick = { onCommentClick(post) }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Comment on post"
                    )
                }
                
                Text(
                    text = "${post.commentsCount}",
                    style = MaterialTheme.typography.labelMedium
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Tags
                post.tags.takeIf { it.isNotEmpty() }?.let { tags ->
                    AssistChip(
                        onClick = { /* TODO: Add tag filtering */ },
                        label = { Text(tags.first()) }
                    )
                    
                    if (tags.size > 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+${tags.size - 1}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PostCardPreview() {
    // Create a dummy post for preview
    val dummyPost = Post().apply {
        title = "Sample Post Title"
        content = "This is a sample post content with some more text to demonstrate how the post card looks with longer content."
        likesCount = 42
        commentsCount = 7
        tags = listOf("Rooster", "Breeding", "Tips")
        // Note: createdAt and objectId are set by Parse but we don't need them for the preview
    }
    
    MaterialTheme {
        PostCard(post = dummyPost)
    }
}