package app.verdant.android.ui.splash

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.faltet.BotanicalIllustration
import app.verdant.android.ui.faltet.BotanicalPlate
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetInk
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import app.verdant.android.BuildConfig
import app.verdant.android.data.TokenStore
import app.verdant.android.data.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val TAG = "SplashScreen"

@HiltViewModel
class SplashViewModel @Inject constructor(
    val tokenStore: TokenStore,
    val authRepository: AuthRepository
) : ViewModel()

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToOrgRequired: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Log.d(TAG, "Splash started, checking stored token...")
        val existingToken = viewModel.tokenStore.getToken()
        if (existingToken != null) {
            Log.d(TAG, "Stored token found, refreshing user + org…")
            try {
                val user = viewModel.authRepository.refreshUser()
                if (user.organizations.isEmpty()) {
                    Log.d(TAG, "User has no organizations, navigating to OrgRequired")
                    onNavigateToOrgRequired()
                } else {
                    Log.d(TAG, "User refresh succeeded, navigating to dashboard")
                    onNavigateToDashboard()
                }
            } catch (e: Exception) {
                Log.w(TAG, "User refresh failed (${e.javaClass.simpleName}); navigating to dashboard anyway")
                onNavigateToDashboard()
            }
            return@LaunchedEffect
        }
        Log.d(TAG, "No stored token, attempting silent Google sign-in...")

        try {
            val credentialManager = CredentialManager.create(context)
            Log.d(TAG, "CredentialManager created")

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            Log.d(TAG, "Requesting credential")

            val activity = context as Activity
            val result = credentialManager.getCredential(activity, request)
            Log.d(TAG, "Got credential response, type=${result.credential.type}")

            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            Log.d(TAG, "Google ID token obtained, calling backend auth...")

            val auth = viewModel.authRepository.signIn(credential.idToken)
            if (auth.user.organizations.isEmpty()) {
                Log.d(TAG, "Silent sign-in: user has no organizations, navigating to OrgRequired")
                onNavigateToOrgRequired()
            } else {
                Log.d(TAG, "Backend auth successful, navigating to dashboard")
                onNavigateToDashboard()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Silent sign-in failed: ${e.javaClass.simpleName}: ${e.message}")
            onNavigateToAuth()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FaltetCream),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BotanicalIllustration(
                plate = BotanicalPlate.Frontispiece,
                size = 220.dp,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Verdant",
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 44.sp,
                color = FaltetInk,
            )
        }
    }
}
