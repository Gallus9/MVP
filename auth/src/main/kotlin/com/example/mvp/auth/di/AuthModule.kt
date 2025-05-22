package com.example.mvp.auth.di

import com.example.mvp.auth.data.AuthRepository
import com.example.mvp.auth.ui.AuthViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository {
        return AuthRepository()
    }

    @Provides
    @Singleton
    fun provideAuthViewModel(authRepository: AuthRepository): AuthViewModel {
        return AuthViewModel(authRepository)
    }
}