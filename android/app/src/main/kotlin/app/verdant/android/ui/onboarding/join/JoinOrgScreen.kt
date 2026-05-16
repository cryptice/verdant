package app.verdant.android.ui.onboarding.join

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
import app.verdant.android.data.repository.OrgRepository
import app.verdant.android.data.repository.UserRefresher
import app.verdant.android.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

private const val TAG = "JoinOrgScreen"

data class JoinOrgState(
    val name: String = "",
    val isSubmitting: Boolean = false,
    val requestSentOrgName: String? = null,
    val notFound: Boolean = false,
    val gainedOrg: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class JoinOrgViewModel @Inject constructor(
    private val orgRepository: OrgRepository,
    private val userRefresher: UserRefresher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(JoinOrgState())
    val uiState = _uiState.asStateFlow()

    fun setName(value: String) { _uiState.value = _uiState.value.copy(name = value, notFound = false) }

    fun submit() {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null, notFound = false)
            try {
                val org = orgRepository.lookup(name)
                if (org == null) {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, notFound = true)
                    return@launch
                }
                try {
                    orgRepository.requestJoin(org.id)
                } catch (e: HttpException) {
                    if (e.code() != 409) throw e
                    Log.w(TAG, "requestJoin returned 409 (already pending) — treating as success")
                }
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false, requestSentOrgName = org.name,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    fun checkStatus() {
        viewModelScope.launch {
            try {
                val user = userRefresher.refreshUser()
                if (user.organizations.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(gainedOrg = true)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@Composable
fun JoinOrgScreen(
    onJoined: () -> Unit,
    onCreateOrg: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: JoinOrgViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.gainedOrg) { if (state.gainedOrg) onJoined() }

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
                text = "Gå med i en organisation",
                fontFamily = FaltetDisplay, fontStyle = FontStyle.Italic,
                fontSize = 22.sp, color = FaltetInk, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            if (state.requestSentOrgName != null) {
                Text(
                    "Förfrågan skickad till \"${state.requestSentOrgName}\".\nVänta på att ägaren godkänner.",
                    fontSize = 14.sp, color = FaltetForest, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = viewModel::checkStatus,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = FaltetInk, contentColor = FaltetCream),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text("KONTROLLERA STATUS",
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 2.sp)
                }
            } else {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text("Organisationens namn") },
                    isError = state.notFound,
                    supportingText = if (state.notFound) { { Text("Hittades inte") } } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = viewModel::submit,
                    enabled = !state.isSubmitting && state.name.isNotBlank(),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = FaltetInk, contentColor = FaltetCream),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(color = FaltetCream, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text("SKICKA FÖRFRÅGAN",
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 2.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            TextButton(onClick = onCreateOrg) {
                Text("Skapa en egen organisation",
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
