package app.verdant.service

import app.verdant.dto.CreateOrganizationRequest
import app.verdant.entity.OrgMember
import app.verdant.entity.Organization
import app.verdant.repository.OrgInviteRepository
import app.verdant.repository.OrgJoinRequestRepository
import app.verdant.repository.OrgMemberRepository
import app.verdant.repository.OrganizationRepository
import app.verdant.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
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
}
