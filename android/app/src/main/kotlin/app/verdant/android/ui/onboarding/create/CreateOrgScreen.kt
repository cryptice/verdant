package app.verdant.android.ui.onboarding.create

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
import javax.inject.Inject

data class CreateOrgState(
    val name: String = "",
    val emoji: String = "🌱",
    val isSubmitting: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CreateOrgViewModel @Inject constructor(
    private val orgRepository: OrgRepository,
    private val userRefresher: UserRefresher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateOrgState())
    val uiState = _uiState.asStateFlow()

    fun setName(v: String) { _uiState.value = _uiState.value.copy(name = v) }
    fun setEmoji(v: String) { _uiState.value = _uiState.value.copy(emoji = v.take(4)) }

    fun submit() {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                orgRepository.create(name, _uiState.value.emoji.ifBlank { null })
                userRefresher.refreshUser()
                _uiState.value = _uiState.value.copy(isSubmitting = false, created = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }
}

@Composable
fun CreateOrgScreen(
    onCreated: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateOrgViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.created) { if (state.created) onCreated() }

    Box(
        modifier = Modifier.fillMaxSize().background(FaltetCream),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text("Skapa organisation",
                fontFamily = FaltetDisplay, fontStyle = FontStyle.Italic,
                fontSize = 22.sp, color = FaltetInk, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = state.name, onValueChange = viewModel::setName,
                label = { Text("Namn") }, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.emoji, onValueChange = viewModel::setEmoji,
                label = { Text("Emoji") }, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
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
                    Text("SKAPA", fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 2.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack) {
                Text("AVBRYT", fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.4.sp, color = FaltetForest)
            }
        }
    }
}
