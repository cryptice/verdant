package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.InviteStatus
import app.verdant.entity.OrgInvite
import app.verdant.entity.OrgMember
import app.verdant.entity.OrgRole
import app.verdant.entity.Organization
import app.verdant.repository.OrgInviteRepository
import app.verdant.repository.OrgMemberRepository
import app.verdant.repository.OrganizationRepository
import app.verdant.repository.UserRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
    private val orgMemberRepository: OrgMemberRepository,
    private val orgInviteRepository: OrgInviteRepository,
    private val userRepository: UserRepository,
) {

    fun getOrganizationsForUser(userId: Long): List<OrganizationResponse> {
        val memberships = orgMemberRepository.findByUserId(userId)
        val orgIds = memberships.map { it.orgId }.toSet()
        if (orgIds.isEmpty()) return emptyList()
        val roleByOrgId = memberships.associate { it.orgId to it.role }
        return orgIds.mapNotNull { orgId ->
            organizationRepository.findById(orgId)?.toResponse(roleByOrgId[orgId] ?: OrgRole.MEMBER)
        }
    }

    @Transactional
    fun createOrganization(request: CreateOrganizationRequest, userId: Long): OrganizationResponse {
        val org = organizationRepository.persist(
            Organization(
                name = request.name,
                emoji = request.emoji,
            )
        )
        orgMemberRepository.persist(
            OrgMember(
                orgId = org.id!!,
                userId = userId,
                role = OrgRole.OWNER,
            )
        )
        return org.toResponse(OrgRole.OWNER)
    }

    fun updateOrganization(orgId: Long, request: UpdateOrganizationRequest, userId: Long): OrganizationResponse {
        checkOwner(orgId, userId)
        val org = organizationRepository.findById(orgId) ?: throw NotFoundException("Organization not found")
        val updated = org.copy(
            name = request.name ?: org.name,
            emoji = request.emoji ?: org.emoji,
        )
        organizationRepository.update(updated)
        val role = orgMemberRepository.findByOrgAndUser(orgId, userId)?.role ?: OrgRole.OWNER
        return updated.toResponse(role)
    }

    fun deleteOrganization(orgId: Long, userId: Long) {
        checkOwner(orgId, userId)
        organizationRepository.delete(orgId)
    }

    fun getMembers(orgId: Long): List<OrgMemberResponse> {
        val members = orgMemberRepository.findByOrgId(orgId)
        val userIds = members.map { it.userId }.toSet()
        val usersById = userRepository.findByIds(userIds)
        return members.mapNotNull { member ->
            val user = usersById[member.userId] ?: return@mapNotNull null
            OrgMemberResponse(
                id = member.id!!,
                userId = user.id!!,
                email = user.email,
                displayName = user.displayName,
                avatarUrl = user.avatarUrl,
                role = member.role,
                joinedAt = member.joinedAt,
            )
        }
    }

    fun inviteMember(orgId: Long, request: InviteMemberRequest, userId: Long): OrgInviteResponse {
        checkOwner(orgId, userId)
        val org = organizationRepository.findById(orgId) ?: throw NotFoundException("Organization not found")
        val inviter = userRepository.findById(userId) ?: throw NotFoundException("User not found")
        val invite = orgInviteRepository.persist(
            OrgInvite(
                orgId = orgId,
                email = request.email,
                invitedBy = userId,
            )
        )
        return invite.toResponse(orgName = org.name, invitedByName = inviter.displayName)
    }

    fun removeMember(orgId: Long, targetUserId: Long, userId: Long) {
        val isSelf = targetUserId == userId
        if (!isSelf) {
            checkOwner(orgId, userId)
        }
        if (!isSelf) {
            val target = orgMemberRepository.findByOrgAndUser(orgId, targetUserId)
                ?: throw NotFoundException("Member not found")
            if (target.role == OrgRole.OWNER) {
                val owners = orgMemberRepository.findByOrgId(orgId).count { it.role == OrgRole.OWNER }
                if (owners <= 1) throw BadRequestException("Cannot remove the last owner")
            }
        } else {
            val member = orgMemberRepository.findByOrgAndUser(orgId, userId)
                ?: throw NotFoundException("Member not found")
            if (member.role == OrgRole.OWNER) {
                val owners = orgMemberRepository.findByOrgId(orgId).count { it.role == OrgRole.OWNER }
                if (owners <= 1) throw BadRequestException("Cannot remove the last owner")
            }
        }
        orgMemberRepository.delete(orgId, targetUserId)
    }

    fun getPendingInvitesForUser(email: String): List<OrgInviteResponse> {
        val invites = orgInviteRepository.findPendingByEmail(email)
        return invites.mapNotNull { invite ->
            val org = organizationRepository.findById(invite.orgId) ?: return@mapNotNull null
            val inviter = userRepository.findById(invite.invitedBy) ?: return@mapNotNull null
            invite.toResponse(orgName = org.name, invitedByName = inviter.displayName)
        }
    }

    @Transactional
    fun acceptInvite(inviteId: Long, userId: Long): OrganizationResponse {
        val invite = orgInviteRepository.findById(inviteId) ?: throw NotFoundException("Invite not found")
        val user = userRepository.findById(userId) ?: throw NotFoundException("User not found")
        if (!invite.email.equals(user.email, ignoreCase = true)) throw ForbiddenException()
        if (invite.status != InviteStatus.PENDING) throw BadRequestException("Invite is no longer pending")
        orgInviteRepository.updateStatus(inviteId, InviteStatus.ACCEPTED)
        orgMemberRepository.persist(
            OrgMember(
                orgId = invite.orgId,
                userId = userId,
                role = OrgRole.MEMBER,
            )
        )
        val org = organizationRepository.findById(invite.orgId) ?: throw NotFoundException("Organization not found")
        return org.toResponse(OrgRole.MEMBER)
    }

    fun declineInvite(inviteId: Long, userId: Long) {
        val invite = orgInviteRepository.findById(inviteId) ?: throw NotFoundException("Invite not found")
        val user = userRepository.findById(userId) ?: throw NotFoundException("User not found")
        if (!invite.email.equals(user.email, ignoreCase = true)) throw ForbiddenException()
        if (invite.status != InviteStatus.PENDING) throw BadRequestException("Invite is no longer pending")
        orgInviteRepository.updateStatus(inviteId, InviteStatus.DECLINED)
    }

    private fun checkOwner(orgId: Long, userId: Long) {
        val member = orgMemberRepository.findByOrgAndUser(orgId, userId)
            ?: throw ForbiddenException()
        if (member.role != OrgRole.OWNER) throw ForbiddenException()
    }
}

fun Organization.toResponse(role: OrgRole) = OrganizationResponse(
    id = id!!,
    name = name,
    emoji = emoji,
    role = role,
    createdAt = createdAt,
)

fun OrgInvite.toResponse(orgName: String, invitedByName: String) = OrgInviteResponse(
    id = id!!,
    orgId = orgId,
    orgName = orgName,
    email = email,
    invitedByName = invitedByName,
    status = status,
    createdAt = createdAt,
)
