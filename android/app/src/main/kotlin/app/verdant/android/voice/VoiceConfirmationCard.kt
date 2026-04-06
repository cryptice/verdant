package app.verdant.android.voice

import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.verdant.android.R
import java.util.Locale

@Composable
fun VoiceConfirmationCard(
    command: VoiceCommand,
    speciesName: String?,
    supplyName: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // TTS readback
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val t = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
        tts = t
        onDispose { t.shutdown() }
    }

    // Build display text and speak it
    val displayText = when (command) {
        is VoiceCommand.PlantActivity -> {
            val actionLabel = when (command.action) {
                "SOW" -> context.getString(R.string.voice_action_sow)
                "SOAK" -> context.getString(R.string.voice_action_soak)
                "POT_UP" -> context.getString(R.string.voice_action_pot_up)
                "PLANT" -> context.getString(R.string.voice_action_plant)
                "HARVEST" -> context.getString(R.string.voice_action_harvest)
                else -> command.action
            }
            "$actionLabel ${command.quantity} x ${speciesName ?: command.speciesQuery}"
        }
        is VoiceCommand.SupplyUsage -> {
            "${context.getString(R.string.voice_use)} ${command.quantity} ${supplyName ?: command.supplyQuery}"
        }
        is VoiceCommand.Unparseable -> context.getString(R.string.voice_not_understood)
    }

    // Speak on first composition
    LaunchedEffect(displayText) {
        tts?.speak(displayText, TextToSpeech.QUEUE_FLUSH, null, "voice_confirm")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(20.dp))

            if (command is VoiceCommand.Unparseable) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.voice_try_again))
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = {
                            playTone(ToneGenerator.TONE_PROP_NACK)
                            onCancel()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.voice_cancel))
                    }

                    Button(
                        onClick = {
                            playTone(ToneGenerator.TONE_PROP_ACK)
                            onConfirm()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.voice_confirm))
                    }
                }
            }
        }
    }
}

private fun playTone(toneType: Int) {
    try {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        toneGen.startTone(toneType, 200)
    } catch (_: Exception) { }
}
