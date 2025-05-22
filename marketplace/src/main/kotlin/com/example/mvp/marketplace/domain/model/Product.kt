package com.example.mvp.marketplace.domain.model

import com.example.mvp.data.models.User
import java.util.Date

/**
 * Domain model for a Product in the marketplace module
 */
data class Product(
    val id: String = "",
    val name: String,
    val description: String,
    val price: Double,
    val quantity: Int,
    val category: String,
    val sellerId: String,
    val seller: User? = null,
    val images: List<String> = emptyList(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val tags: List<String> = emptyList(),
    val isOrganic: Boolean = false,
    val location: String? = null
)