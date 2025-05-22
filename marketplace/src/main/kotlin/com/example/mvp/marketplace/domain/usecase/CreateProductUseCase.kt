package com.example.mvp.marketplace.domain.usecase

import com.example.mvp.core.base.Resource
import com.example.mvp.marketplace.domain.model.Product
import com.example.mvp.marketplace.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for creating a new product
 */
class CreateProductUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    /**
     * Execute the use case to create a new product
     */
    suspend operator fun invoke(product: Product, imageData: List<ByteArray>? = null): Flow<Resource<Product>> {
        return repository.createProduct(product, imageData)
    }
}