package app.verdant.android.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.UpdateUserRequest
import app.verdant.android.ui.theme.verdantTopAppBarColors
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

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = AccountState(isLoading = true)
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

    fun updateLanguage(language: String) {
        viewModelScope.launch {
            try {
                gardenRepository.updateMe(UpdateUserRequest(language = language))
                val user = gardenRepository.getMe()
                _uiState.value = AccountState(isLoading = false, user = user)
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
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
            title = { Text(stringResource(R.string.delete_account)) },
            text = { Text(stringResource(R.string.delete_account_confirm)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.deleteAccount() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.account)) }, colors = verdantTopAppBarColors()) }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> {
                app.verdant.android.ui.common.ConnectionErrorState(
                    message = stringResource(R.string.couldnt_load_account),
                    onRetry = { viewModel.load() },
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

                    Spacer(Modifier.height(32.dp))

                    // Language picker
                    Text(stringResource(R.string.language), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.user?.language == "en",
                            onClick = { viewModel.updateLanguage("en") },
                            label = { Text(stringResource(R.string.language_english)) }
                        )
                        FilterChip(
                            selected = uiState.user?.language != "en",
                            onClick = { viewModel.updateLanguage("sv") },
                            label = { Text(stringResource(R.string.language_swedish)) }
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    OutlinedButton(
                        onClick = { viewModel.signOut() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.sign_out))
                    }

                    Spacer(Modifier.height(16.dp))

                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.delete_account), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
