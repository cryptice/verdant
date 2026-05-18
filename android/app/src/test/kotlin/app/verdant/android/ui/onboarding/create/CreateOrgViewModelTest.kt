package app.verdant.android.ui.onboarding.create

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateOrgViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `submit with blank name does nothing`() = runTest {
        val org = FakeOrgRepo()
        val refresher = FakeUserRefresher()
        val vm = CreateOrgViewModel(org, refresher)
        vm.setName("   ")
        vm.submit()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.created)
        assertEquals(0, org.createCalls.size)
        assertEquals(0, refresher.refreshCount)
    }

    @Test
    fun `submit creates org and flips created to true`() = runTest {
        val org = FakeOrgRepo()
        val refresher = FakeUserRefresher()
        val vm = CreateOrgViewModel(org, refresher)
        vm.setName("Trädgården")
        vm.setEmoji("🌻")
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.created)
        assertFalse(vm.uiState.value.isSubmitting)
        assertEquals(listOf("Trädgården" to "🌻"), org.createCalls)
        assertEquals(1, refresher.refreshCount)
    }

    @Test
    fun `submit surfaces error on failure`() = runTest {
        val org = FakeOrgRepo(throwOnCreate = RuntimeException("boom"))
        val refresher = FakeUserRefresher()
        val vm = CreateOrgViewModel(org, refresher)
        vm.setName("X")
        vm.submit()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.created)
        assertFalse(vm.uiState.value.isSubmitting)
        assertEquals("boom", vm.uiState.value.error)
        assertEquals(0, refresher.refreshCount)
    }
}

private class FakeOrgRepo(
    private val throwOnCreate: Throwable? = null,
) : OrgRepository {
    val createCalls = mutableListOf<Pair<String, String?>>()

    override suspend fun create(name: String, emoji: String?): OrganizationResponse {
        throwOnCreate?.let { throw it }
        createCalls += name to emoji
        return OrganizationResponse(
            id = 1L, name = name, emoji = emoji,
            role = "OWNER", createdAt = "2026-05-18T00:00:00Z",
        )
    }

    override suspend fun lookup(name: String): OrgLookupResponse? = error("not used")
    override suspend fun requestJoin(orgId: Long): OrgJoinRequestResponse = error("not used")
    override suspend fun listJoinRequests(orgId: Long): List<OrgJoinRequestResponse> = error("not used")
    override suspend fun acceptJoinRequest(orgId: Long, reqId: Long) { error("not used") }
    override suspend fun declineJoinRequest(orgId: Long, reqId: Long) { error("not used") }
    override suspend fun listMembers(orgId: Long): List<OrgMemberResponse> = error("not used")
    override suspend fun removeMember(orgId: Long, userId: Long) { error("not used") }
    override suspend fun listInvites(orgId: Long): List<OrgInviteResponse> = error("not used")
    override suspend fun invite(orgId: Long, email: String): OrgInviteResponse = error("not used")
    override suspend fun cancelInvite(orgId: Long, inviteId: Long) { error("not used") }
    override suspend fun update(orgId: Long, name: String?, emoji: String?): OrganizationResponse = error("not used")
}

private class FakeUserRefresher : UserRefresher {
    var refreshCount = 0
    override suspend fun refreshUser(): UserResponse {
        refreshCount += 1
        return UserResponse(
            id = 1L, email = "u@x.com", displayName = "U", avatarUrl = null,
            role = "USER", language = "sv", organizations = emptyList(),
            createdAt = "2026-05-18T00:00:00Z",
        )
    }
}
