package app.verdant.android.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.verdant.android.R
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.SupplyInventoryResponse
import kotlinx.coroutines.launch

@Composable
fun VoiceCommandOverlay(
    speciesList: List<SpeciesResponse>,
    supplyList: List<SupplyInventoryResponse>,
    onPlantActivity: suspend (action: String, quantity: Int, species: SpeciesResponse) -> Unit,
    onSupplyUsage: suspend (supply: SupplyInventoryResponse, quantity: Double) -> Unit,
    fabModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var pendingCommand by remember { mutableStateOf<VoiceCommand?>(null) }
    var resolvedSpecies by remember { mutableStateOf<SpeciesResponse?>(null) }
    var resolvedSupply by remember { mutableStateOf<SupplyInventoryResponse?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val successText = stringResource(R.string.voice_success)
    val errorText = stringResource(R.string.voice_error)
    val noSpeciesText = stringResource(R.string.voice_no_species_match)
    val noSupplyText = stringResource(R.string.voice_no_supply_match)

    Box(Modifier.fillMaxSize()) {
        // Main content
        content()

        // FAB at bottom-end
        VoiceCommandFAB(
            onCommand = { command ->
                when (command) {
                    is VoiceCommand.PlantActivity -> {
                        val match = SpeciesMatcher.findBestMatch(command.speciesQuery, speciesList).firstOrNull()
                        if (match != null) {
                            resolvedSpecies = match.species
                            resolvedSupply = null
                            pendingCommand = command
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(noSpeciesText) }
                        }
                    }
                    is VoiceCommand.SupplyUsage -> {
                        val match = SupplyMatcher.findBestMatch(command.supplyQuery, supplyList).firstOrNull()
                        if (match != null) {
                            resolvedSupply = match.supply
                            resolvedSpecies = null
                            pendingCommand = command
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(noSupplyText) }
                        }
                    }
                    is VoiceCommand.Unparseable -> {
                        resolvedSpecies = null
                        resolvedSupply = null
                        pendingCommand = command
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .then(fabModifier)
                .padding(16.dp),
        )

        // Confirmation overlay
        val cmd = pendingCommand
        if (cmd != null) {
            // Dimmed background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        // Dismiss on background tap
                        pendingCommand = null
                    },
            )

            // Confirmation card centered
            VoiceConfirmationCard(
                command = cmd,
                speciesName = resolvedSpecies?.let { s ->
                    val variant = s.variantName
                    if (variant != null) "${s.commonName} $variant" else s.commonName
                },
                supplyName = resolvedSupply?.supplyTypeName,
                onConfirm = {
                    val currentCommand = cmd
                    pendingCommand = null
                    scope.launch {
                        try {
                            when (currentCommand) {
                                is VoiceCommand.PlantActivity -> {
                                    val species = resolvedSpecies ?: return@launch
                                    onPlantActivity(currentCommand.action, currentCommand.quantity, species)
                                }
                                is VoiceCommand.SupplyUsage -> {
                                    val supply = resolvedSupply ?: return@launch
                                    onSupplyUsage(supply, currentCommand.quantity)
                                }
                                is VoiceCommand.Unparseable -> return@launch
                            }
                            snackbarHostState.showSnackbar(successText)
                        } catch (_: Exception) {
                            snackbarHostState.showSnackbar(errorText)
                        }
                    }
                },
                onCancel = { pendingCommand = null },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
