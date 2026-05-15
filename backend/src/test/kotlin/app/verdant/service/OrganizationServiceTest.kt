package app.verdant.service

import app.verdant.dto.CreateOrganizationRequest
import app.verdant.entity.JoinRequestStatus
import app.verdant.entity.OrgJoinRequest
import app.verdant.entity.OrgMember
import app.verdant.entity.OrgRole
import app.verdant.entity.Organization
import app.verdant.entity.User
import app.verdant.repository.OrgInviteRepository
import app.verdant.repository.OrgJoinRequestRepository
import app.verdant.repository.OrgMemberRepository
import app.verdant.repository.OrganizationRepository
import app.verdant.repository.UserRepository
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class OrganizationServiceTest {

    private val orgRepo: OrganizationRepository = mock()
    private val memberRepo: OrgMemberRepository = mock()
    private val inviteRepo: OrgInviteRepository = mock()
    private val joinRequestRepo: OrgJoinRequestRepository = mock()
    private val userRepo: UserRepository = mock()
    private val supplyService: SupplyService = mock()

    private val service = OrganizationService(orgRepo, memberRepo, inviteRepo, joinRequestRepo, userRepo, supplyService)

    @Test
    fun `createOrganization seeds inexhaustible fertilizers for the new org`() {
        val persisted = Organization(id = 99L, name = "Test", emoji = "🌻")
        whenever(orgRepo.persist(any<Organization>())).thenReturn(persisted)
        whenever(memberRepo.persist(any<OrgMember>())).thenAnswer { it.arguments[0] as OrgMember }

        service.createOrganization(CreateOrganizationRequest(name = "Test", emoji = "🌻"), userId = 7L)

        verify(supplyService).seedInexhaustibleFertilizers(eq(99L))
    }

    @Test
    fun `lookupByName returns the org when name matches exactly`() {
        val org = Organization(id = 1L, name = "Trädgården", emoji = "🌱")
        whenever(orgRepo.findByNameIgnoreCase("Trädgården")).thenReturn(org)

        val result = service.lookupByName("Trädgården")

        assertEquals(1L, result?.id)
        assertEquals("Trädgården", result?.name)
        assertEquals("🌱", result?.emoji)
    }

    @Test
    fun `lookupByName is case-insensitive`() {
        val org = Organization(id = 1L, name = "Trädgården", emoji = "🌱")
        whenever(orgRepo.findByNameIgnoreCase("trädgården")).thenReturn(org)

        val result = service.lookupByName("trädgården")
        assertEquals(1L, result?.id)
    }

    @Test
    fun `lookupByName returns null when no match`() {
        whenever(orgRepo.findByNameIgnoreCase("unknown")).thenReturn(null)
        assertNull(service.lookupByName("unknown"))
    }

    // ── requestJoin ─────────────────────────────────────────────────────────

    @Test
    fun `requestJoin creates a PENDING request when user is not a member`() {
        val orgId = 1L; val userId = 7L
        val org = Organization(id = orgId, name = "Trädgården", emoji = "🌱")
        val user = User(id = userId, email = "u@example.com", displayName = "U")
        whenever(orgRepo.findById(orgId)).thenReturn(org)
        whenever(userRepo.findById(userId)).thenReturn(user)
        whenever(memberRepo.findByOrgAndUser(orgId, userId)).thenReturn(null)
        whenever(joinRequestRepo.findByOrgAndUser(orgId, userId)).thenReturn(null)
        whenever(joinRequestRepo.persist(any())).thenAnswer {
            (it.arguments[0] as OrgJoinRequest).copy(id = 42L)
        }

        val result = service.requestJoin(orgId, userId)

        assertEquals(42L, result.id)
        assertEquals(JoinRequestStatus.PENDING, result.status)
        verify(joinRequestRepo).persist(any())
    }

    @Test
    fun `requestJoin is rejected when the user is already a member`() {
        val orgId = 1L; val userId = 7L
        whenever(orgRepo.findById(orgId)).thenReturn(Organization(id = orgId, name = "O"))
        whenever(memberRepo.findByOrgAndUser(orgId, userId)).thenReturn(
            OrgMember(id = 1L, orgId = orgId, userId = userId, role = OrgRole.MEMBER)
        )

        assertThrows<BadRequestException> { service.requestJoin(orgId, userId) }
    }

    @Test
    fun `requestJoin returns existing PENDING request idempotently`() {
        val orgId = 1L; val userId = 7L
        val existing = OrgJoinRequest(id = 5L, orgId = orgId, userId = userId, status = JoinRequestStatus.PENDING)
        val org = Organization(id = orgId, name = "Trädgården")
        val user = User(id = userId, email = "u@example.com", displayName = "U")
        whenever(orgRepo.findById(orgId)).thenReturn(org)
        whenever(userRepo.findById(userId)).thenReturn(user)
        whenever(memberRepo.findByOrgAndUser(orgId, userId)).thenReturn(null)
        whenever(joinRequestRepo.findByOrgAndUser(orgId, userId)).thenReturn(existing)

        val result = service.requestJoin(orgId, userId)

        assertEquals(5L, result.id)
        verify(joinRequestRepo, never()).persist(any())
    }

    @Test
    fun `requestJoin deletes a DECLINED request and creates a new PENDING one`() {
        val orgId = 1L; val userId = 7L
        val declined = OrgJoinRequest(id = 5L, orgId = orgId, userId = userId, status = JoinRequestStatus.DECLINED)
        val org = Organization(id = orgId, name = "Trädgården")
        val user = User(id = userId, email = "u@example.com", displayName = "U")
        whenever(orgRepo.findById(orgId)).thenReturn(org)
        whenever(userRepo.findById(userId)).thenReturn(user)
        whenever(memberRepo.findByOrgAndUser(orgId, userId)).thenReturn(null)
        whenever(joinRequestRepo.findByOrgAndUser(orgId, userId)).thenReturn(declined)
        whenever(joinRequestRepo.persist(any())).thenAnswer {
            (it.arguments[0] as OrgJoinRequest).copy(id = 99L)
        }

        val result = service.requestJoin(orgId, userId)

        verify(joinRequestRepo).delete(5L)
        verify(joinRequestRepo).persist(any())
        assertEquals(99L, result.id)
        assertEquals(JoinRequestStatus.PENDING, result.status)
    }

    @Test
    fun `requestJoin throws NotFoundException for unknown org`() {
        whenever(orgRepo.findById(1L)).thenReturn(null)
        assertThrows<NotFoundException> { service.requestJoin(1L, 7L) }
    }

    // ── accept/decline/list join requests ───────────────────────────────────

    @Test
    fun `getPendingJoinRequests returns pending requests for owner`() {
        val orgId = 1L; val ownerId = 7L; val requesterId = 9L
        val req = OrgJoinRequest(id = 10L, orgId = orgId, userId = requesterId, status = JoinRequestStatus.PENDING)
        val org = Organization(id = orgId, name = "O")
        val requester = User(id = requesterId, email = "r@example.com", displayName = "R")
        whenever(memberRepo.findByOrgAndUser(orgId, ownerId))
            .thenReturn(OrgMember(id = 1L, orgId = orgId, userId = ownerId, role = OrgRole.OWNER))
        whenever(joinRequestRepo.findPendingByOrgId(orgId)).thenReturn(listOf(req))
        whenever(orgRepo.findById(orgId)).thenReturn(org)
        whenever(userRepo.findByIds(setOf(requesterId))).thenReturn(mapOf(requesterId to requester))

        val result = service.getPendingJoinRequests(orgId, ownerId)

        assertEquals(1, result.size)
        assertEquals(10L, result[0].id)
        assertEquals("r@example.com", result[0].userEmail)
    }

    @Test
    fun `getPendingJoinRequests rejects non-owners`() {
        val orgId = 1L; val memberId = 7L
        whenever(memberRepo.findByOrgAndUser(orgId, memberId))
            .thenReturn(OrgMember(id = 1L, orgId = orgId, userId = memberId, role = OrgRole.MEMBER))

        assertThrows<ForbiddenException> { service.getPendingJoinRequests(orgId, memberId) }
    }

    @Test
    fun `acceptJoinRequest creates an OrgMember and flips status`() {
        val orgId = 1L; val ownerId = 7L; val requesterId = 9L
        val req = OrgJoinRequest(id = 10L, orgId = orgId, userId = requesterId, status = JoinRequestStatus.PENDING)
        val org = Organization(id = orgId, name = "O")
        val requester = User(id = requesterId, email = "r@example.com", displayName = "R")
        whenever(memberRepo.findByOrgAndUser(orgId, ownerId))
            .thenReturn(OrgMember(id = 1L, orgId = orgId, userId = ownerId, role = OrgRole.OWNER))
        whenever(joinRequestRepo.findById(10L)).thenReturn(req)
        whenever(orgRepo.findById(orgId)).thenReturn(org)
        whenever(userRepo.findById(requesterId)).thenReturn(requester)

        service.acceptJoinRequest(orgId, 10L, ownerId)

        verify(memberRepo).persist(check {
            assertEquals(orgId, it.orgId)
            assertEquals(requesterId, it.userId)
            assertEquals(OrgRole.MEMBER, it.role)
        })
        verify(joinRequestRepo).updateStatus(10L, JoinRequestStatus.ACCEPTED)
    }

    @Test
    fun `acceptJoinRequest rejects when status is not PENDING`() {
        val orgId = 1L; val ownerId = 7L
        val req = OrgJoinRequest(id = 10L, orgId = orgId, userId = 9L, status = JoinRequestStatus.ACCEPTED)
        whenever(memberRepo.findByOrgAndUser(orgId, ownerId))
            .thenReturn(OrgMember(id = 1L, orgId = orgId, userId = ownerId, role = OrgRole.OWNER))
        whenever(joinRequestRepo.findById(10L)).thenReturn(req)

        assertThrows<BadRequestException> { service.acceptJoinRequest(orgId, 10L, ownerId) }
    }

    @Test
    fun `acceptJoinRequest rejects cross-org request`() {
        val orgId = 1L; val ownerId = 7L
        val req = OrgJoinRequest(id = 10L, orgId = 99L /* different org */, userId = 9L, status = JoinRequestStatus.PENDING)
        whenever(memberRepo.findByOrgAndUser(orgId, ownerId))
            .thenReturn(OrgMember(id = 1L, orgId = orgId, userId = ownerId, role = OrgRole.OWNER))
        whenever(joinRequestRepo.findById(10L)).thenReturn(req)

        assertThrows<NotFoundException> { service.acceptJoinRequest(orgId, 10L, ownerId) }
    }

    @Test
    fun `declineJoinRequest flips status to DECLINED`() {
        val orgId = 1L; val ownerId = 7L
        val req = OrgJoinRequest(id = 10L, orgId = orgId, userId = 9L, status = JoinRequestStatus.PENDING)
        whenever(memberRepo.findByOrgAndUser(orgId, ownerId))
            .thenReturn(OrgMember(id = 1L, orgId = orgId, userId = ownerId, role = OrgRole.OWNER))
        whenever(joinRequestRepo.findById(10L)).thenReturn(req)

        service.declineJoinRequest(orgId, 10L, ownerId)

        verify(joinRequestRepo).updateStatus(10L, JoinRequestStatus.DECLINED)
        verify(memberRepo, never()).persist(any())
    }
}
