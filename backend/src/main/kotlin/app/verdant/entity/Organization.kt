package app.verdant.entity

import java.time.Instant

data class Organization(
    val id: Long? = null,
    val name: String,
    val emoji: String? = "🌱",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class OrgRole { OWNER, MEMBER }

data class OrgMember(
    val id: Long? = null,
    val orgId: Long,
    val userId: Long,
    val role: OrgRole = OrgRole.MEMBER,
    val joinedAt: Instant = Instant.now(),
)

enum class InviteStatus { PENDING, ACCEPTED, DECLINED }

data class OrgInvite(
    val id: Long? = null,
    val orgId: Long,
    val email: String,
    val invitedBy: Long,
    val status: InviteStatus = InviteStatus.PENDING,
    val createdAt: Instant = Instant.now(),
)
