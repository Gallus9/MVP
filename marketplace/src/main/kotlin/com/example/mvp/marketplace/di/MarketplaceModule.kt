package com.example.mvp.marketplace.di

import com.example.mvp.marketplace.data.repository.ProductRepositoryImpl
import com.example.mvp.marketplace.domain.repository.ProductRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MarketplaceModule {

    @Provides
    @Singleton
    fun provideProductRepository(): ProductRepository {
        return ProductRepositoryImpl()
    }
}