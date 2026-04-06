package app.verdant.android.voice

import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.verdant.android.R
import java.util.Locale

@Composable
fun VoiceCommandFAB(
    onCommand: (VoiceCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isListening by remember { mutableStateOf(false) }

    // Use the activity result API for speech recognition
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val text = matches?.firstOrNull()
        if (text != null) {
            val command = VoiceCommandParser.parse(text)
            onCommand(command)
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say: Verdant I sowed 50 dahlias...")
            }
            isListening = true
            speechLauncher.launch(intent)
        }
    }

    val contentDescription = stringResource(R.string.voice_listening)

    FloatingActionButton(
        onClick = {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        modifier = modifier,
        containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
