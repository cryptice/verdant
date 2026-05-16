package app.verdant.android.ui.onboarding.invite

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.verdant.android.data.model.OrgInviteResponse
import app.verdant.android.data.repository.InviteOps
import app.verdant.android.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "InviteOfferScreen"

data class InviteOfferState(
    val isLoading: Boolean = true,
    val invites: List<OrgInviteResponse> = emptyList(),
    val joined: Boolean = false,
    val allDeclined: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class InviteOfferViewModel @Inject constructor(
    private val ops: InviteOps,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InviteOfferState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = InviteOfferState(isLoading = true)
            try {
                val invites = ops.listPendingInvites()
                _uiState.value = InviteOfferState(isLoading = false, invites = invites)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load invites", e)
                _uiState.value = InviteOfferState(isLoading = false, error = e.message)
            }
        }
    }

    fun accept(inviteId: Long) {
        viewModelScope.launch {
            try {
                ops.acceptInvite(inviteId)
                _uiState.value = _uiState.value.copy(joined = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun decline(inviteId: Long) {
        viewModelScope.launch {
            try {
                ops.declineInvite(inviteId)
                val remaining = _uiState.value.invites.filter { it.id != inviteId }
                _uiState.value = _uiState.value.copy(
                    invites = remaining,
                    allDeclined = remaining.isEmpty(),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@Composable
fun InviteOfferScreen(
    onJoined: () -> Unit,
    onAllDeclined: () -> Unit,
    onCreateOrg: () -> Unit,
    onJoinByName: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: InviteOfferViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.joined) { if (state.joined) onJoined() }
    LaunchedEffect(state.allDeclined) { if (state.allDeclined) onAllDeclined() }

    Box(
        modifier = Modifier.fillMaxSize().background(FaltetCream),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = "VERDANT", fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic, fontSize = 48.sp,
                color = FaltetInk, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Du har en inbjudan",
                fontFamily = FaltetDisplay, fontStyle = FontStyle.Italic,
                fontSize = 22.sp, color = FaltetInk,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))

            if (state.isLoading) {
                CircularProgressIndicator(color = FaltetAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                state.invites.forEach { invite ->
                    InviteCard(
                        invite = invite,
                        onAccept = { viewModel.accept(invite.id) },
                        onDecline = { viewModel.decline(invite.id) },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            TextButton(onClick = onCreateOrg) {
                Text("Skapa en egen organisation",
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                    letterSpacing = 1.6.sp, color = FaltetForest)
            }
            TextButton(onClick = onJoinByName) {
                Text("Gå med via namn istället",
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                    letterSpacing = 1.6.sp, color = FaltetForest)
            }
            TextButton(onClick = onSignedOut) {
                Text("LOGGA UT",
                    fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                    letterSpacing = 1.4.sp, color = FaltetForest)
            }
        }
    }
}

@Composable
private fun InviteCard(
    invite: OrgInviteResponse,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FaltetInk.copy(alpha = 0.04f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(invite.orgName, fontFamily = FaltetDisplay, fontSize = 20.sp, color = FaltetInk)
        Spacer(Modifier.height(4.dp))
        Text("Inbjuden av ${invite.invitedByName}", fontSize = 12.sp, color = FaltetForest)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onAccept,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = FaltetInk, contentColor = FaltetCream),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("GÅ MED", fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onDecline,
            shape = RectangleShape,
            modifier = Modifier.fillMaxWidth().height(40.dp),
        ) {
            Text("Avvisa", fontSize = 12.sp, color = FaltetAccent)
        }
    }
}
