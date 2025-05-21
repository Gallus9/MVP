package com.example.mvp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvp.data.models.Media
import com.example.mvp.data.models.ProductListing
import com.example.mvp.data.models.User
import com.parse.ParseFile
import com.parse.ParseQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume

sealed class ProductListState {
    object Idle : ProductListState()
    object Loading : ProductListState()
    data class Error(val message: String) : ProductListState()
    data class Success(val products: List<ProductListing>) : ProductListState()
}

sealed class ProductDetailState {
    object Idle : ProductDetailState()
    object Loading : ProductDetailState()
    data class Error(val message: String) : ProductDetailState()
    data class Success(val product: ProductListing, val images: List<Media>) : ProductDetailState()
}

class ProductViewModel : ViewModel() {
    
    private val _productListState = MutableStateFlow<ProductListState>(ProductListState.Idle)
    val productListState: StateFlow<ProductListState> = _productListState.asStateFlow()
    
    private val _productDetailState = MutableStateFlow<ProductDetailState>(ProductDetailState.Idle)
    val productDetailState: StateFlow<ProductDetailState> = _productDetailState.asStateFlow()
    
    // Fetch all product listings
    fun fetchProducts(limit: Int = 20) {
        viewModelScope.launch {
            try {
                _productListState.value = ProductListState.Loading
                
                val products = getProducts(limit)
                _productListState.value = ProductListState.Success(products)
            } catch (e: Exception) {
                _productListState.value = ProductListState.Error(e.message ?: "Failed to fetch products")
            }
        }
    }
    
    // Fetch products by seller
    fun fetchProductsBySeller(seller: User, limit: Int = 20) {
        viewModelScope.launch {
            try {
                _productListState.value = ProductListState.Loading
                
                val products = getProductsBySeller(seller, limit)
                _productListState.value = ProductListState.Success(products)
            } catch (e: Exception) {
                _productListState.value = ProductListState.Error(e.message ?: "Failed to fetch products")
            }
        }
    }
    
    // Fetch a single product by ID along with its images
    fun fetchProductDetail(productId: String) {
        viewModelScope.launch {
            try {
                _productDetailState.value = ProductDetailState.Loading
                
                val product = getProductById(productId)
                if (product != null) {
                    val images = getProductImages(product)
                    _productDetailState.value = ProductDetailState.Success(product, images)
                } else {
                    _productDetailState.value = ProductDetailState.Error("Product not found")
                }
            } catch (e: Exception) {
                _productDetailState.value = ProductDetailState.Error(e.message ?: "Failed to fetch product details")
            }
        }
    }
    
    // Create a new product listing
    fun createProduct(
        title: String,
        description: String,
        price: Double,
        isTraceable: Boolean,
        traceId: String?,
        imageFiles: List<ByteArray>,
        mimeTypes: List<String>,
        currentUser: User
    ) {
        viewModelScope.launch {
            try {
                // Create the product
                val product = ProductListing()
                product.title = title
                product.description = description
                product.price = price
                product.isTraceable = isTraceable
                product.traceId = traceId
                product.seller = currentUser
                
                // Save product first
                saveProduct(product)
                
                // Create and save media objects
                if (imageFiles.isNotEmpty()) {
                    for (i in imageFiles.indices) {
                        val media = Media()
                        val parseFile = ParseFile("product_image_${System.currentTimeMillis()}.jpg", imageFiles[i])
                        parseFile.saveInBackground()
                        
                        media.file = parseFile
                        media.owner = currentUser
                        media.listing = product
                        media.mediaType = Media.TYPE_IMAGE
                        media.saveInBackground()
                        
                        // Add to product's image relation
                        product.addImage(media)
                    }
                    product.saveInBackground()
                }
                
                // Refresh product list
                fetchProducts()
            } catch (e: Exception) {
                _productListState.value = ProductListState.Error(e.message ?: "Failed to create product")
            }
        }
    }
    
    // Helper methods to convert callback-based Parse operations to coroutines
    private suspend fun getProducts(limit: Int): List<ProductListing> = suspendCancellableCoroutine { continuation ->
        val query = ProductListing.getQuery()
            .include(ProductListing.KEY_SELLER)
            .setLimit(limit)
            .orderByDescending("createdAt")
        
        query.findInBackground { products, e ->
            if (e == null) {
                continuation.resume(products)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private suspend fun getProductsBySeller(seller: User, limit: Int): List<ProductListing> = suspendCancellableCoroutine { continuation ->
        val query = ProductListing.getQuery()
            .whereEqualTo(ProductListing.KEY_SELLER, seller)
            .include(ProductListing.KEY_SELLER)
            .setLimit(limit)
            .orderByDescending("createdAt")
        
        query.findInBackground { products, e ->
            if (e == null) {
                continuation.resume(products)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private suspend fun getProductById(productId: String): ProductListing? = suspendCancellableCoroutine { continuation ->
        val query = ProductListing.getQuery()
            .whereEqualTo("objectId", productId)
            .include(ProductListing.KEY_SELLER)
        
        query.getFirstInBackground { product, e ->
            if (e == null) {
                continuation.resume(product)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private suspend fun getProductImages(product: ProductListing): List<Media> = suspendCancellableCoroutine { continuation ->
        val query = Media.getQuery()
            .whereEqualTo(Media.KEY_LISTING, product)
            .orderByAscending("createdAt")
        
        query.findInBackground { images, e ->
            if (e == null) {
                continuation.resume(images)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private suspend fun saveProduct(product: ProductListing): Boolean = suspendCancellableCoroutine { continuation ->
        product.saveInBackground { e ->
            if (e == null) {
                continuation.resume(true)
            } else {
                continuation.resumeWithException(e)
            }
        }
    }
}