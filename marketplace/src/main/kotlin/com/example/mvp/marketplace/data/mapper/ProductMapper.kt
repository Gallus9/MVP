package com.example.mvp.marketplace.data.mapper

import com.example.mvp.data.models.Media
import com.example.mvp.data.models.ProductListing
import com.example.mvp.marketplace.domain.model.Product

/**
 * Mapper class to convert between domain Product model and Parse ProductListing data model
 */
object ProductMapper {
    
    /**
     * Map from Parse ProductListing to domain Product
     */
    fun fromEntity(entity: ProductListing): Product {
        val images = entity.images?.map { media ->
            (media as? Media)?.file?.url ?: ""
        }?.filter { it.isNotEmpty() } ?: emptyList()
        
        return Product(
            id = entity.objectId ?: "",
            name = entity.title ?: "",
            description = entity.description ?: "",
            price = entity.price ?: 0.0,
            quantity = entity.quantity ?: 0,
            category = entity.category ?: "",
            sellerId = entity.seller?.objectId ?: "",
            seller = entity.seller,
            images = images,
            createdAt = entity.createdAt ?: java.util.Date(),
            updatedAt = entity.updatedAt ?: java.util.Date(),
            tags = entity.tags ?: emptyList(),
            isOrganic = entity.isOrganic ?: false,
            location = entity.location
        )
    }
    
    /**
     * Map from domain Product to Parse ProductListing
     */
    fun toEntity(product: Product): ProductListing {
        val entity = ProductListing()
        entity.objectId = if (product.id.isEmpty()) null else product.id
        entity.title = product.name
        entity.description = product.description
        entity.price = product.price
        entity.quantity = product.quantity
        entity.category = product.category
        entity.seller = product.seller
        entity.tags = product.tags
        entity.isOrganic = product.isOrganic
        entity.location = product.location
        // Images are handled separately in the repository when uploading
        
        return entity
    }
}