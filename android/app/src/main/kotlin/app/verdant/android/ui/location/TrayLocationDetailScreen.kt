package app.verdant.android.ui.location
import app.verdant.android.ui.faltet.BotanicalPlate
import app.verdant.android.data.repository.PlantRepository
import app.verdant.android.data.repository.TrayLocationRepository

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.MoveTrayLocationRequest
import app.verdant.android.data.model.TrayLocationResponse
import app.verdant.android.data.model.TraySummaryEntry
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TrayLocationDetailScreen"

data class TrayLocationDetailState(
    val isLoading: Boolean = true,
    val location: TrayLocationResponse? = null,
    val allLocations: List<TrayLocationResponse> = emptyList(),
    val entries: List<TraySummaryEntry> = emptyList(),
    val totalCount: Int = 0,
    val acting: Boolean = false,
    val error: String? = null,
    val info: String? = null,
)

@HiltViewModel
class TrayLocationDetailViewModel @Inject constructor(
    private val trayLocationRepository: TrayLocationRepository,
    private val plantRepository: PlantRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val locationId: Long = savedStateHandle.get<Long>("locationId")
        ?: error("locationId missing")

    private val _uiState = MutableStateFlow(TrayLocationDetailState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val all = trayLocationRepository.list()
                val loc = all.firstOrNull { it.id == locationId }
                val summary = plantRepository.traySummary().filter { it.trayLocationId == locationId }
                _uiState.value = TrayLocationDetailState(
                    isLoading = false,
                    location = loc,
                    allLocations = all,
                    entries = summary,
                    totalCount = summary.sumOf { it.count },
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load location detail", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun water() = bulk("Vattnade") { trayLocationRepository.water(locationId).plantsAffected }

    fun note(text: String) = bulk("Anteckning sparad") {
        trayLocationRepository.note(locationId, text).plantsAffected
    }

    fun move(targetId: Long?, count: Int, speciesId: Long?, status: String?) = bulk("Flyttade") {
        trayLocationRepository.move(
            locationId,
            MoveTrayLocationRequest(
                targetLocationId = targetId,
                count = count,
                speciesId = speciesId,
                status = status,
            ),
        ).plantsAffected
    }

    fun rename(newName: String) {
        viewModelScope.launch {
            try { trayLocationRepository.update(locationId, newName); refresh() }
            catch (e: Exception) { Log.e(TAG, "rename failed", e) }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try { trayLocationRepository.delete(locationId); onDeleted() }
            catch (e: Exception) { Log.e(TAG, "delete failed", e) }
        }
    }

    private fun bulk(verb: String, call: suspend () -> Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(acting = true, error = null, info = null)
            try {
                val n = call()
                _uiState.value = _uiState.value.copy(acting = false, info = "$verb · $n plantor")
                refresh()
            } catch (e: Exception) {
                Log.e(TAG, "bulk action failed", e)
                _uiState.value = _uiState.value.copy(acting = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrayLocationDetailScreen(
    onBack: () -> Unit,
    onSpeciesClick: (Long) -> Unit = {},
    onFertilize: (Long) -> Unit = {},
    onDeleted: () -> Unit = onBack,
    viewModel: TrayLocationDetailViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    var showWaterConfirm by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var moveMode by remember { mutableStateOf(false) }
    var partialMoveTarget by remember { mutableStateOf<TraySummaryEntry?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(ui.info) {
        if (ui.info != null) {
            kotlinx.coroutines.delay(1500)
        }
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = ui.location?.name ?: "Plats",
        mastheadRight = {
            if (ui.location != null) {
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Redigera plats",
                        tint = FaltetForest,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        watermark = BotanicalPlate.EmptyGarden,
) { padding ->
        when {
            ui.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            ui.error != null && ui.location == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            ui.location == null -> FaltetEmptyState(
                headline = "Plats saknas",
                subtitle = "Den här platsen finns inte längre.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${ui.totalCount} ST",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = FaltetForest,
                            modifier = Modifier.weight(1f),
                        )
                        ui.info?.let {
                            Text(
                                text = it,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = FaltetAccent,
                            )
                        }
                    }
                }
                item {
                    if (moveMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Tryck en rad för att flytta",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = FaltetAccent,
                                modifier = Modifier.weight(1f),
                            )
                            ActionButton(
                                label = "Avbryt",
                                enabled = !ui.acting,
                                onClick = { moveMode = false },
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ActionButton(
                                label = "Vattna alla",
                                enabled = !ui.acting && ui.totalCount > 0,
                                onClick = { showWaterConfirm = true },
                            )
                            ActionButton(
                                label = "Gödsla",
                                enabled = !ui.acting && ui.totalCount > 0,
                                onClick = { ui.location?.id?.let(onFertilize) },
                            )
                            ActionButton(
                                label = "Anteckna",
                                enabled = !ui.acting && ui.totalCount > 0,
                                onClick = { showNoteDialog = true },
                            )
                            ActionButton(
                                label = "Flytta",
                                enabled = !ui.acting && ui.totalCount > 0,
                                onClick = { moveMode = true },
                            )
                        }
                    }
                }
                item { FaltetSectionHeader(label = "Plantor") }
                if (ui.entries.isEmpty()) {
                    item {
                        Text(
                            text = "—",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = FaltetForest,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                } else {
                    items(ui.entries, key = { "${it.speciesId}_${it.status}" }) { entry ->
                        FaltetListRow(
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
                                        color = FaltetForest,
                                    )
                                }
                            },
                            onClick = {
                                if (moveMode) partialMoveTarget = entry
                                else entry.speciesId?.let(onSpeciesClick)
                            },
                        )
                    }
                }
            }
        }
    }

    if (showWaterConfirm) {
        ConfirmActionDialog(
            title = "Vattna alla?",
            text = "Vattnar ${ui.totalCount} plantor i ${ui.location?.name}.",
            confirmLabel = "Vattna",
            onConfirm = { viewModel.water(); showWaterConfirm = false },
            onDismiss = { showWaterConfirm = false },
        )
    }
    if (showNoteDialog) {
        NoteDialog(
            onDismiss = { showNoteDialog = false },
            onSubmit = { text -> viewModel.note(text); showNoteDialog = false },
        )
    }
    partialMoveTarget?.let { entry ->
        MoveDialog(
            allLocations = ui.allLocations.filter { it.id != ui.location?.id },
            sourceCount = entry.count,
            initialTitle = entry.variantName?.let { "${entry.speciesName} – $it" } ?: entry.speciesName,
            onDismiss = { partialMoveTarget = null },
            onConfirm = { targetId, count ->
                viewModel.move(targetId, count, entry.speciesId, entry.status)
                partialMoveTarget = null
                moveMode = false
            },
        )
    }

    if (showEditDialog && ui.location != null) {
        EditLocationDialog(
            initialName = ui.location!!.name,
            onDismiss = { showEditDialog = false },
            onRename = { newName ->
                viewModel.rename(newName)
                showEditDialog = false
            },
            onDelete = {
                showEditDialog = false
                showDeleteConfirm = true
            },
        )
    }

    if (showDeleteConfirm && ui.location != null) {
        val loc = ui.location!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Ta bort plats") },
            text = {
                Text(
                    if (loc.activePlantCount > 0)
                        "${loc.activePlantCount} plantor i ${loc.name} blir utan plats. Fortsätt?"
                    else
                        "Ta bort ${loc.name}?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(onDeleted)
                }) { Text("Ta bort", color = FaltetAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Avbryt", color = FaltetForest)
                }
            },
        )
    }
}

@Composable
private fun EditLocationDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val guard = app.verdant.android.ui.faltet.rememberUnsavedChangesGuard(
        isDirty = name.trim() != initialName && name.trim().isNotEmpty(),
    )
    guard.RenderConfirmDialog()
    AlertDialog(
        onDismissRequest = guard.requestDismiss(onDismiss),
        title = { Text("Redigera plats") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Namn") },
                    singleLine = true,
                )
                TextButton(onClick = onDelete) {
                    Text("Ta bort plats", color = FaltetAccent)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(name.trim()) },
                enabled = name.trim().isNotEmpty() && name.trim() != initialName,
            ) { Text("Spara", color = FaltetAccent) }
        },
        dismissButton = {
            TextButton(onClick = guard.requestDismiss(onDismiss)) { Text("Avbryt", color = FaltetForest) }
        },
    )
}

@Composable
private fun ActionButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    // Renders as an italic, underlined link in the accent colour so the
    // row of actions reads unambiguously as tappable text rather than as
    // small static labels.
    TextButton(onClick = onClick, enabled = enabled) {
        Text(
            text = label,
            color = FaltetAccent,
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.W500,
            fontSize = 15.sp,
            textDecoration = TextDecoration.Underline,
        )
    }
}

@Composable
private fun ConfirmActionDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = FaltetAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt", color = FaltetForest) }
        },
    )
}

@Composable
private fun NoteDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val guard = app.verdant.android.ui.faltet.rememberUnsavedChangesGuard(
        isDirty = text.trim().isNotEmpty(),
    )
    guard.RenderConfirmDialog()
    AlertDialog(
        onDismissRequest = guard.requestDismiss(onDismiss),
        title = { Text("Anteckna") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Anteckning") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(text.trim()) },
                enabled = text.trim().isNotEmpty(),
            ) { Text("Spara", color = FaltetAccent) }
        },
        dismissButton = {
            TextButton(onClick = guard.requestDismiss(onDismiss)) { Text("Avbryt", color = FaltetForest) }
        },
    )
}

@Composable
private fun MoveDialog(
    allLocations: List<TrayLocationResponse>,
    sourceCount: Int,
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (targetId: Long?, count: Int) -> Unit,
) {
    var target by remember { mutableStateOf<TrayLocationResponse?>(null) }
    var detach by remember { mutableStateOf(false) }
    var countText by remember { mutableStateOf(sourceCount.toString()) }
    val count = countText.toIntOrNull() ?: 0
    val canSubmit = count in 1..sourceCount && (detach || target != null)

    val guard = app.verdant.android.ui.faltet.rememberUnsavedChangesGuard(
        isDirty = (target != null || detach) || (countText != sourceCount.toString()),
    )
    guard.RenderConfirmDialog()
    AlertDialog(
        onDismissRequest = guard.requestDismiss(onDismiss),
        title = { Text("Flytta plantor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(initialTitle, fontSize = 12.sp, color = FaltetForest)
                FaltetDropdown(
                    label = "Flytta till",
                    options = allLocations,
                    selected = target,
                    onSelectedChange = { target = it; detach = false },
                    labelFor = { it.name },
                    searchable = false,
                    required = !detach,
                )
                TextButton(onClick = { detach = !detach; if (detach) target = null }) {
                    Text(
                        text = if (detach) "✓ Inget mål (utan plats)" else "Eller: ta bort plats",
                        color = FaltetAccent,
                        fontSize = 12.sp,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { v -> countText = v.filter { it.isDigit() } },
                        label = { Text("Antal (max $sourceCount)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { countText = sourceCount.toString() }) {
                        Text("Alla", color = FaltetAccent, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(target?.id, count) },
                enabled = canSubmit,
            ) { Text("Flytta", color = FaltetAccent) }
        },
        dismissButton = {
            TextButton(onClick = guard.requestDismiss(onDismiss)) { Text("Avbryt", color = FaltetForest) }
        },
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
