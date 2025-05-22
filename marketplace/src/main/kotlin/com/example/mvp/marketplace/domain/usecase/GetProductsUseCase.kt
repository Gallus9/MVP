package com.example.mvp.marketplace.domain.usecase

import com.example.mvp.core.base.Resource
import com.example.mvp.marketplace.domain.model.Product
import com.example.mvp.marketplace.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for fetching products
 */
class GetProductsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    /**
     * Execute the use case to get products
     */
    suspend operator fun invoke(
        category: String? = null,
        query: String? = null,
        limit: Int = 20,
        skip: Int = 0
    ): Flow<Resource<List<Product>>> {
        return repository.getProducts(category, query, limit, skip)
    }
}