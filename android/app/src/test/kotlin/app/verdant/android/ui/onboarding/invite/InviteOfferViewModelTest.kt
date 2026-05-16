package app.verdant.android.ui.onboarding.invite

import app.verdant.android.data.model.OrgInviteResponse
import app.verdant.android.data.repository.InviteOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InviteOfferViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `load fetches invites into state`() = runTest {
        val invites = listOf(invite(1, "Trädgården"), invite(2, "Skogen"))
        val vm = InviteOfferViewModel(FakeInviteOps(invites))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(invites, state.invites)
    }

    @Test
    fun `accept flips joined to true`() = runTest {
        val vm = InviteOfferViewModel(FakeInviteOps(listOf(invite(1, "T"))))
        advanceUntilIdle()
        vm.accept(1L)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.joined)
    }

    @Test
    fun `decline drops the invite and sets allDeclined when empty`() = runTest {
        val vm = InviteOfferViewModel(FakeInviteOps(listOf(invite(1, "T"))))
        advanceUntilIdle()
        vm.decline(1L)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.invites.isEmpty())
        assertTrue(state.allDeclined)
    }

    private fun invite(id: Long, orgName: String) = OrgInviteResponse(
        id = id, orgId = id, orgName = orgName,
        email = "me@example.com", invitedByName = "Owner",
        status = "PENDING", createdAt = "2026-05-01T00:00:00Z",
    )
}

private class FakeInviteOps(
    private val initialInvites: List<OrgInviteResponse>,
) : InviteOps {
    val accepted = mutableListOf<Long>()
    val declined = mutableListOf<Long>()
    override suspend fun listPendingInvites() = initialInvites
    override suspend fun acceptInvite(inviteId: Long) { accepted += inviteId }
    override suspend fun declineInvite(inviteId: Long) { declined += inviteId }
}
