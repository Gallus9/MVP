package com.example.mvp.marketplace.domain.repository

import com.example.mvp.core.base.Resource
import com.example.mvp.marketplace.domain.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for product-related operations
 */
interface ProductRepository {
    
    /**
     * Get all products with optional filters
     * @param category Optional category filter
     * @param query Optional search query
     * @param limit Maximum number of results to return
     * @param skip Number of results to skip (for pagination)
     */
    suspend fun getProducts(
        category: String? = null,
        query: String? = null,
        limit: Int = 20,
        skip: Int = 0
    ): Flow<Resource<List<Product>>>
    
    /**
     * Get a single product by ID
     * @param productId The ID of the product to retrieve
     */
    suspend fun getProductById(productId: String): Flow<Resource<Product>>
    
    /**
     * Create a new product listing
     * @param product The product to create
     * @param imageData List of image data to upload
     */
    suspend fun createProduct(
        product: Product,
        imageData: List<ByteArray>? = null
    ): Flow<Resource<Product>>
    
    /**
     * Update an existing product
     * @param product The updated product data
     * @param imageData Optional new images to upload
     */
    suspend fun updateProduct(
        product: Product,
        imageData: List<ByteArray>? = null
    ): Flow<Resource<Product>>
    
    /**
     * Delete a product by ID
     * @param productId The ID of the product to delete
     */
    suspend fun deleteProduct(productId: String): Flow<Resource<Boolean>>
    
    /**
     * Get products created by a specific seller
     * @param sellerId The ID of the seller
     * @param limit Maximum number of results to return
     * @param skip Number of results to skip (for pagination)
     */
    suspend fun getSellerProducts(
        sellerId: String,
        limit: Int = 20,
        skip: Int = 0
    ): Flow<Resource<List<Product>>>
}