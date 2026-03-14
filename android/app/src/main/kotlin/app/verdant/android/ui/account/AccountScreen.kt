package app.verdant.android.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.UserResponse
import app.verdant.android.data.repository.AuthRepository
import app.verdant.android.data.repository.GardenRepository
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountState(
    val isLoading: Boolean = true,
    val user: UserResponse? = null,
    val signedOut: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val gardenRepository: GardenRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val user = gardenRepository.getMe()
                _uiState.value = AccountState(isLoading = false, user = user)
            } catch (e: Exception) {
                _uiState.value = AccountState(isLoading = false, error = e.message)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = _uiState.value.copy(signedOut = true)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                gardenRepository.deleteMe()
                authRepository.signOut()
                _uiState.value = _uiState.value.copy(signedOut = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onSignedOut: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.signedOut) {
        if (uiState.signedOut) onSignedOut()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("This will permanently delete your account and all your data. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.deleteAccount() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Account") }) }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> {
                app.verdant.android.ui.common.ConnectionErrorState(
                    message = "Couldn't load your account",
                    onRetry = { viewModel.signOut() },
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.user != null -> {
                val user = uiState.user!!
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(24.dp))
                    user.avatarUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(96.dp).clip(CircleShape)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    Text(user.displayName, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(user.email, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

                    Spacer(Modifier.height(48.dp))

                    OutlinedButton(
                        onClick = { viewModel.signOut() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sign Out")
                    }

                    Spacer(Modifier.height(16.dp))

                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Account", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
