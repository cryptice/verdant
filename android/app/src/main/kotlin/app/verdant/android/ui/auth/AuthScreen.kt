package app.verdant.android.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.BuildConfig
import app.verdant.android.R
import app.verdant.android.data.repository.AuthRepository
import app.verdant.android.data.model.AuthResponse
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AuthScreen"

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val errorResource: Int? = null,
    val success: Boolean = false,
    val needsOrg: Boolean = false,
)

@HiltViewModel
class AuthViewModel internal constructor(
    private val authenticate: suspend (String) -> AuthResponse,
) : ViewModel() {
    @Inject constructor(authRepository: AuthRepository) : this(authRepository::signIn)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun setError(message: String) {
        _uiState.value = AuthUiState(error = message)
    }

    // Own both stages in the ViewModel: opening Google's UI can recreate the
    // Activity, but must not cancel the token exchange or lose its result.
    fun signInWithGoogle(requestIdToken: suspend () -> String) {
        if (_uiState.value.isLoading) return
        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            var receivedGoogleToken = false
            try {
                Log.d(TAG, "Starting Google button sign-in")
                val idToken = requestIdToken()
                receivedGoogleToken = true
                Log.d(TAG, "Sending ID token to backend...")
                val auth = authenticate(idToken)
                Log.d(TAG, "Backend auth successful; orgs=${auth.user.organizations.size}")
                _uiState.value = if (auth.user.organizations.isEmpty()) {
                    AuthUiState(needsOrg = true)
                } else {
                    AuthUiState(success = true)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "Google credential request cancelled")
                _uiState.value = AuthUiState()
            } catch (e: CancellationException) {
                Log.d(TAG, "Sign-in operation cancelled")
                _uiState.value = AuthUiState()
                throw e
            } catch (e: NoCredentialException) {
                Log.w(TAG, "Google button flow returned no credential", e)
                _uiState.value = AuthUiState(errorResource = R.string.google_sign_in_no_credential)
            } catch (e: Exception) {
                Log.e(TAG, "Sign-in failed: ${e.javaClass.simpleName}: ${e.message}", e)
                _uiState.value = if (receivedGoogleToken) {
                    AuthUiState(error = e.message ?: "Sign in failed")
                } else {
                    AuthUiState(errorResource = R.string.google_sign_in_failed)
                }
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onNeedsOnboarding: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.success) {
        if (uiState.success) onAuthSuccess()
    }
    LaunchedEffect(uiState.needsOrg) {
        if (uiState.needsOrg) onNeedsOnboarding()
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
                    val activity = context.findActivity()
                    when {
                        activity == null -> viewModel.setError(context.getString(R.string.google_sign_in_unavailable))
                        BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank() -> viewModel.setError(context.getString(R.string.google_sign_in_not_configured))
                        else -> {
                            val credentialContext = MutableContextWrapper(activity)
                            val credentialManager = CredentialManager.create(activity.applicationContext)
                            viewModel.signInWithGoogle {
                                val request = googleButtonSignInRequest(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                val result = credentialManager.getCredential(credentialContext, request)
                                val credential = result.credential
                                check(credential is CustomCredential &&
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    "Unexpected Google credential type"
                                }
                                GoogleIdTokenCredential.createFrom(credential.data).idToken
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
                        text = stringResource(R.string.sign_in_with_google),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                    )
                }
            }

            val errorMessage = uiState.errorResource?.let { stringResource(it) } ?: uiState.error
            errorMessage?.let { msg ->
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
                    text = stringResource(R.string.sign_in_with_google),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
