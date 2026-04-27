package app.verdant.android.ui.auth

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.BuildConfig
import app.verdant.android.data.repository.AuthRepository
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AuthScreen"

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val needsOrg: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun setError(message: String) {
        _uiState.value = AuthUiState(error = message)
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                Log.d(TAG, "Sending ID token to backend...")
                val auth = authRepository.signIn(idToken)
                Log.d(TAG, "Backend auth successful; orgs=${auth.user.organizations.size}")
                _uiState.value = if (auth.user.organizations.isEmpty()) {
                    AuthUiState(needsOrg = true)
                } else {
                    AuthUiState(success = true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Backend auth failed: ${e.javaClass.simpleName}: ${e.message}", e)
                _uiState.value = AuthUiState(error = e.message ?: "Sign in failed")
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onNeedsOrg: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.success) {
        if (uiState.success) onAuthSuccess()
    }
    LaunchedEffect(uiState.needsOrg) {
        if (uiState.needsOrg) onNeedsOrg()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FaltetCream),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = "VERDANT",
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 48.sp,
                color = FaltetInk,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Din trädgård, planerad.",
                fontSize = 14.sp,
                color = FaltetForest,
            )
            Spacer(Modifier.height(48.dp))

            Button(
                onClick = {
                    Log.d(TAG, "Sign in button clicked")
                    val credentialManager = CredentialManager.create(context)
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    Log.d(TAG, "Launching credential request...")

                    (context as? Activity)?.let { activity ->
                        viewModel.viewModelScope.launch {
                            try {
                                val result = credentialManager.getCredential(activity, request)
                                Log.d(TAG, "Got credential, type=${result.credential.type}")
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                Log.d(TAG, "Google ID token obtained, sending to backend...")
                                viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                                Log.e(TAG, "No credentials available", e)
                                viewModel.setError("No Google account found. Check that you're signed into a Google account in Settings.")
                            } catch (e: Exception) {
                                Log.e(TAG, "Credential request failed: ${e.javaClass.simpleName}: ${e.message}", e)
                                viewModel.setError(e.message ?: "Sign in failed")
                            }
                        }
                    }
                },
                enabled = !uiState.isLoading,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FaltetInk,
                    contentColor = FaltetCream,
                    disabledContainerColor = FaltetInk.copy(alpha = 0.4f),
                    disabledContentColor = FaltetCream,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = FaltetCream,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = "LOGGA IN MED GOOGLE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                    )
                }
            }

            uiState.error?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = msg,
                    color = FaltetClay,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun AuthScreenPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FaltetCream),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = "VERDANT",
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 48.sp,
                color = FaltetInk,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Din trädgård, planerad.",
                fontSize = 14.sp,
                color = FaltetForest,
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = {},
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FaltetInk,
                    contentColor = FaltetCream,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    text = "LOGGA IN MED GOOGLE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}
