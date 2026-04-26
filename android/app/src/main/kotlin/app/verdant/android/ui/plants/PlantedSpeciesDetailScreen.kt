package app.verdant.android.ui.plants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
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

    fun deleteEvent(
        eventType: String,
        eventDate: String,
        count: Int,
        currentStatus: String,
        trayOnly: Boolean = true,
    ) {
        viewModelScope.launch {
            try {
                repo.deleteSpeciesEvent(
                    speciesId,
                    app.verdant.android.data.model.DeleteSpeciesEventRequest(
                        eventType = eventType,
                        eventDate = eventDate,
                        count = count,
                        currentStatus = currentStatus,
                        trayOnly = trayOnly,
                    ),
                )
                load()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete event", e)
            }
        }
    }

    fun updateEventDate(
        eventType: String,
        oldDate: String,
        newDate: java.time.LocalDate,
        currentStatus: String,
        trayOnly: Boolean = true,
    ) {
        viewModelScope.launch {
            try {
                repo.updateSpeciesEventDate(
                    speciesId,
                    app.verdant.android.data.model.UpdateSpeciesEventDateRequest(
                        eventType = eventType,
                        oldDate = oldDate,
                        newDate = newDate.toString(),
                        currentStatus = currentStatus,
                        trayOnly = trayOnly,
                    ),
                )
                load()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update event date", e)
            }
        }
    }

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
    val expandedTrayStatuses = remember { mutableStateOf<Set<String>>(emptySet()) }
    // (eventType, oldDate, currentStatus, currentCount)
    data class EventTarget(val type: String, val date: String, val status: String, val current: Int)
    var eventChooserTarget by remember { mutableStateOf<EventTarget?>(null) }
    var editDateTarget by remember { mutableStateOf<EventTarget?>(null) }
    var deleteEventTarget by remember { mutableStateOf<EventTarget?>(null) }
    androidx.compose.runtime.LaunchedEffect(uiState.locations) {
        // Default-expand every tray row so the events show without an extra tap.
        expandedTrayStatuses.value = uiState.locations.filter { it.bedId == null }
            .map { it.status }.toSet()
    }

    eventChooserTarget?.let { t ->
        EventChooserDialog(
            eventLabel = eventLabelSv(t.type),
            date = t.date,
            count = t.current,
            onDismiss = { eventChooserTarget = null },
            onEditDate = {
                eventChooserTarget = null
                editDateTarget = t
            },
            onDelete = {
                eventChooserTarget = null
                deleteEventTarget = t
            },
        )
    }
    editDateTarget?.let { t ->
        EditEventDateDialog(
            initialDate = runCatching { java.time.LocalDate.parse(t.date) }.getOrNull()
                ?: java.time.LocalDate.now(),
            onDismiss = { editDateTarget = null },
            onConfirm = { newDate ->
                editDateTarget = null
                if (newDate.toString() != t.date) {
                    viewModel.updateEventDate(t.type, t.date, newDate, t.status)
                }
            },
        )
    }
    deleteEventTarget?.let { t ->
        DeleteEventDialog(
            eventLabel = eventLabelSv(t.type),
            date = t.date,
            maxCount = t.current,
            onDismiss = { deleteEventTarget = null },
            onConfirm = { count ->
                deleteEventTarget = null
                if (count > 0) viewModel.deleteEvent(t.type, t.date, count, t.status)
            },
        )
    }

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
                            item(key = "card_${status}") {
                                StatusSectionCard(
                                    status = status,
                                    locations = locations,
                                    expandedTrayStatuses = expandedTrayStatuses.value,
                                    trayEvents = uiState.trayEvents,
                                    onTrayToggle = { s ->
                                        expandedTrayStatuses.value =
                                            if (s in expandedTrayStatuses.value) expandedTrayStatuses.value - s
                                            else expandedTrayStatuses.value + s
                                    },
                                    onAct = { loc ->
                                        selectedSubItem = loc
                                        actionCount = loc.count.toString()
                                    },
                                    onEventTap = { eventType, oldDate, currentStatus, current ->
                                        eventChooserTarget = EventTarget(eventType, oldDate, currentStatus, current)
                                    },
                                )
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
    allEvents: List<app.verdant.android.data.model.SpeciesEventSummaryEntry>,
    currentStatus: String,
    onAct: () -> Unit,
    onEventTap: (eventType: String, oldDate: String, currentStatus: String, currentCount: Int) -> Unit,
) {
    // Show only the events whose plants are currently in this row's status,
    // collapsed across (eventType, eventDate). For each event, also report
    // the species-wide total (across all current statuses) so the user can
    // see e.g. "5 (10) Sådda" — 5 of the 10 originally sown are still
    // SEEDED, the others moved on.
    data class Row(val type: String, val date: String, val current: Int, val total: Int)
    val rows = allEvents
        .groupBy { it.eventType to it.eventDate }
        .map { (key, entries) ->
            val (type, date) = key
            Row(
                type = type,
                date = date,
                current = entries.filter { it.currentStatus == currentStatus }.sumOf { it.count },
                total = entries.sumOf { it.count },
            )
        }
        .filter { it.current > 0 }
        .sortedWith(compareByDescending<Row> { it.date }.thenByDescending { it.type })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 18.dp, top = 6.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        var showAll by remember(currentStatus, rows.size) { mutableStateOf(false) }
        val visibleRows = if (rows.size <= 3 || showAll) rows else rows.take(3)

        if (rows.isEmpty()) {
            Text(
                text = "Inga händelser registrerade.",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = FaltetForest,
            )
        } else {
            visibleRows.forEachIndexed { index, e ->
                val isLatest = index == 0
                val countLabel = if (e.current < e.total) "${e.current} (${e.total}) st"
                    else "${e.current} st"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEventTap(e.type, e.date, currentStatus, e.current) }
                        .padding(vertical = 2.dp),
                ) {
                    Text(
                        text = countLabel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = if (isLatest) 13.sp else 11.sp,
                        color = if (isLatest) FaltetAccent else FaltetInk,
                        fontWeight = if (isLatest) androidx.compose.ui.text.font.FontWeight.SemiBold
                            else androidx.compose.ui.text.font.FontWeight.Normal,
                        modifier = Modifier.width(80.dp),
                    )
                    Text(
                        text = eventLabelSv(e.type),
                        fontFamily = FaltetDisplay,
                        fontStyle = FontStyle.Italic,
                        fontSize = if (isLatest) 17.sp else 14.sp,
                        color = FaltetInk,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = e.date,
                        fontFamily = FontFamily.Monospace,
                        fontSize = if (isLatest) 11.sp else 10.sp,
                        letterSpacing = 1.2.sp,
                        color = if (isLatest) FaltetAccent else FaltetForest,
                    )
                }
            }
            if (rows.size > 3) {
                TextButton(
                    onClick = { showAll = !showAll },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = if (showAll) "Visa färre" else "Visa ${rows.size - 3} till",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        color = FaltetAccent,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = onAct,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FaltetAccent,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Åtgärd",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                )
            }
        }
    }
}

@Composable
private fun StatusSectionCard(
    status: String,
    locations: List<PlantLocationGroup>,
    expandedTrayStatuses: Set<String>,
    trayEvents: List<app.verdant.android.data.model.SpeciesEventSummaryEntry>,
    onTrayToggle: (String) -> Unit,
    onAct: (PlantLocationGroup) -> Unit,
    onEventTap: (eventType: String, oldDate: String, currentStatus: String, currentCount: Int) -> Unit,
) {
    val totalCount = locations.sumOf { it.count }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .border(
                1.dp,
                FaltetInk,
                androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            )
            .background(
                app.verdant.android.ui.theme.FaltetCream,
                androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            ),
    ) {
        // Card header — status name + dot + total count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .drawBehind { drawCircle(statusColor(status)) },
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = statusLabelPluralSv(status).uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = FaltetForest,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = totalCount.toString(),
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
        // Locations list inside the card
        locations.forEach { loc ->
            val isTray = loc.bedId == null
            val locationLabel = if (isTray) "Bricka" else
                listOfNotNull(loc.gardenName, loc.bedName).joinToString(" / ")
            FaltetListRow(
                title = locationLabel.ifBlank { "—" },
                meta = loc.year.toString(),
                stat = if (locations.size > 1) {
                    {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = loc.count.toString(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
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
                    if (isTray) onTrayToggle(loc.status)
                    else onAct(loc)
                },
            )
            if (isTray && loc.status in expandedTrayStatuses) {
                TrayEventsExpansion(
                    allEvents = trayEvents,
                    currentStatus = loc.status,
                    onAct = { onAct(loc) },
                    onEventTap = onEventTap,
                )
            }
        }
    }
}

private fun eventLabelSv(eventType: String): String = when (eventType) {
    "SEEDED" -> "Sådda"
    "POTTED_UP" -> "Omskolade"
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
    "WATERED" -> "Vattnade"
    "MOVED" -> "Flyttade"
    else -> eventType
}

private fun statusLabelPluralSv(status: String): String = when (status) {
    "SEEDED" -> "Sådda"
    "POTTED_UP" -> "Omskolade"
    "PLANTED_OUT", "GROWING" -> "Utplanterade"
    "HARVESTED" -> "Skördade"
    "RECOVERED" -> "Återhämtade"
    "REMOVED" -> "Borttagna"
    else -> status
}

private fun statusLabelSv(status: String): String = when (status) {
    "SEEDED" -> "Sådd"
    "POTTED_UP" -> "Omskolad"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEventDateDialog(
    initialDate: java.time.LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (java.time.LocalDate) -> Unit,
) {
    val state = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant().toEpochMilli(),
    )
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    val picked = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneOffset.UTC)
                        .toLocalDate()
                    onConfirm(picked)
                } else {
                    onDismiss()
                }
            }) { Text("Spara", color = FaltetAccent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    ) {
        androidx.compose.material3.DatePicker(state = state)
    }
}

@Composable
private fun EventChooserDialog(
    eventLabel: String,
    date: String,
    count: Int,
    onDismiss: () -> Unit,
    onEditDate: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$eventLabel · $date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "$count plantor",
                    fontSize = 12.sp,
                    color = FaltetForest,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onEditDate) {
                    Text("Ändra datum", color = FaltetAccent)
                }
                TextButton(onClick = onDelete) {
                    Text("Ta bort händelse…", color = FaltetAccent)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

@Composable
private fun DeleteEventDialog(
    eventLabel: String,
    date: String,
    maxCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (count: Int) -> Unit,
) {
    var countText by remember { mutableStateOf(maxCount.toString()) }
    val parsed = countText.toIntOrNull()
    val valid = parsed != null && parsed in 1..maxCount
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ta bort händelse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "$eventLabel · $date",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = FaltetForest,
                )
                Text(
                    text = "Antal plantor (max $maxCount):",
                    fontSize = 13.sp,
                    color = FaltetInk,
                )
                androidx.compose.material3.OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    ),
                )
                Text(
                    text = "Plantor som blir kvar utan händelser markeras som borttagna.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    color = FaltetForest,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (valid) onConfirm(parsed!!) },
                enabled = valid,
            ) { Text("Ta bort", color = if (valid) FaltetAccent else FaltetForest) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}
