package com.example.mvp.data.repositories

import android.util.Log
import com.example.mvp.data.models.User
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.parse.ParseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository {
    private val TAG = "ChatRepository"
    private val database = FirebaseDatabase.getInstance()
    private val messagesRef = database.getReference("messages")
    private val conversationsRef = database.getReference("conversations")

    /**
     * Data class for chat messages
     */
    data class Message(
        var id: String = UUID.randomUUID().toString(),
        val senderId: String = "",
        val receiverId: String = "",
        val text: String = "",
        val timestamp: Any = ServerValue.TIMESTAMP,
        var seen: Boolean = false
    ) {
        // Empty constructor for Firebase
        constructor() : this(
            id = UUID.randomUUID().toString(),
            senderId = "",
            receiverId = "",
            text = "",
            timestamp = 0L,
            seen = false
        )

        // Convert from DataSnapshot
        companion object {
            fun fromSnapshot(snapshot: DataSnapshot): Message {
                val message = snapshot.getValue(Message::class.java)!!
                message.id = snapshot.key ?: UUID.randomUUID().toString()
                return message
            }
        }
    }

    /**
     * Generate or retrieve a conversation ID between two users
     */
    private fun getConversationId(userId1: String, userId2: String): String {
        // Ensure consistent ID regardless of order
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }

    /**
     * Send a message in a conversation
     */
    suspend fun sendMessage(recipientId: String, messageText: String): Result<String> {
        val currentUser = ParseUser.getCurrentUser() as? User ?: 
            return Result.failure(Exception("No authenticated user"))
        
        val currentUserId = currentUser.objectId
        val conversationId = getConversationId(currentUserId, recipientId)
        
        return try {
            val message = Message(
                senderId = currentUserId,
                receiverId = recipientId,
                text = messageText
            )
            
            // Update the message in the messages collection
            val messageRef = messagesRef.child(conversationId).push()
            messageRef.setValue(message).await()
            
            // Update conversation metadata
            val conversationUpdate = mapOf(
                "lastMessage" to messageText,
                "lastMessageTime" to ServerValue.TIMESTAMP,
                "members" to listOf(currentUserId, recipientId)
            )
            
            conversationsRef.child(conversationId).updateChildren(conversationUpdate).await()
            
            Result.success(messageRef.key ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Listen for messages in a conversation as a Flow
     */
    fun getMessages(recipientId: String): Flow<List<Message>> = callbackFlow {
        val currentUser = ParseUser.getCurrentUser() as? User
        if (currentUser == null) {
            close(Exception("No authenticated user"))
            return@callbackFlow
        }

        val conversationId = getConversationId(currentUser.objectId, recipientId)
        val messagesListener = messagesRef.child(conversationId)
            .orderByChild("timestamp")
            .addChildEventListener(object : ChildEventListener {
                val messagesList = mutableListOf<Message>()

                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val message = Message.fromSnapshot(snapshot)
                    messagesList.add(message)
                    trySend(messagesList.toList())
                    
                    // Mark messages from other user as seen
                    if (message.senderId != currentUser.objectId && !message.seen) {
                        snapshot.ref.child("seen").setValue(true)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val updatedMessage = Message.fromSnapshot(snapshot)
                    val index = messagesList.indexOfFirst { it.id == updatedMessage.id }
                    if (index >= 0) {
                        messagesList[index] = updatedMessage
                        trySend(messagesList.toList())
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val messageId = snapshot.key
                    messagesList.removeIf { it.id == messageId }
                    trySend(messagesList.toList())
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // Not implemented for basic chat
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            })

        awaitClose {
            messagesRef.child(conversationId).removeEventListener(messagesListener)
        }
    }

    /**
     * Get all conversations for the current user
     */
    fun getConversations(): Flow<List<Map<String, Any>>> = callbackFlow {
        val currentUser = ParseUser.getCurrentUser() as? User
        if (currentUser == null) {
            close(Exception("No authenticated user"))
            return@callbackFlow
        }

        val listener = conversationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversations = mutableListOf<Map<String, Any>>()
                
                for (conversationSnapshot in snapshot.children) {
                    // Check if the conversation has members
                    val membersSnapshot = conversationSnapshot.child("members")
                    
                    // Check if current user is a member
                    var containsCurrentUser = false
                    var otherUserId: String? = null
                    
                    // Iterate through members to find current user and the other user
                    for (memberSnapshot in membersSnapshot.children) {
                        val memberId = memberSnapshot.getValue(String::class.java)
                        if (memberId == currentUser.objectId) {
                            containsCurrentUser = true
                        } else {
                            otherUserId = memberId
                        }
                    }
                    
                    if (containsCurrentUser && otherUserId != null) {
                        val lastMessage = conversationSnapshot.child("lastMessage").getValue(String::class.java) ?: ""
                        val lastMessageTime = conversationSnapshot.child("lastMessageTime").getValue(Long::class.java) ?: 0L
                        
                        conversations.add(mapOf(
                            "conversationId" to (conversationSnapshot.key ?: ""),
                            "otherUserId" to otherUserId,
                            "lastMessage" to lastMessage,
                            "lastMessageTime" to lastMessageTime
                        ))
                    }
                }
                
                trySend(conversations)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            conversationsRef.removeEventListener(listener)
        }
    }

    /**
     * Delete a message
     */
    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        return try {
            messagesRef.child(conversationId).child(messageId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message: ${e.message}", e)
            Result.failure(e)
        }
    }
}