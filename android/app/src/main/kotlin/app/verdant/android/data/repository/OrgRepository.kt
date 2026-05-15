package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateOrganizationRequest
import app.verdant.android.data.model.InviteMemberRequest
import app.verdant.android.data.model.OrgInviteResponse
import app.verdant.android.data.model.OrgJoinRequestResponse
import app.verdant.android.data.model.OrgLookupResponse
import app.verdant.android.data.model.OrgMemberResponse
import app.verdant.android.data.model.OrganizationResponse
import app.verdant.android.data.model.UpdateOrganizationRequest
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/** Organisation management. Interface so ViewModels can be tested with a fake. */
interface OrgRepository {
    suspend fun lookup(name: String): OrgLookupResponse?
    suspend fun requestJoin(orgId: Long): OrgJoinRequestResponse
    suspend fun listJoinRequests(orgId: Long): List<OrgJoinRequestResponse>
    suspend fun acceptJoinRequest(orgId: Long, reqId: Long)
    suspend fun declineJoinRequest(orgId: Long, reqId: Long)
    suspend fun listMembers(orgId: Long): List<OrgMemberResponse>
    suspend fun removeMember(orgId: Long, userId: Long)
    suspend fun listInvites(orgId: Long): List<OrgInviteResponse>
    suspend fun invite(orgId: Long, email: String): OrgInviteResponse
    suspend fun cancelInvite(orgId: Long, inviteId: Long)
    suspend fun create(name: String, emoji: String?): OrganizationResponse
    suspend fun update(orgId: Long, name: String?, emoji: String?): OrganizationResponse
}

@Singleton
class OrgRepositoryImpl @Inject constructor(
    private val api: VerdantApi,
) : OrgRepository {
    override suspend fun lookup(name: String): OrgLookupResponse? = try {
        api.lookupOrg(name)
    } catch (e: HttpException) {
        if (e.code() == 404) null else throw e
    }

    override suspend fun requestJoin(orgId: Long) = api.requestJoin(orgId)
    override suspend fun listJoinRequests(orgId: Long) = api.listJoinRequests(orgId)
    override suspend fun acceptJoinRequest(orgId: Long, reqId: Long) { api.acceptJoinRequest(orgId, reqId) }
    override suspend fun declineJoinRequest(orgId: Long, reqId: Long) { api.declineJoinRequest(orgId, reqId) }

    override suspend fun listMembers(orgId: Long) = api.listMembers(orgId)
    override suspend fun removeMember(orgId: Long, userId: Long) { api.removeMember(orgId, userId) }

    override suspend fun listInvites(orgId: Long) = api.listOrgInvites(orgId)
    override suspend fun invite(orgId: Long, email: String) =
        api.inviteMember(orgId, InviteMemberRequest(email))
    override suspend fun cancelInvite(orgId: Long, inviteId: Long) { api.cancelInvite(orgId, inviteId) }

    override suspend fun create(name: String, emoji: String?) =
        api.createOrg(CreateOrganizationRequest(name, emoji))
    override suspend fun update(orgId: Long, name: String?, emoji: String?) =
        api.updateOrg(orgId, UpdateOrganizationRequest(name, emoji))
}
