package app.verdant.android.ui.garden

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BedResponse
import app.verdant.android.data.model.GardenResponse
import app.verdant.android.data.model.UpdateGardenRequest
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "GardenDetail"

data class GardenDetailState(
    val isLoading: Boolean = true,
    val garden: GardenResponse? = null,
    val beds: List<BedResponse> = emptyList(),
    val trayPlants: List<app.verdant.android.data.model.TraySummaryEntry> = emptyList(),
    val error: String? = null,
    val deleted: Boolean = false
)

@HiltViewModel
class GardenDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gardenRepository: GardenRepository
) : ViewModel() {
    private val gardenId: Long = savedStateHandle.get<Long>("gardenId")!!
    private val _uiState = MutableStateFlow(GardenDetailState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = GardenDetailState(isLoading = true)
            // Retry up to 3 times with delay — garden may not be visible immediately after creation
            var lastError: Exception? = null
            for (attempt in 1..3) {
                try {
                    Log.d(TAG, "Loading garden $gardenId (attempt $attempt)")
                    val garden = gardenRepository.getGarden(gardenId)
                    Log.d(TAG, "Garden loaded: ${garden.name}")
                    val beds = gardenRepository.getBeds(gardenId).sortedBy { it.name.lowercase() }
                    Log.d(TAG, "Beds loaded: ${beds.size}")
                    val tray = runCatching { gardenRepository.getTraySummary() }.getOrDefault(emptyList())
                    _uiState.value = GardenDetailState(isLoading = false, garden = garden, beds = beds, trayPlants = tray)
                    return@launch
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load garden (attempt $attempt): ${e.message}")
                    lastError = e
                    if (attempt < 3) kotlinx.coroutines.delay(500L * attempt)
                }
            }
            _uiState.value = GardenDetailState(isLoading = false, error = lastError?.message)
        }
    }

    fun update(name: String, description: String?, emoji: String?) {
        viewModelScope.launch {
            try {
                gardenRepository.updateGarden(gardenId, UpdateGardenRequest(name = name, description = description, emoji = emoji))
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                gardenRepository.deleteGarden(gardenId)
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenDetailScreen(
    onBack: () -> Unit,
    onBedClick: (Long) -> Unit,
    onCreateBed: (Long) -> Unit,
    onTrayAction: (action: String, speciesId: Long) -> Unit = { _, _ -> },
    viewModel: GardenDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var trayActionTarget by remember { mutableStateOf<app.verdant.android.data.model.TraySummaryEntry?>(null) }

    trayActionTarget?.let { entry ->
        app.verdant.android.ui.dashboard.TrayActionDialog(
            entry = entry,
            onDismiss = { trayActionTarget = null },
            onAction = { action ->
                trayActionTarget = null
                entry.speciesId?.let { onTrayAction(action, it) }
            },
        )
    }

    // Refresh whenever this screen comes back to the foreground (e.g. after
    // popping back from CreateBed) so newly added beds show up.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }

    if (showEditDialog && uiState.garden != null) {
        EditGardenDialog(
            garden = uiState.garden!!,
            onDismiss = { showEditDialog = false },
            onSave = { name, description, emoji ->
                viewModel.update(name, description, emoji)
                showEditDialog = false
            },
        )
    }

    if (showDeleteDialog && uiState.garden != null) {
        val gardenName = uiState.garden!!.name
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Ta bort trädgård") },
            text = { Text("Vill du ta bort trädgården \"${gardenName}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete()
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
        mastheadCenter = uiState.garden?.name ?: "",
        mastheadRight = {
            if (uiState.garden != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Redigera", tint = FaltetAccent, modifier = Modifier.size(18.dp))
                    }
                    if (uiState.beds.isEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.DeleteOutline, "Ta bort", tint = FaltetClay, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        fab = {
            uiState.garden?.let { garden ->
                FaltetFab(onClick = { onCreateBed(garden.id) }, contentDescription = "Skapa bädd")
            }
        },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            uiState.garden == null -> FaltetEmptyState(
                headline = "Trädgården hittades inte",
                subtitle = "Trädgården kan ha tagits bort.",
                modifier = Modifier.padding(padding),
            )
            else -> {
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item { FaltetSectionHeader(label = "Bäddar") }
                    if (uiState.beds.isEmpty()) {
                        item { InlineEmpty("Inga bäddar ännu. Tryck + för att skapa.") }
                    } else {
                        items(uiState.beds, key = { it.id }) { bed ->
                            BedRow(bed = bed, onClick = { onBedClick(bed.id) })
                        }
                    }
                    if (uiState.trayPlants.isNotEmpty()) {
                        item { FaltetSectionHeader(label = "Plantor i brätten") }
                        items(uiState.trayPlants) { entry ->
                            app.verdant.android.ui.faltet.FaltetListRow(
                                title = entry.variantName?.let { "${entry.speciesName} – $it" } ?: entry.speciesName,
                                meta = trayStatusLabelSv(entry.status),
                                stat = {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = entry.count.toString(),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 16.sp,
                                            color = FaltetInk,
                                        )
                                        Text(
                                            text = " ST",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            letterSpacing = 1.2.sp,
                                            color = FaltetForest,
                                        )
                                    }
                                },
                                onClick = { trayActionTarget = entry },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EditGardenDialog(
    garden: GardenResponse,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String?, emoji: String?) -> Unit,
) {
    var editName by remember { mutableStateOf(garden.name) }
    var editDescription by remember { mutableStateOf(garden.description ?: "") }
    var editEmoji by remember { mutableStateOf(garden.emoji ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redigera trädgård") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(
                    label = "Namn",
                    value = editName,
                    onValueChange = { editName = it },
                    required = true,
                )
                Field(
                    label = "Beskrivning (valfri)",
                    value = editDescription,
                    onValueChange = { editDescription = it },
                )
                Field(
                    label = "Emoji",
                    value = editEmoji,
                    onValueChange = { editEmoji = it },
                    placeholder = "🌱",
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(editName, editDescription.ifBlank { null }, editEmoji.ifBlank { null })
                },
                enabled = editName.isNotBlank(),
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        },
    )
}

@Composable
private fun InlineEmpty(text: String) {
    Text(
        text = text,
        fontFamily = FaltetDisplay,
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        color = FaltetForest,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
    )
}

private fun trayStatusLabelSv(status: String): String = when (status) {
    "SEEDED" -> "Sådd"
    "POTTED_UP" -> "Omskolad"
    "PLANTED_OUT", "GROWING" -> "Växer"
    "HARVESTED" -> "Skördad"
    "RECOVERED" -> "Återhämtad"
    "REMOVED" -> "Borttagen"
    else -> status
}

@Composable
private fun BedRow(bed: BedResponse, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(
            text = bed.name,
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 18.sp,
            color = FaltetInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!bed.description.isNullOrBlank()) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = bed.description!!.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = FaltetForest,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun InlineEmptyPreview() {
    InlineEmpty("Inga bäddar ännu. Tryck + för att skapa.")
}
