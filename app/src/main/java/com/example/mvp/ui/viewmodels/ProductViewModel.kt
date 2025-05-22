package com.example.mvp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.mvp.core.base.BaseViewModel
import com.example.mvp.core.base.Resource
import com.example.mvp.data.models.Media
import com.example.mvp.data.models.ProductListing
import com.example.mvp.data.models.User
import com.parse.ParseFile
import com.parse.ParseQuery
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class ProductEvent : com.example.mvp.core.base.UiEvent {
    data class FetchProducts(val limit: Int = 20) : ProductEvent()
    data class FetchProductsBySeller(val seller: User, val limit: Int = 20) : ProductEvent()
    data class FetchProductDetail(val productId: String) : ProductEvent()
    data class CreateProduct(
        val title: String,
        val description: String,
        val price: Double,
        val isTraceable: Boolean,
        val traceId: String?,
        val imageFiles: List<ByteArray>,
        val mimeTypes: List<String>,
        val currentUser: User
    ) : ProductEvent()
}

data class ProductState(
    val productList: Resource<List<ProductListing>> = Resource.Success(emptyList()),
    val productDetail: Resource<Pair<ProductListing, List<Media>>>? = null
) : com.example.mvp.core.base.UiState

sealed class ProductEffect : com.example.mvp.core.base.UiEffect {
    object ProductCreated : ProductEffect()
    data class ShowError(val message: String) : ProductEffect()
}

class ProductViewModel : BaseViewModel<ProductEvent, ProductState, ProductEffect>() {
    
    init {
        // Initialize with idle state
        setState { ProductState() }
    }
    
    // Fetch all product listings
    private fun fetchProducts(limit: Int = 20) {
        viewModelScope.launch {
            try {
                setState { copy(productList = Resource.Loading) }
                val products = getProducts(limit)
                setState { copy(productList = Resource.Success(products)) }
            } catch (e: Exception) {
                setState { copy(productList = Resource.Error(e.message ?: "Failed to fetch products")) }
                setEffect { ProductEffect.ShowError(e.message ?: "Failed to fetch products") }
            }
        }
    }
    
    // Fetch products by seller
    private fun fetchProductsBySeller(seller: User, limit: Int = 20) {
        viewModelScope.launch {
            try {
                setState { copy(productList = Resource.Loading) }
                val products = getProductsBySeller(seller, limit)
                setState { copy(productList = Resource.Success(products)) }
            } catch (e: Exception) {
                setState { copy(productList = Resource.Error(e.message ?: "Failed to fetch products")) }
                setEffect { ProductEffect.ShowError(e.message ?: "Failed to fetch products") }
            }
        }
    }
    
    // Fetch a single product by ID along with its images
    private fun fetchProductDetail(productId: String) {
        viewModelScope.launch {
            try {
                setState { copy(productDetail = Resource.Loading) }
                val product = getProductById(productId)
                if (product != null) {
                    val images = getProductImages(product)
                    setState { copy(productDetail = Resource.Success(Pair(product, images))) }
                } else {
                    setState { copy(productDetail = Resource.Error("Product not found")) }
                    setEffect { ProductEffect.ShowError("Product not found") }
                }
            } catch (e: Exception) {
                setState { copy(productDetail = Resource.Error(e.message ?: "Failed to fetch product details")) }
                setEffect { ProductEffect.ShowError(e.message ?: "Failed to fetch product details") }
            }
        }
    }
    
    // Create a new product listing
    private fun createProduct(
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
                setState { copy(productList = Resource.Loading) }
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
                val products = getProducts(20)
                setState { copy(productList = Resource.Success(products)) }
                setEffect { ProductEffect.ProductCreated }
            } catch (e: Exception) {
                setState { copy(productList = Resource.Error(e.message ?: "Failed to create product")) }
                setEffect { ProductEffect.ShowError(e.message ?: "Failed to create product") }
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
    
    override fun createInitialState(): ProductState = ProductState()
    
    override fun handleEvent(event: ProductEvent) {
        when (event) {
            is ProductEvent.FetchProducts -> fetchProducts(event.limit)
            is ProductEvent.FetchProductsBySeller -> fetchProductsBySeller(event.seller, event.limit)
            is ProductEvent.FetchProductDetail -> fetchProductDetail(event.productId)
            is ProductEvent.CreateProduct -> createProduct(
                event.title,
                event.description,
                event.price,
                event.isTraceable,
                event.traceId,
                event.imageFiles,
                event.mimeTypes,
                event.currentUser
            )
        }
    }
}