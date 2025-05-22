package com.example.mvp.marketplace.ui

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.mvp.core.base.BaseViewModel
import com.example.mvp.core.base.Resource
import com.example.mvp.core.base.UiEffect
import com.example.mvp.core.base.UiEvent
import com.example.mvp.core.base.UiState
import com.example.mvp.marketplace.domain.model.Product
import com.example.mvp.marketplace.domain.usecase.CreateProductUseCase
import com.example.mvp.marketplace.domain.usecase.GetProductsUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

class MarketplaceViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val createProductUseCase: CreateProductUseCase
) : BaseViewModel<MarketplaceEvent, MarketplaceState, MarketplaceEffect>() {
    
    companion object {
        private const val TAG = "MarketplaceViewModel"
    }
    
    init {
        loadProducts()
    }
    
    private fun loadProducts(
        category: String? = null,
        query: String? = null
    ) {
        viewModelScope.launch {
            getProductsUseCase(category, query)
                .onStart { 
                    setState { copy(isLoading = true) }
                }
                .catch { e ->
                    Log.e(TAG, "Error loading products: ${e.message ?: "Unknown error"}", e)
                    setState { copy(isLoading = false) }
                    setEffect { MarketplaceEffect.ShowError(e.message ?: "Failed to load products") }
                }
                .collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            setState { 
                                copy(
                                    products = resource.data,
                                    filteredCategory = category,
                                    searchQuery = query,
                                    isLoading = false
                                ) 
                            }
                        }
                        is Resource.Error -> {
                            setState { copy(isLoading = false) }
                            setEffect { MarketplaceEffect.ShowError(resource.message) }
                        }
                        is Resource.Loading -> {
                            setState { copy(isLoading = true) }
                        }
                    }
                }
        }
    }
    
    private fun createProduct(product: Product, images: List<ByteArray>?) {
        viewModelScope.launch {
            createProductUseCase(product, images)
                .onStart { 
                    setState { copy(isCreatingProduct = true) }
                }
                .catch { e ->
                    Log.e(TAG, "Error creating product: ${e.message ?: "Unknown error"}", e)
                    setState { copy(isCreatingProduct = false) }
                    setEffect { MarketplaceEffect.ShowError(e.message ?: "Failed to create product") }
                }
                .collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            setState { 
                                copy(
                                    products = products + resource.data,
                                    isCreatingProduct = false
                                ) 
                            }
                            setEffect { MarketplaceEffect.ProductCreated(resource.data) }
                        }
                        is Resource.Error -> {
                            setState { copy(isCreatingProduct = false) }
                            setEffect { MarketplaceEffect.ShowError(resource.message) }
                        }
                        is Resource.Loading -> {
                            setState { copy(isCreatingProduct = true) }
                        }
                    }
                }
        }
    }
    
    override fun createInitialState(): MarketplaceState = MarketplaceState()
    
    override fun handleEvent(event: MarketplaceEvent) {
        when (event) {
            is MarketplaceEvent.LoadProducts -> loadProducts(event.category, event.query)
            is MarketplaceEvent.CreateProduct -> createProduct(event.product, event.images)
        }
    }
}

sealed class MarketplaceEvent : UiEvent {
    data class LoadProducts(val category: String? = null, val query: String? = null) : MarketplaceEvent()
    data class CreateProduct(val product: Product, val images: List<ByteArray>? = null) : MarketplaceEvent()
}

data class MarketplaceState(
    val products: List<Product> = emptyList(),
    val filteredCategory: String? = null,
    val searchQuery: String? = null,
    val isLoading: Boolean = false,
    val isCreatingProduct: Boolean = false
) : UiState

sealed class MarketplaceEffect : UiEffect {
    data class ShowError(val message: String) : MarketplaceEffect()
    data class ProductCreated(val product: Product) : MarketplaceEffect()
}