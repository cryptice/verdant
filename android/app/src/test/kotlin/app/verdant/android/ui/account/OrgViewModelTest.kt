package app.verdant.android.ui.account

import app.verdant.android.data.model.OrgInviteResponse
import app.verdant.android.data.model.OrgJoinRequestResponse
import app.verdant.android.data.model.OrgLookupResponse
import app.verdant.android.data.model.OrgMemberResponse
import app.verdant.android.data.model.OrganizationResponse
import app.verdant.android.data.model.UserOrgMembership
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrgViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `load populates owner's view`() = runTest {
        val org = OrgRepoFake(
            members = listOf(member(1, "Alice", "OWNER"), member(2, "Bob", "MEMBER")),
            invites = listOf(invite(10, "x@y.com")),
            joinRequests = listOf(joinReq(20, "z@y.com", "Z")),
        )
        val refresher = UserRefresherFake(userWithOrg(role = "OWNER"))
        val vm = OrgViewModel(org, refresher)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(2, s.members.size)
        assertEquals(1, s.invites.size)
        assertEquals(1, s.joinRequests.size)
        assertTrue(s.isOwner)
    }

    @Test
    fun `invite appends to state`() = runTest {
        val org = OrgRepoFake()
        val vm = OrgViewModel(org, UserRefresherFake(userWithOrg(role = "OWNER")))
        advanceUntilIdle()
        vm.invite("new@example.com")
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.invites.size)
        assertEquals("new@example.com", vm.uiState.value.invites.single().email)
    }

    @Test
    fun `cancelInvite removes from state`() = runTest {
        val org = OrgRepoFake(invites = listOf(invite(10, "a@b.com"), invite(11, "c@d.com")))
        val vm = OrgViewModel(org, UserRefresherFake(userWithOrg(role = "OWNER")))
        advanceUntilIdle()
        vm.cancelInvite(10L)
        advanceUntilIdle()
        assertEquals(listOf(11L), vm.uiState.value.invites.map { it.id })
    }

    @Test
    fun `updateOrg updates state with returned org`() = runTest {
        val org = object : app.verdant.android.data.repository.OrgRepository by OrgRepoFake() {
            override suspend fun update(orgId: Long, name: String?, emoji: String?) =
                app.verdant.android.data.model.OrganizationResponse(
                    id = orgId, name = name ?: "X", emoji = emoji ?: "🌳",
                    role = "OWNER", createdAt = "2026-05-01T00:00:00Z",
                )
        }
        val vm = OrgViewModel(org, UserRefresherFake(userWithOrg(role = "OWNER")))
        advanceUntilIdle()
        vm.updateOrg("Nytt namn", "🌳")
        advanceUntilIdle()
        val s = vm.uiState.value
        assertEquals("Nytt namn", s.orgName)
        assertEquals("🌳", s.orgEmoji)
    }

    @Test
    fun `acceptJoinRequest removes the request and reloads members`() = runTest {
        val org = OrgRepoFake(
            members = listOf(member(1, "A", "OWNER")),
            joinRequests = listOf(joinReq(20, "z@y.com", "Z")),
            membersAfterAccept = listOf(member(1, "A", "OWNER"), member(2, "Z", "MEMBER")),
        )
        val vm = OrgViewModel(org, UserRefresherFake(userWithOrg(role = "OWNER")))
        advanceUntilIdle()
        vm.acceptJoinRequest(20L)
        advanceUntilIdle()
        val s = vm.uiState.value
        assertTrue(s.joinRequests.isEmpty())
        assertEquals(2, s.members.size)
    }

    private fun member(id: Long, name: String, role: String) = OrgMemberResponse(
        id = id, userId = id, email = "$name@x.com", displayName = name,
        avatarUrl = null, role = role, joinedAt = "2026-05-01T00:00:00Z",
    )
    private fun invite(id: Long, email: String) = OrgInviteResponse(
        id = id, orgId = 1L, orgName = "Org", email = email,
        invitedByName = "Owner", status = "PENDING", createdAt = "2026-05-01T00:00:00Z",
    )
    private fun joinReq(id: Long, email: String, name: String) = OrgJoinRequestResponse(
        id = id, orgId = 1L, orgName = "Org", userId = id, userEmail = email,
        userDisplayName = name, status = "PENDING", createdAt = "2026-05-01T00:00:00Z",
    )
    private fun userWithOrg(role: String) = UserResponse(
        id = 1L, email = "owner@x.com", displayName = "Owner", avatarUrl = null,
        role = "USER", language = "sv",
        organizations = listOf(UserOrgMembership(orgId = 1L, orgName = "Org", orgEmoji = "🌱", role = role)),
        createdAt = "2026-05-01T00:00:00Z",
    )
}

private class OrgRepoFake(
    private val members: List<OrgMemberResponse> = emptyList(),
    private val invites: List<OrgInviteResponse> = emptyList(),
    private val joinRequests: List<OrgJoinRequestResponse> = emptyList(),
    private val membersAfterAccept: List<OrgMemberResponse>? = null,
) : OrgRepository {
    private var acceptCalled = false
    override suspend fun lookup(name: String): OrgLookupResponse? = null
    override suspend fun requestJoin(orgId: Long) =
        OrgJoinRequestResponse(id = 0, orgId = orgId, orgName = "Org", userId = 0,
            userEmail = "", userDisplayName = "", status = "PENDING",
            createdAt = "2026-05-01T00:00:00Z")
    override suspend fun listJoinRequests(orgId: Long) = joinRequests
    override suspend fun acceptJoinRequest(orgId: Long, reqId: Long) { acceptCalled = true }
    override suspend fun declineJoinRequest(orgId: Long, reqId: Long) {}
    override suspend fun listMembers(orgId: Long) =
        if (acceptCalled && membersAfterAccept != null) membersAfterAccept else members
    override suspend fun removeMember(orgId: Long, userId: Long) {}
    override suspend fun listInvites(orgId: Long) = invites
    override suspend fun invite(orgId: Long, email: String) =
        OrgInviteResponse(id = 99, orgId = orgId, orgName = "Org", email = email,
            invitedByName = "Owner", status = "PENDING", createdAt = "2026-05-01T00:00:00Z")
    override suspend fun cancelInvite(orgId: Long, inviteId: Long) {}
    override suspend fun create(name: String, emoji: String?) =
        OrganizationResponse(id = 0, name = name, emoji = emoji, role = "OWNER", createdAt = "2026-05-01T00:00:00Z")
    override suspend fun update(orgId: Long, name: String?, emoji: String?) =
        OrganizationResponse(id = orgId, name = name ?: "", emoji = emoji, role = "OWNER", createdAt = "2026-05-01T00:00:00Z")
}

private class UserRefresherFake(private val user: UserResponse) : UserRefresher {
    override suspend fun refreshUser(): UserResponse = user
}
