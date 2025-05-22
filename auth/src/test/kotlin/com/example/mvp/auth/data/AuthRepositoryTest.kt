package com.example.mvp.auth.data

import com.example.mvp.core.base.Resource
import com.example.mvp.data.models.User
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.parse.ParseUser
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.TaskImpl
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRepositoryTest {

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    @Mock
    private lateinit var authResult: AuthResult

    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Set up the repository with mocked FirebaseAuth
        authRepository = AuthRepositoryWithMockedFirebase(firebaseAuth)
    }

    @Test
    fun `getCurrentUser returns success when user is logged in`() = runBlocking {
        // Given
        val mockUser = mock(User::class.java)
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        mockParseUserCurrent(mockUser)

        // When
        val result = authRepository.getCurrentUser()

        // Then
        assertTrue(result is Resource.Success)
        assertEquals(mockUser, (result as Resource.Success).data)
    }

    @Test
    fun `getCurrentUser returns error when Firebase user exists but Parse user doesn't`() = runBlocking {
        // Given
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        mockParseUserCurrent(null)

        // When
        val result = authRepository.getCurrentUser()

        // Then
        assertTrue(result is Resource.Error)
        assertEquals("User session inconsistent", (result as Resource.Error).message)
        verify(firebaseAuth).signOut()
    }

    @Test
    fun `getCurrentUser returns error when no user is logged in`() = runBlocking {
        // Given
        whenever(firebaseAuth.currentUser).thenReturn(null)

        // When
        val result = authRepository.getCurrentUser()

        // Then
        assertTrue(result is Resource.Error)
        assertEquals("No user logged in", (result as Resource.Error).message)
    }

    @Test
    fun `login returns success when both Firebase and Parse login succeed`() = runBlocking {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val mockUser = mock(User::class.java)
        val task = TaskImpl<AuthResult>()
        task.setResult(authResult)
        
        whenever(firebaseAuth.signInWithEmailAndPassword(email, password)).thenReturn(task)
        whenever(authResult.user).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn("test-uid")
        mockParseUserLogin(mockUser)

        // When
        val result = authRepository.login(email, password)

        // Then
        assertTrue(result is Resource.Success)
        assertEquals(mockUser, (result as Resource.Success).data)
    }

    @Test
    fun `logout returns success when logout succeeds`() = runBlocking {
        // When
        val result = authRepository.logout()

        // Then
        assertTrue(result is Resource.Success)
        verify(firebaseAuth).signOut()
    }

    // Helper method to mock ParseUser.getCurrentUser()
    private fun mockParseUserCurrent(user: User?) {
        val staticMock = mockStatic(ParseUser::class.java)
        staticMock.`when`<ParseUser> { ParseUser.getCurrentUser() }.thenReturn(user)
    }

    // Helper method to mock ParseUser.logIn()
    private fun mockParseUserLogin(user: User?) {
        val staticMock = mockStatic(ParseUser::class.java)
        staticMock.`when`<ParseUser> { ParseUser.logIn(any(), any()) }.thenReturn(user)
    }
    
    // Class extension to inject mocked FirebaseAuth
    private class AuthRepositoryWithMockedFirebase(private val mockedFirebaseAuth: FirebaseAuth) : AuthRepository() {
        override fun getFirebaseAuth(): FirebaseAuth {
            return mockedFirebaseAuth
        }
    }
}