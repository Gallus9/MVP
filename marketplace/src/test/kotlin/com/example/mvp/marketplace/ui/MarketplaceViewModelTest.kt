package com.example.mvp.marketplace.ui

import app.cash.turbine.test
import com.example.mvp.core.base.Resource
import com.example.mvp.marketplace.domain.model.Product
import com.example.mvp.marketplace.domain.usecase.CreateProductUseCase
import com.example.mvp.marketplace.domain.usecase.GetProductsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class MarketplaceViewModelTest {

    @Mock
    private lateinit var getProductsUseCase: GetProductsUseCase
    
    @Mock
    private lateinit var createProductUseCase: CreateProductUseCase
    
    private lateinit var viewModel: MarketplaceViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    // Sample test data
    private val sampleProduct1 = Product(
        id = "1",
        name = "Organic Apples",
        description = "Fresh organic apples from local farms",
        price = 2.99,
        quantity = 100,
        category = "Fruits",
        sellerId = "seller1",
        isOrganic = true
    )
    
    private val sampleProduct2 = Product(
        id = "2",
        name = "Farm Fresh Eggs",
        description = "Free-range eggs from happy chickens",
        price = 4.99,
        quantity = 24,
        category = "Dairy",
        sellerId = "seller2",
        isOrganic = false
    )
    
    private val sampleProducts = listOf(sampleProduct1, sampleProduct2)

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when initialized, products are loaded`() = runTest {
        // Given
        `when`(getProductsUseCase(null, null)).thenReturn(
            flowOf(Resource.Success(sampleProducts))
        )
        
        // When
        viewModel = MarketplaceViewModel(getProductsUseCase, createProductUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(sampleProducts, viewModel.uiState.value.products)
        assertFalse(viewModel.uiState.value.isLoading)
    }
    
    @Test
    fun `when loading products fails, error effect is emitted`() = runTest {
        // Given
        val errorMessage = "Network error"
        `when`(getProductsUseCase(null, null)).thenReturn(
            flowOf(Resource.Error(errorMessage))
        )
        
        // When
        viewModel = MarketplaceViewModel(getProductsUseCase, createProductUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.uiState.value.products.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        
        viewModel.effect.test {
            val effect = awaitItem()
            assertTrue(effect is MarketplaceEffect.ShowError)
            assertEquals(errorMessage, (effect as MarketplaceEffect.ShowError).message)
        }
    }
    
    @Test
    fun `when creating a product succeeds, state is updated and success effect is emitted`() = runTest {
        // Given
        val newProduct = sampleProduct1.copy(id = "3")
        val imageData = listOf(ByteArray(10))
        
        `when`(getProductsUseCase(null, null)).thenReturn(
            flowOf(Resource.Success(emptyList()))
        )
        `when`(createProductUseCase(newProduct, imageData)).thenReturn(
            flowOf(Resource.Success(newProduct))
        )
        
        viewModel = MarketplaceViewModel(getProductsUseCase, createProductUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.setEvent(MarketplaceEvent.CreateProduct(newProduct, imageData))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(listOf(newProduct), viewModel.uiState.value.products)
        assertFalse(viewModel.uiState.value.isCreatingProduct)
        
        viewModel.effect.test {
            val effect = awaitItem()
            assertTrue(effect is MarketplaceEffect.ProductCreated)
            assertEquals(newProduct, (effect as MarketplaceEffect.ProductCreated).product)
        }
    }
}