package com.example.mvp.marketplace.data.repository

import com.example.mvp.core.base.Resource
import com.example.mvp.data.models.Media
import com.example.mvp.data.models.ProductListing
import com.example.mvp.marketplace.data.mapper.ProductMapper
import com.example.mvp.marketplace.domain.model.Product
import com.example.mvp.marketplace.domain.repository.ProductRepository
import com.parse.ParseFile
import com.parse.ParseQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ProductRepositoryImpl @Inject constructor() : ProductRepository {
    
    override suspend fun getProducts(
        category: String?,
        query: String?,
        limit: Int,
        skip: Int
    ): Flow<Resource<List<Product>>> = flow {
        emit(Resource.Loading)
        
        try {
            val queryBuilder = ParseQuery.getQuery(ProductListing::class.java)
                .include(ProductListing.KEY_SELLER)
                .include(ProductListing.KEY_IMAGES)
                .setLimit(limit)
                .setSkip(skip)
                .orderByDescending("createdAt")
            
            // Add category filter if provided
            if (!category.isNullOrBlank()) {
                queryBuilder.whereEqualTo("category", category)
            }
            
            // Add search query if provided
            if (!query.isNullOrBlank()) {
                queryBuilder.whereContains("title", query)
            }
            
            val products = withContext(Dispatchers.IO) {
                queryBuilder.find()
            }
            
            val domainProducts = products.map { ProductMapper.fromEntity(it) }
            emit(Resource.Success(domainProducts))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to fetch products: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun getProductById(productId: String): Flow<Resource<Product>> = flow {
        emit(Resource.Loading)
        
        try {
            val product = withContext(Dispatchers.IO) {
                val query = ParseQuery.getQuery(ProductListing::class.java)
                    .whereEqualTo("objectId", productId)
                    .include(ProductListing.KEY_SELLER)
                    .include(ProductListing.KEY_IMAGES)
                    
                query.first()
            }
            
            if (product != null) {
                emit(Resource.Success(ProductMapper.fromEntity(product)))
            } else {
                emit(Resource.Error("Product not found"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Failed to fetch product: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun createProduct(
        product: Product, 
        imageData: List<ByteArray>?
    ): Flow<Resource<Product>> = flow {
        emit(Resource.Loading)
        
        try {
            // Convert to entity
            val entity = ProductMapper.toEntity(product)
            
            // Upload images if provided
            if (!imageData.isNullOrEmpty()) {
                val mediaList = uploadImages(entity, imageData)
                entity.images = mediaList
            }
            
            // Save the product
            withContext(Dispatchers.IO) {
                entity.save()
            }
            
            emit(Resource.Success(ProductMapper.fromEntity(entity)))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to create product: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun updateProduct(
        product: Product, 
        imageData: List<ByteArray>?
    ): Flow<Resource<Product>> = flow {
        emit(Resource.Loading)
        
        try {
            // Get the existing product
            val existingProduct = withContext(Dispatchers.IO) {
                val query = ParseQuery.getQuery(ProductListing::class.java)
                    .whereEqualTo("objectId", product.id)
                
                query.first()
            } ?: throw Exception("Product not found")
            
            // Update fields
            existingProduct.title = product.name
            existingProduct.description = product.description
            existingProduct.price = product.price
            existingProduct.quantity = product.quantity
            existingProduct.category = product.category
            existingProduct.tags = product.tags
            existingProduct.isOrganic = product.isOrganic
            existingProduct.location = product.location
            
            // Upload new images if provided
            if (!imageData.isNullOrEmpty()) {
                val mediaList = uploadImages(existingProduct, imageData)
                existingProduct.images = mediaList
            }
            
            // Save the updated product
            withContext(Dispatchers.IO) {
                existingProduct.save()
            }
            
            emit(Resource.Success(ProductMapper.fromEntity(existingProduct)))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to update product: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun deleteProduct(productId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        
        try {
            val result = withContext(Dispatchers.IO) {
                suspendCancellableCoroutine<Boolean> { continuation ->
                    val query = ParseQuery.getQuery(ProductListing::class.java)
                    query.getInBackground(productId) { product, e ->
                        if (e != null) {
                            continuation.resumeWithException(e)
                            return@getInBackground
                        }
                        
                        product.deleteInBackground { e2 ->
                            if (e2 != null) {
                                continuation.resumeWithException(e2)
                            } else {
                                continuation.resume(true)
                            }
                        }
                    }
                }
            }
            
            emit(Resource.Success(result))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to delete product: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun getSellerProducts(
        sellerId: String, 
        limit: Int, 
        skip: Int
    ): Flow<Resource<List<Product>>> = flow {
        emit(Resource.Loading)
        
        try {
            val products = withContext(Dispatchers.IO) {
                val query = ParseQuery.getQuery(ProductListing::class.java)
                    .whereEqualTo("seller.objectId", sellerId)
                    .include(ProductListing.KEY_SELLER)
                    .include(ProductListing.KEY_IMAGES)
                    .setLimit(limit)
                    .setSkip(skip)
                    .orderByDescending("createdAt")
                
                query.find()
            }
            
            val domainProducts = products.map { ProductMapper.fromEntity(it) }
            emit(Resource.Success(domainProducts))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to fetch seller products: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Upload images and create Media objects
     */
    private suspend fun uploadImages(product: ProductListing, imageDataList: List<ByteArray>): List<Media> {
        return withContext(Dispatchers.IO) {
            imageDataList.mapIndexed { index, imageData ->
                // Create ParseFile
                val fileName = "product_${product.objectId ?: System.currentTimeMillis()}_${index}.jpg"
                val parseFile = ParseFile(fileName, imageData)
                parseFile.save()
                
                // Create Media object
                val media = Media()
                media.file = parseFile
                media.mediaType = Media.TYPE_IMAGE
                media.owner = product.seller
                media.save()
                
                media
            }
        }
    }
}