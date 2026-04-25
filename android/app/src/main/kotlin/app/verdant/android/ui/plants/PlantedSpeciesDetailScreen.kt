package app.verdant.android.ui.plants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BatchEventRequest
import app.verdant.android.data.model.BedWithGardenResponse
import app.verdant.android.data.model.PlantLocationGroup
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.faltet.PhotoTone
import app.verdant.android.ui.theme.FaltetBerry
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine40
import app.verdant.android.ui.theme.FaltetMustard
import app.verdant.android.ui.theme.FaltetSage
import app.verdant.android.ui.theme.FaltetSky
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PlantedSpeciesDetail"

data class PlantedSpeciesDetailState(
    val isLoading: Boolean = true,
    val speciesName: String = "",
    val tasks: List<ScheduledTaskResponse> = emptyList(),
    val locations: List<PlantLocationGroup> = emptyList(),
    val beds: List<BedWithGardenResponse> = emptyList(),
    val trayEvents: List<app.verdant.android.data.model.SpeciesEventSummaryEntry> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class PlantedSpeciesDetailViewModel @Inject constructor(
    private val repo: GardenRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val speciesId: Long = savedStateHandle["speciesId"]!!
    private val _uiState = MutableStateFlow(PlantedSpeciesDetailState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun batchEvent(item: PlantLocationGroup, eventType: String, count: Int, targetBedId: Long? = null, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.batchEvent(
                    BatchEventRequest(
                        speciesId = speciesId,
                        bedId = item.bedId,
                        plantedDate = null,
                        status = item.status,
                        eventType = eventType,
                        count = count,
                        targetBedId = targetBedId,
                    )
                )
                load()
                onDone()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit batch event", e)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val locations = repo.getSpeciesLocations(speciesId)
                val beds = repo.getAllBeds()
                val tasks = repo.getTasks().filter {
                    it.speciesId == speciesId && it.status == "PENDING"
                }
                // Resolve species name (with variant) from species summary, falling back to task data
                val summary = repo.getSpeciesPlantSummary().find { it.speciesId == speciesId }
                val name = summary?.let {
                    it.variantName?.let { v -> "${it.speciesName} – $v" } ?: it.speciesName
                } ?: tasks.firstOrNull()?.speciesName ?: ""
                val trayEvents = runCatching { repo.getSpeciesEventSummary(speciesId, trayOnly = true) }
                    .onFailure { Log.e(TAG, "Failed to load species event summary", it) }
                    .getOrDefault(emptyList())
                _uiState.value = PlantedSpeciesDetailState(
                    isLoading = false,
                    speciesName = name,
                    tasks = tasks,
                    locations = locations,
                    beds = beds,
                    trayEvents = trayEvents,
                )
            } catch (e: Exception) {
                _uiState.value = PlantedSpeciesDetailState(isLoading = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantedSpeciesDetailScreen(
    onBack: () -> Unit,
    viewModel: PlantedSpeciesDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val statusOrder = listOf("SEEDED", "POTTED_UP", "PLANTED_OUT", "GROWING", "HARVESTED", "RECOVERED", "REMOVED")
    val byStatus: List<Pair<String, List<PlantLocationGroup>>> = remember(uiState.locations) {
        uiState.locations
            .groupBy { it.status }
            .toList()
            .sortedBy { (status, _) ->
                statusOrder.indexOf(status).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }
    }

    val aggregateCount = remember(uiState.locations) {
        uiState.locations.sumOf { it.count }
    }

    var selectedSubItem by remember { mutableStateOf<PlantLocationGroup?>(null) }
    var actionCount by remember { mutableStateOf("") }
    var actionSubmitting by remember { mutableStateOf(false) }
    var selectedTargetBedId by remember { mutableStateOf<Long?>(null) }
    var bedExpanded by remember { mutableStateOf(false) }
    var plantOutMode by remember { mutableStateOf(false) }
    var trayExpanded by remember { mutableStateOf(false) }

    val dismissModal: () -> Unit = {
        selectedSubItem = null
        actionCount = ""
        actionSubmitting = false
        selectedTargetBedId = null
        bedExpanded = false
        plantOutMode = false
    }

    // Dialog 2: action selection for a specific status group
    if (selectedSubItem != null && !plantOutMode) {
        val item = selectedSubItem!!
        val actions: List<Pair<String, String>> = buildList {
            when (item.status) {
                "SEEDED" -> {
                    if (item.bedId == null) add("POTTED_UP" to "Skola om")
                    add("PLANTED_OUT" to "Plantera ut")
                }
                "POTTED_UP" -> add("PLANTED_OUT" to "Plantera ut")
                "PLANTED_OUT", "GROWING" -> {
                    add("HARVESTED" to "Skörda")
                    add("RECOVERED" to "Återhämta")
                }
                "HARVESTED" -> add("RECOVERED" to "Återhämta")
            }
            add("REMOVED" to "Kassera")
        }
        AlertDialog(
            onDismissRequest = dismissModal,
            title = { Text("${statusLabelSv(item.status ?: "")} · ${item.count}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = actionCount,
                        onValueChange = { actionCount = it.filter { c -> c.isDigit() } },
                        label = { Text("Antal") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    val count = actionCount.toIntOrNull() ?: 0
                    actions.forEach { (eventType, label) ->
                        val isDestructive = eventType == "REMOVED"
                        Button(
                            onClick = {
                                if (eventType == "PLANTED_OUT") {
                                    plantOutMode = true
                                } else {
                                    actionSubmitting = true
                                    viewModel.batchEvent(item, eventType, count.coerceAtMost(item.count)) {
                                        dismissModal()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = count in 1..item.count && !actionSubmitting,
                            colors = if (isDestructive) ButtonDefaults.buttonColors(containerColor = FaltetAccent)
                                     else ButtonDefaults.buttonColors(),
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = dismissModal) { Text("Avbryt") }
            },
        )
    }

    // Dialog 3: bed picker for plant-out
    if (selectedSubItem != null && plantOutMode) {
        val item = selectedSubItem!!
        val count = actionCount.toIntOrNull() ?: 0
        AlertDialog(
            onDismissRequest = dismissModal,
            title = { Text("Plantera ut") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Välj bädd",
                        fontSize = 14.sp,
                        color = FaltetForest,
                    )
                    ExposedDropdownMenuBox(
                        expanded = bedExpanded,
                        onExpandedChange = { bedExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = uiState.beds.find { it.id == selectedTargetBedId }?.let { "${it.gardenName} - ${it.name}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Välj bädd") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(bedExpanded) },
                        )
                        ExposedDropdownMenu(
                            expanded = bedExpanded,
                            onDismissRequest = { bedExpanded = false },
                        ) {
                            uiState.beds.forEach { bed ->
                                DropdownMenuItem(
                                    text = { Text("${bed.gardenName} - ${bed.name}") },
                                    onClick = { selectedTargetBedId = bed.id; bedExpanded = false },
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            actionSubmitting = true
                            viewModel.batchEvent(item, "PLANTED_OUT", count.coerceAtMost(item.count), targetBedId = selectedTargetBedId) {
                                dismissModal()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedTargetBedId != null && count in 1..item.count && !actionSubmitting,
                    ) {
                        Text("Plantera ut")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { plantOutMode = false; selectedTargetBedId = null }) {
                    Text("Tillbaka")
                }
            },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = uiState.speciesName,
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.load() })
            }
            uiState.speciesName.isEmpty() && uiState.locations.isEmpty() -> FaltetEmptyState(
                headline = "Arten hittades inte",
                subtitle = "Arten kan ha tagits bort.",
                modifier = Modifier.padding(padding),
            )
            else -> {
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = uiState.speciesName,
                                fontFamily = FaltetDisplay,
                                fontStyle = FontStyle.Italic,
                                fontSize = 24.sp,
                                color = FaltetInk,
                            )
                            Text(
                                text = "${aggregateCount} plantor".uppercase(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                letterSpacing = 1.4.sp,
                                color = FaltetForest,
                            )
                        }
                    }

                    if (byStatus.isEmpty() || byStatus.all { it.second.isEmpty() }) {
                        item { FaltetSectionHeader(label = "Plantor") }
                        item { InlineEmpty("Inga plantor av denna art ännu.") }
                    } else {
                        byStatus.forEach { (status, locations) ->
                            if (locations.isEmpty()) return@forEach
                            item(key = "status_${status}") {
                                FaltetSectionHeader(label = statusLabelSv(status))
                            }
                            items(locations, key = { loc -> "loc_${status}_${loc.bedId}_${loc.year}" }) { loc ->
                                val isTray = loc.bedId == null
                                val locationLabel = if (isTray) "Bricka" else
                                    listOfNotNull(loc.gardenName, loc.bedName).joinToString(" / ")
                                FaltetListRow(
                                    leading = {
                                        Box(
                                            Modifier
                                                .size(10.dp)
                                                .drawBehind { drawCircle(statusColor(loc.status)) },
                                        )
                                    },
                                    title = locationLabel.ifBlank { "—" },
                                    meta = loc.year.toString(),
                                    stat = if (loc.count > 1) {
                                        {
                                            Row(verticalAlignment = Alignment.Bottom) {
                                                Text(
                                                    text = loc.count.toString(),
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
                                        }
                                    } else null,
                                    onClick = {
                                        if (isTray) {
                                            trayExpanded = !trayExpanded
                                        } else {
                                            selectedSubItem = loc
                                            actionCount = loc.count.toString()
                                        }
                                    },
                                )
                                if (isTray && trayExpanded) {
                                    TrayEventsExpansion(
                                        events = uiState.trayEvents,
                                        onAct = {
                                            selectedSubItem = loc
                                            actionCount = loc.count.toString()
                                        },
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TrayEventsExpansion(
    events: List<app.verdant.android.data.model.SpeciesEventSummaryEntry>,
    onAct: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 18.dp, top = 6.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (events.isEmpty()) {
            Text(
                text = "Inga händelser registrerade.",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = FaltetForest,
            )
        } else {
            events.forEach { e ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${e.count} st",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = FaltetInk,
                        modifier = Modifier.width(60.dp),
                    )
                    Text(
                        text = eventLabelSv(e.eventType),
                        fontFamily = FaltetDisplay,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        color = FaltetInk,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = e.eventDate,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        color = FaltetForest,
                    )
                }
            }
        }
        TextButton(onClick = onAct, modifier = Modifier.padding(top = 4.dp)) {
            Text("Åtgärd", color = FaltetAccent, fontSize = 12.sp)
        }
    }
}

private fun eventLabelSv(eventType: String): String = when (eventType) {
    "SEEDED" -> "Sådda"
    "POTTED_UP" -> "Skolade om"
    "PLANTED_OUT" -> "Utplanterade"
    "HARVESTED" -> "Skördade"
    "RECOVERED" -> "Återhämtade"
    "REMOVED" -> "Borttagna"
    "NOTE" -> "Notering"
    "BUDDING" -> "Knoppar"
    "FIRST_BLOOM" -> "Första blomman"
    "PEAK_BLOOM" -> "Toppblomning"
    "LAST_BLOOM" -> "Sista blomman"
    "LIFTED" -> "Uppgrävda"
    "DIVIDED" -> "Delade"
    "STORED" -> "Lagrade"
    "PINCHED" -> "Toppade"
    "DISBUDDED" -> "Knopprensade"
    "APPLIED_SUPPLY" -> "Gödslade"
    else -> eventType
}

private fun statusLabelSv(status: String): String = when (status) {
    "SEEDED" -> "Sådd"
    "POTTED_UP" -> "Krukad"
    "PLANTED_OUT", "GROWING" -> "Utplanterad"
    "HARVESTED" -> "Skördad"
    "RECOVERED" -> "Återhämtad"
    "REMOVED" -> "Borttagen"
    else -> status
}

private fun statusColor(status: String?): androidx.compose.ui.graphics.Color = when (status) {
    "SEEDED" -> FaltetMustard
    "POTTED_UP" -> FaltetSky
    "PLANTED_OUT", "GROWING" -> FaltetSage
    "HARVESTED" -> FaltetAccent
    "RECOVERED" -> FaltetBerry
    "REMOVED" -> FaltetInkLine40
    else -> FaltetForest
}

private fun speciesTone(categoryName: String?): PhotoTone {
    val n = categoryName?.lowercase() ?: ""
    return when {
        n.contains("grönsak") -> PhotoTone.Sage
        n.contains("snittblom") || n.contains("blom") -> PhotoTone.Blush
        n.contains("ört") -> PhotoTone.Butter
        n.contains("frukt") -> PhotoTone.Sage
        else -> PhotoTone.Sage
    }
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

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun StatusDotsPreview() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf("SEEDED", "POTTED_UP", "PLANTED_OUT", "HARVESTED", "REMOVED").forEach { status ->
            Box(
                Modifier
                    .padding(horizontal = 6.dp)
                    .size(10.dp)
                    .drawBehind { drawCircle(statusColor(status)) },
            )
        }
    }
}
