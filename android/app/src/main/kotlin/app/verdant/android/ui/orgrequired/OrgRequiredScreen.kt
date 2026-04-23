package app.verdant.android.ui.orgrequired

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import app.verdant.android.data.repository.AuthRepository
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine40
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "OrgRequiredScreen"
private const val VERDANT_URL = "https://verdantplanner.com"

@HiltViewModel
class OrgRequiredViewModel @Inject constructor(
    val authRepository: AuthRepository,
) : ViewModel()

@Composable
fun OrgRequiredScreen(
    onOrgReady: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: OrgRequiredViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(FaltetCream),
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
            Spacer(Modifier.height(32.dp))

            Text(
                text = "Ingen organisation",
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 22.sp,
                color = FaltetInk,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Skapa en organisation på verdantplanner.com för att börja använda appen.",
                fontSize = 14.sp,
                color = FaltetForest,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(VERDANT_URL))
                    context.startActivity(intent)
                },
                enabled = !checking,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FaltetInk,
                    contentColor = FaltetCream,
                    disabledContainerColor = FaltetInk.copy(alpha = 0.4f),
                    disabledContentColor = FaltetCream,
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(
                    text = "ÖPPNA VERDANTPLANNER.COM",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    message = null
                    scope.launch {
                        checking = true
                        try {
                            val user = viewModel.authRepository.refreshUser()
                            if (user.organizations.isNotEmpty()) {
                                onOrgReady()
                            } else {
                                message = "Ingen organisation hittades. Försök igen när du har skapat en."
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "refresh failed: ${e.javaClass.simpleName}: ${e.message}")
                            message = "Kunde inte kontrollera. Kontrollera anslutningen."
                        } finally {
                            checking = false
                        }
                    }
                },
                enabled = !checking,
                shape = RectangleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, FaltetInkLine40),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FaltetAccent),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (checking) {
                    CircularProgressIndicator(
                        color = FaltetAccent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = "JAG HAR SKAPAT EN, FÖRSÖK IGEN",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                    )
                }
            }

            message?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = msg,
                    color = FaltetClay,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(32.dp))

            TextButton(
                onClick = {
                    scope.launch {
                        viewModel.authRepository.signOut()
                        onSignedOut()
                    }
                },
                enabled = !checking,
            ) {
                Text(
                    text = "LOGGA UT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    color = FaltetForest,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun OrgRequiredScreenPreview() {
    Box(
        modifier = Modifier.fillMaxSize().background(FaltetCream),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text("VERDANT", fontFamily = FaltetDisplay, fontStyle = FontStyle.Italic, fontSize = 48.sp, color = FaltetInk, letterSpacing = 2.sp)
            Spacer(Modifier.height(32.dp))
            Text("Ingen organisation", fontFamily = FaltetDisplay, fontStyle = FontStyle.Italic, fontSize = 22.sp, color = FaltetInk)
            Spacer(Modifier.height(12.dp))
            Text("Skapa en organisation på verdantplanner.com för att börja använda appen.", fontSize = 14.sp, color = FaltetForest, textAlign = TextAlign.Center)
        }
    }
}
