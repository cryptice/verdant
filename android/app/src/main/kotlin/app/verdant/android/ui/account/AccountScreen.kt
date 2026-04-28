package app.verdant.android.ui.account
import app.verdant.android.data.repository.UserRepository

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.BuildConfig
import app.verdant.android.data.BackendStore
import app.verdant.android.data.model.UpdateUserRequest
import app.verdant.android.data.model.UserResponse
import app.verdant.android.data.repository.AuthRepository
import app.verdant.android.ui.faltet.FaltetAvatar
import app.verdant.android.ui.faltet.FaltetChipSelector
import app.verdant.android.ui.faltet.FaltetHero
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetClay
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountState(
    val isLoading: Boolean = true,
    val user: UserResponse? = null,
    val signedOut: Boolean = false,
    val error: String? = null,
    val useLocalBackend: Boolean = false,
)

private const val DEV_EMAIL = "erik@l2c.se"

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val backendStore: BackendStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            backendStore.useLocal.collect { value ->
                _uiState.value = _uiState.value.copy(useLocalBackend = value)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = AccountState(isLoading = true)
            try {
                val user = userRepository.me()
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
                userRepository.updateMe(UpdateUserRequest(language = language))
                val user = userRepository.me()
                _uiState.value = AccountState(isLoading = false, user = user)
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun setUseLocalBackend(value: Boolean) {
        viewModelScope.launch { backendStore.setUseLocal(value) }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                userRepository.deleteMe()
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(uiState.signedOut) {
        if (uiState.signedOut) onSignedOut()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Ta bort konto") },
            text = { Text("Vill du verkligen ta bort ditt konto? Detta kan inte ångras.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount()
                    showDeleteDialog = false
                }) { Text("Ta bort", color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Avbryt") }
            },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Konto",
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState()
            uiState.user != null -> {
                val user = uiState.user!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    item {
                        FaltetHero(
                            title = user.displayName,
                            subtitle = user.email,
                            leading = {
                                FaltetAvatar(
                                    url = user.avatarUrl,
                                    displayName = user.displayName,
                                    size = 88.dp,
                                )
                            },
                        )
                    }
                    item {
                        FaltetSectionHeader(label = "Språk")
                        FaltetChipSelector(
                            label = "",
                            options = listOf("sv", "en"),
                            selected = user.language,
                            onSelectedChange = { newLang -> newLang?.let { viewModel.updateLanguage(it) } },
                            labelFor = { if (it == "sv") "Svenska" else "English" },
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (user.email == DEV_EMAIL) {
                        item {
                            FaltetSectionHeader(label = "Utveckling")
                            FaltetListRow(
                                title = "Använd lokal backend",
                                meta = if (uiState.useLocalBackend) BuildConfig.LOCAL_API_BASE_URL else BuildConfig.API_BASE_URL,
                                actions = {
                                    Switch(
                                        checked = uiState.useLocalBackend,
                                        onCheckedChange = { viewModel.setUseLocalBackend(it) },
                                    )
                                },
                            )
                        }
                    }
                    item {
                        FaltetSectionHeader(label = "Konto")
                        FaltetListRow(
                            title = "Logga ut",
                            onClick = { viewModel.signOut() },
                        )
                        FaltetListRow(
                            title = "Ta bort konto",
                            onClick = { showDeleteDialog = true },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun AccountHeroPreview() {
    FaltetHero(
        title = "Erik Lindblad",
        subtitle = "erik@example.se",
        leading = {
            FaltetAvatar(url = null, displayName = "Erik Lindblad", size = 88.dp)
        },
    )
}
