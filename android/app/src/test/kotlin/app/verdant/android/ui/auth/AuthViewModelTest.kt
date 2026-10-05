package app.verdant.android.ui.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import app.verdant.android.R
import app.verdant.android.data.model.AuthResponse
import app.verdant.android.data.model.UserOrgMembership
import app.verdant.android.data.model.UserResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    private fun response(hasOrg: Boolean = true) = AuthResponse("test-session", UserResponse(
        id = 1, email = "grower@example.test", displayName = "Grower", avatarUrl = null, role = "USER",
        organizations = if (hasOrg) listOf(UserOrgMembership(1, "Farm", null, "OWNER")) else emptyList(),
        createdAt = "2026-10-05T00:00:00Z",
    ))

    @Test fun `button stays busy through Google picker and backend exchange and ignores duplicate taps`() = runTest {
        val google = CompletableDeferred<String>()
        val backend = CompletableDeferred<AuthResponse>()
        var pickerCalls = 0
        var backendCalls = 0
        val vm = AuthViewModel { token ->
            assertEquals("google-token", token)
            backendCalls++
            backend.await()
        }
        vm.signInWithGoogle { pickerCalls++; google.await() }
        assertTrue(vm.uiState.value.isLoading)
        vm.signInWithGoogle { pickerCalls++; "duplicate" }
        runCurrent()
        assertEquals(1, pickerCalls)
        assertEquals(0, backendCalls)
        google.complete("google-token"); runCurrent()
        assertTrue(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.success)
        backend.complete(response()); advanceUntilIdle()
        assertEquals(1, backendCalls)
        assertTrue(vm.uiState.value.success)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `sign-in survives cancellation of the UI caller and retains navigation result`() = runTest {
        val google = CompletableDeferred<String>()
        val vm = AuthViewModel { response() }
        val uiJob = launch { vm.signInWithGoogle { google.await() } }
        runCurrent()
        uiJob.cancel()
        google.complete("google-token"); advanceUntilIdle()
        assertTrue(vm.uiState.value.success)
        assertFalse(vm.uiState.value.needsOrg)
    }

    @Test fun `account without an organization goes to onboarding`() = runTest {
        val vm = AuthViewModel { response(hasOrg = false) }
        vm.signInWithGoogle { "google-token" }; advanceUntilIdle()
        assertTrue(vm.uiState.value.needsOrg)
        assertFalse(vm.uiState.value.success)
    }

    @Test fun `Google cancellation enables retry without authenticating`() = runTest {
        var calls = 0
        val vm = AuthViewModel { calls++; response() }
        vm.signInWithGoogle { throw GetCredentialCancellationException() }; advanceUntilIdle()
        assertEquals(AuthUiState(), vm.uiState.value)
        assertEquals(0, calls)
        vm.signInWithGoogle { "google-token" }; advanceUntilIdle()
        assertTrue(vm.uiState.value.success)
    }

    @Test fun `provider failure is visible and does not signal success`() = runTest {
        val vm = AuthViewModel { error("Must not call backend") }
        vm.signInWithGoogle { throw NoCredentialException() }; advanceUntilIdle()
        assertEquals(R.string.google_sign_in_no_credential, vm.uiState.value.errorResource)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.success)
    }

    @Test fun `Google success alone does not navigate when backend rejects login`() = runTest {
        val vm = AuthViewModel { throw IllegalStateException("Backend rejected sign-in") }
        vm.signInWithGoogle { "google-token" }; advanceUntilIdle()
        assertEquals("Backend rejected sign-in", vm.uiState.value.error)
        assertFalse(vm.uiState.value.success)
        assertFalse(vm.uiState.value.isLoading)
    }
}
