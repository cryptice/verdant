package app.verdant.android.ui.onboarding.join

import app.verdant.android.data.model.OrgInviteResponse
import app.verdant.android.data.model.OrgJoinRequestResponse
import app.verdant.android.data.model.OrgLookupResponse
import app.verdant.android.data.model.OrgMemberResponse
import app.verdant.android.data.model.OrganizationResponse
import app.verdant.android.data.model.UserResponse
import app.verdant.android.data.repository.OrgRepository
import app.verdant.android.data.repository.UserRefresher
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JoinOrgViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `submit with blank name does nothing`() = runTest {
        val repo = FakeOrgRepo()
        val vm = JoinOrgViewModel(repo, FakeUserRefresher())
        vm.setName("   ")
        vm.submit()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isSubmitting)
        assertNull(vm.uiState.value.requestSentOrgName)
    }

    @Test
    fun `submit with not-found sets notFound flag`() = runTest {
        val vm = JoinOrgViewModel(FakeOrgRepo(), FakeUserRefresher())
        vm.setName("Foo")
        vm.submit()
        advanceUntilIdle()
        val s = vm.uiState.value
        assertTrue(s.notFound)
        assertNull(s.requestSentOrgName)
    }

    @Test
    fun `submit with found org calls requestJoin and shows sent state`() = runTest {
        val repo = FakeOrgRepo(lookupResult = OrgLookupResponse(id = 9, name = "Trädgården", emoji = null))
        val vm = JoinOrgViewModel(repo, FakeUserRefresher())
        vm.setName("Trädgården")
        vm.submit()
        advanceUntilIdle()
        val s = vm.uiState.value
        assertEquals("Trädgården", s.requestSentOrgName)
        assertEquals(listOf(9L), repo.joinedIds)
    }
}

private class FakeOrgRepo(
    private val lookupResult: OrgLookupResponse? = null,
) : OrgRepository {
    val joinedIds = mutableListOf<Long>()
    override suspend fun lookup(name: String): OrgLookupResponse? = lookupResult
    override suspend fun requestJoin(orgId: Long): OrgJoinRequestResponse {
        joinedIds += orgId
        return OrgJoinRequestResponse(
            id = 1L, orgId = orgId, orgName = "X", userId = 1L,
            userEmail = "u@x.com", userDisplayName = "U",
            status = "PENDING", createdAt = "2026-05-01T00:00:00Z",
        )
    }
    override suspend fun listJoinRequests(orgId: Long): List<OrgJoinRequestResponse> = emptyList()
    override suspend fun acceptJoinRequest(orgId: Long, reqId: Long) {}
    override suspend fun declineJoinRequest(orgId: Long, reqId: Long) {}
    override suspend fun listMembers(orgId: Long): List<OrgMemberResponse> = emptyList()
    override suspend fun removeMember(orgId: Long, userId: Long) {}
    override suspend fun listInvites(orgId: Long): List<OrgInviteResponse> = emptyList()
    override suspend fun invite(orgId: Long, email: String) = OrgInviteResponse(
        id = 0L, orgId = orgId, orgName = "X", email = email,
        invitedByName = "O", status = "PENDING", createdAt = "2026-05-01T00:00:00Z",
    )
    override suspend fun cancelInvite(orgId: Long, inviteId: Long) {}
    override suspend fun create(name: String, emoji: String?) = OrganizationResponse(
        id = 0L, name = name, emoji = emoji, role = "OWNER", createdAt = "2026-05-01T00:00:00Z",
    )
    override suspend fun update(orgId: Long, name: String?, emoji: String?) = OrganizationResponse(
        id = orgId, name = name ?: "", emoji = emoji, role = "OWNER", createdAt = "2026-05-01T00:00:00Z",
    )
}

private class FakeUserRefresher : UserRefresher {
    override suspend fun refreshUser(): UserResponse = UserResponse(
        id = 1L, email = "u@x.com", displayName = "U", avatarUrl = null,
        role = "USER", language = "sv", organizations = emptyList(),
        createdAt = "2026-05-01T00:00:00Z",
    )
}
