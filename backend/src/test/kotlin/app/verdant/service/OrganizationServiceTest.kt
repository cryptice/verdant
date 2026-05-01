package app.verdant.service

import app.verdant.dto.CreateOrganizationRequest
import app.verdant.entity.OrgMember
import app.verdant.entity.Organization
import app.verdant.repository.OrgInviteRepository
import app.verdant.repository.OrgMemberRepository
import app.verdant.repository.OrganizationRepository
import app.verdant.repository.UserRepository
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
    private val userRepo: UserRepository = mock()
    private val supplyService: SupplyService = mock()

    private val service = OrganizationService(orgRepo, memberRepo, inviteRepo, userRepo, supplyService)

    @Test
    fun `createOrganization seeds inexhaustible fertilizers for the new org`() {
        val persisted = Organization(id = 99L, name = "Test", emoji = "🌻")
        whenever(orgRepo.persist(any<Organization>())).thenReturn(persisted)
        whenever(memberRepo.persist(any<OrgMember>())).thenAnswer { it.arguments[0] as OrgMember }

        service.createOrganization(CreateOrganizationRequest(name = "Test", emoji = "🌻"), userId = 7L)

        verify(supplyService).seedInexhaustibleFertilizers(eq(99L))
    }
}
