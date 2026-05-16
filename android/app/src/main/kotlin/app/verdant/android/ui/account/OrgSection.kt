package app.verdant.android.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.verdant.android.data.model.OrgInviteResponse
import app.verdant.android.data.model.OrgJoinRequestResponse
import app.verdant.android.data.model.OrgMemberResponse
import app.verdant.android.data.repository.OrgRepository
import app.verdant.android.data.repository.UserRefresher
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrgState(
    val isLoading: Boolean = true,
    val orgId: Long? = null,
    val orgName: String = "",
    val orgEmoji: String? = null,
    val isOwner: Boolean = false,
    val currentUserId: Long? = null,
    val members: List<OrgMemberResponse> = emptyList(),
    val invites: List<OrgInviteResponse> = emptyList(),
    val joinRequests: List<OrgJoinRequestResponse> = emptyList(),
    val error: String? = null,
    val leftOrg: Boolean = false,
)

@HiltViewModel
class OrgViewModel @Inject constructor(
    private val orgRepository: OrgRepository,
    private val userRefresher: UserRefresher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrgState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = OrgState(isLoading = true)
            try {
                val user = userRefresher.refreshUser()
                val first = user.organizations.firstOrNull() ?: run {
                    _uiState.value = OrgState(isLoading = false)
                    return@launch
                }
                val members = orgRepository.listMembers(first.orgId)
                val invites = if (first.role == "OWNER") orgRepository.listInvites(first.orgId) else emptyList()
                val requests = if (first.role == "OWNER") orgRepository.listJoinRequests(first.orgId) else emptyList()
                _uiState.value = OrgState(
                    isLoading = false,
                    orgId = first.orgId,
                    orgName = first.orgName,
                    orgEmoji = first.orgEmoji,
                    isOwner = first.role == "OWNER",
                    currentUserId = user.id,
                    members = members,
                    invites = invites,
                    joinRequests = requests,
                )
            } catch (e: Exception) {
                _uiState.value = OrgState(isLoading = false, error = e.message)
            }
        }
    }

    fun updateOrg(name: String, emoji: String?) {
        val orgId = _uiState.value.orgId ?: return
        viewModelScope.launch {
            try {
                val org = orgRepository.update(orgId, name.trim().ifBlank { null }, emoji?.trim()?.ifBlank { null })
                _uiState.value = _uiState.value.copy(orgName = org.name, orgEmoji = org.emoji)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun invite(email: String) {
        val orgId = _uiState.value.orgId ?: return
        viewModelScope.launch {
            try {
                val invite = orgRepository.invite(orgId, email)
                _uiState.value = _uiState.value.copy(invites = _uiState.value.invites + invite)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun cancelInvite(inviteId: Long) {
        val orgId = _uiState.value.orgId ?: return
        viewModelScope.launch {
            try {
                orgRepository.cancelInvite(orgId, inviteId)
                _uiState.value = _uiState.value.copy(invites = _uiState.value.invites.filter { it.id != inviteId })
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun acceptJoinRequest(reqId: Long) {
        val orgId = _uiState.value.orgId ?: return
        viewModelScope.launch {
            try {
                orgRepository.acceptJoinRequest(orgId, reqId)
                val members = orgRepository.listMembers(orgId)
                _uiState.value = _uiState.value.copy(
                    joinRequests = _uiState.value.joinRequests.filter { it.id != reqId },
                    members = members,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun declineJoinRequest(reqId: Long) {
        val orgId = _uiState.value.orgId ?: return
        viewModelScope.launch {
            try {
                orgRepository.declineJoinRequest(orgId, reqId)
                _uiState.value = _uiState.value.copy(
                    joinRequests = _uiState.value.joinRequests.filter { it.id != reqId },
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun removeMember(userId: Long) {
        val orgId = _uiState.value.orgId ?: return
        viewModelScope.launch {
            try {
                orgRepository.removeMember(orgId, userId)
                _uiState.value = _uiState.value.copy(
                    members = _uiState.value.members.filter { it.userId != userId },
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun leaveOrg() {
        val orgId = _uiState.value.orgId ?: return
        val userId = _uiState.value.currentUserId ?: return
        viewModelScope.launch {
            try {
                orgRepository.removeMember(orgId, userId)
                _uiState.value = _uiState.value.copy(leftOrg = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@Composable
fun OrgSection(
    onLeft: () -> Unit,
    viewModel: OrgViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showInviteDialog by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var inviteEmail by remember { mutableStateOf("") }

    LaunchedEffect(state.leftOrg) { if (state.leftOrg) onLeft() }

    Column {
        FaltetSectionHeader(label = "Organisation")
        FaltetListRow(
            title = "${state.orgEmoji ?: "🌱"}  ${state.orgName}",
            actions = if (state.isOwner) {
                {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Redigera", tint = FaltetAccent)
                    }
                }
            } else null,
        )
        if (state.members.isNotEmpty()) {
            FaltetSectionHeader(label = "Medlemmar")
            state.members.forEach { m ->
                FaltetListRow(
                    title = m.displayName,
                    meta = if (m.role == "OWNER") "Ägare" else m.email,
                    actions = if (state.isOwner && m.userId != state.currentUserId) {
                        {
                            IconButton(onClick = { viewModel.removeMember(m.userId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Ta bort", tint = FaltetClay)
                            }
                        }
                    } else null,
                )
            }
        }
        if (state.isOwner) {
            if (state.invites.isNotEmpty()) {
                FaltetSectionHeader(label = "Inbjudningar")
                state.invites.forEach { inv ->
                    FaltetListRow(
                        title = inv.email,
                        meta = "Inbjuden",
                        actions = {
                            IconButton(onClick = { viewModel.cancelInvite(inv.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Avbryt", tint = FaltetClay)
                            }
                        },
                    )
                }
            }
            if (state.joinRequests.isNotEmpty()) {
                FaltetSectionHeader(label = "Förfrågningar")
                state.joinRequests.forEach { req ->
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                        Text(req.userDisplayName, fontSize = 14.sp, color = FaltetInk)
                        Text(req.userEmail, fontSize = 12.sp, color = FaltetForest)
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            TextButton(onClick = { viewModel.acceptJoinRequest(req.id) }) {
                                Text("Godkänn", color = FaltetAccent)
                            }
                            Spacer(Modifier.width(12.dp))
                            TextButton(onClick = { viewModel.declineJoinRequest(req.id) }) {
                                Text("Avvisa", color = FaltetClay)
                            }
                        }
                    }
                }
            }
            FaltetListRow(
                title = "Bjud in via e-post",
                onClick = { showInviteDialog = true },
            )
        }
        if (!state.isOwner && state.orgId != null) {
            FaltetListRow(
                title = "Lämna organisationen",
                onClick = { showLeaveConfirm = true },
            )
        }
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false; inviteEmail = "" },
            title = { Text("Bjud in") },
            text = {
                OutlinedTextField(
                    value = inviteEmail,
                    onValueChange = { inviteEmail = it },
                    label = { Text("E-postadress") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val email = inviteEmail.trim()
                    if (email.isNotBlank()) viewModel.invite(email)
                    showInviteDialog = false
                    inviteEmail = ""
                }) { Text("Skicka") }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false; inviteEmail = "" }) { Text("Avbryt") }
            },
        )
    }

    if (showEditDialog) {
        var editName by remember(state.orgName) { mutableStateOf(state.orgName) }
        var editEmoji by remember(state.orgEmoji) { mutableStateOf(state.orgEmoji ?: "🌱") }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Redigera organisation") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Namn") },
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editEmoji,
                        onValueChange = { editEmoji = it.take(4) },
                        label = { Text("Emoji") },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editName.isNotBlank()) {
                        viewModel.updateOrg(editName, editEmoji)
                    }
                    showEditDialog = false
                }) { Text("Spara") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Avbryt") }
            },
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Lämna organisationen") },
            text = { Text("Du kommer att loggas ut. Fortsätt?") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveConfirm = false
                    viewModel.leaveOrg()
                }) { Text("Lämna", color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Avbryt") }
            },
        )
    }
}
