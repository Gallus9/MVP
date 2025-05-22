package com.example.mvp.auth.ui

import app.cash.turbine.test
import com.example.mvp.auth.data.AuthRepository
import com.example.mvp.core.base.Resource
import com.example.mvp.data.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @Mock
    private lateinit var authRepository: AuthRepository

    @Mock
    private lateinit var mockUser: User

    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when initialized, getCurrentUser is called`() = runTest {
        // Given
        `when`(authRepository.getCurrentUser()).thenReturn(Resource.Error("No user logged in"))

        // When - init called in constructor
        val viewModel = AuthViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(authRepository).getCurrentUser()
    }

    @Test
    fun `when login is successful, state is updated and success effect is emitted`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password"
        `when`(authRepository.login(email, password)).thenReturn(Resource.Success(mockUser))

        // When
        viewModel.setEvent(AuthEvent.Login(email, password))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(mockUser, viewModel.uiState.value.currentUser)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.effect.test {
            assertEquals(AuthEffect.LoginSuccess, awaitItem())
        }
    }

    @Test
    fun `when login fails, error effect is emitted`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password"
        val errorMessage = "Invalid credentials"
        `when`(authRepository.login(email, password)).thenReturn(Resource.Error(errorMessage))

        // When
        viewModel.setEvent(AuthEvent.Login(email, password))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.effect.test {
            val effect = awaitItem()
            assertTrue(effect is AuthEffect.ShowError)
            assertEquals(errorMessage, (effect as AuthEffect.ShowError).message)
        }
    }

    @Test
    fun `when signup is successful, state is updated and success effect is emitted`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password"
        val role = "GeneralUser"
        `when`(authRepository.register(email, password, role)).thenReturn(Resource.Success(mockUser))

        // When
        viewModel.setEvent(AuthEvent.Signup(email, password, role))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(mockUser, viewModel.uiState.value.currentUser)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.effect.test {
            assertEquals(AuthEffect.SignupSuccess, awaitItem())
        }
    }

    @Test
    fun `when logout is successful, state is updated and success effect is emitted`() = runTest {
        // Given
        `when`(authRepository.logout()).thenReturn(Resource.Success(Unit))

        // Set initial state with a user
        val initialState = AuthState(currentUser = mockUser)
        viewModel = AuthViewModel(authRepository, initialState)

        // When
        viewModel.setEvent(AuthEvent.Logout)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.currentUser)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.effect.test {
            assertEquals(AuthEffect.LogoutSuccess, awaitItem())
        }
    }
}