package app.verdant.android.ui.plants
import app.verdant.android.ui.faltet.BotanicalPlate

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import app.verdant.android.data.model.PlantLocationGroup
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantedSpeciesDetailScreen(
    onBack: () -> Unit,
    viewModel: PlantedSpeciesDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loaded = uiState as? PlantedSpeciesDetailUiState.Loaded
    val locations = loaded?.locations ?: emptyList()

    val statusOrder = listOf("SEEDED", "POTTED_UP", "PLANTED_OUT", "GROWING", "HARVESTED", "RECOVERED", "REMOVED")
    val byStatus: List<Pair<String, List<PlantLocationGroup>>> = remember(locations) {
        locations
            .groupBy { it.status }
            .toList()
            .sortedBy { (status, _) ->
                statusOrder.indexOf(status).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }
    }

    val aggregateCount = remember(locations) { locations.sumOf { it.count } }

    var selectedSubItem by remember { mutableStateOf<PlantLocationGroup?>(null) }
    var actionCount by remember { mutableStateOf("") }
    var actionSubmitting by remember { mutableStateOf(false) }
    var selectedTargetBedId by remember { mutableStateOf<Long?>(null) }
    var bedExpanded by remember { mutableStateOf(false) }
    var plantOutMode by remember { mutableStateOf(false) }
    var moveMode by remember { mutableStateOf(false) }
    var selectedTargetTrayLocationId by remember { mutableStateOf<Long?>(null) }
    var detachLocation by remember { mutableStateOf(false) }
    var trayLocationExpanded by remember { mutableStateOf(false) }
    val expandedTrayStatuses = remember { mutableStateOf<Set<String>>(emptySet()) }
    // (eventType, oldDate, currentStatus, currentCount)
    data class EventTarget(val type: String, val date: String, val status: String, val current: Int)
    var eventChooserTarget by remember { mutableStateOf<EventTarget?>(null) }
    var editDateTarget by remember { mutableStateOf<EventTarget?>(null) }
    var deleteEventTarget by remember { mutableStateOf<EventTarget?>(null) }
    androidx.compose.runtime.LaunchedEffect(locations) {
        // Default-expand every tray row so the events show without an extra tap.
        expandedTrayStatuses.value = locations.filter { it.bedId == null }
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
        moveMode = false
        selectedTargetTrayLocationId = null
        detachLocation = false
        trayLocationExpanded = false
    }

    // Dialog 2: action selection for a specific status group
    if (selectedSubItem != null && !plantOutMode && !moveMode) {
        val item = selectedSubItem!!
        // Any tray plant can be moved to a tray location or detached, including
        // plants currently without a tray_location_id (they can be assigned).
        val canMove = item.bedId == null
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
            if (canMove) add("MOVE" to "Flytta")
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
                                when (eventType) {
                                    "PLANTED_OUT" -> plantOutMode = true
                                    "MOVE" -> moveMode = true
                                    else -> {
                                        actionSubmitting = true
                                        viewModel.batchEvent(item, eventType, count.coerceAtMost(item.count)) {
                                            dismissModal()
                                        }
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
                            value = (loaded?.beds ?: emptyList()).find { it.id == selectedTargetBedId }?.let { "${it.gardenName} - ${it.name}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Välj bädd") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable, true),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(bedExpanded) },
                        )
                        ExposedDropdownMenu(
                            expanded = bedExpanded,
                            onDismissRequest = { bedExpanded = false },
                        ) {
                            (loaded?.beds ?: emptyList()).forEach { bed ->
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

    // Dialog 4: tray location picker for "Flytta"
    if (selectedSubItem != null && moveMode) {
        val item = selectedSubItem!!
        val count = actionCount.toIntOrNull() ?: 0
        val targetOptions = (loaded?.trayLocations ?: emptyList()).filter { it.id != item.trayLocationId }
        val sourceLabel = item.trayLocationName ?: "Utan plats"
        val canSubmit = (detachLocation || selectedTargetTrayLocationId != null) &&
            count in 1..item.count && !actionSubmitting
        AlertDialog(
            onDismissRequest = dismissModal,
            title = { Text("Flytta från $sourceLabel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Välj plats",
                        fontSize = 14.sp,
                        color = FaltetForest,
                    )
                    ExposedDropdownMenuBox(
                        expanded = trayLocationExpanded,
                        onExpandedChange = { if (!detachLocation) trayLocationExpanded = it },
                    ) {
                        val displayText = when {
                            detachLocation -> "Utan plats"
                            else -> targetOptions.find { it.id == selectedTargetTrayLocationId }?.name ?: ""
                        }
                        OutlinedTextField(
                            value = displayText,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Välj plats") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable, true),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(trayLocationExpanded) },
                            enabled = !detachLocation,
                        )
                        ExposedDropdownMenu(
                            expanded = trayLocationExpanded,
                            onDismissRequest = { trayLocationExpanded = false },
                        ) {
                            targetOptions.forEach { loc ->
                                DropdownMenuItem(
                                    text = { Text(loc.name) },
                                    onClick = {
                                        selectedTargetTrayLocationId = loc.id
                                        detachLocation = false
                                        trayLocationExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    TextButton(onClick = {
                        detachLocation = !detachLocation
                        if (detachLocation) selectedTargetTrayLocationId = null
                    }) {
                        Text(
                            text = if (detachLocation) "✓ Utan plats" else "Eller: utan plats",
                            color = FaltetAccent,
                            fontSize = 12.sp,
                        )
                    }
                    Button(
                        onClick = {
                            actionSubmitting = true
                            viewModel.moveTrayPlants(
                                sourceLocationId = item.trayLocationId,
                                targetLocationId = if (detachLocation) null else selectedTargetTrayLocationId,
                                status = item.status,
                                count = count.coerceAtMost(item.count),
                            ) { dismissModal() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canSubmit,
                    ) {
                        Text("Flytta")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    moveMode = false
                    selectedTargetTrayLocationId = null
                    detachLocation = false
                    trayLocationExpanded = false
                }) {
                    Text("Tillbaka")
                }
            },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "",
        watermark = BotanicalPlate.EmptyGarden,
) { padding ->
        when (val state = uiState) {
            is PlantedSpeciesDetailUiState.Loading -> FaltetLoadingState(Modifier.padding(padding))
            is PlantedSpeciesDetailUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.load() })
            }
            is PlantedSpeciesDetailUiState.Loaded -> if (state.speciesName.isEmpty() && state.locations.isEmpty()) {
                FaltetEmptyState(
                    headline = "Arten hittades inte",
                    subtitle = "Arten kan ha tagits bort.",
                    modifier = Modifier.padding(padding),
                )
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = state.speciesName,
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
                        byStatus.forEach { (status, locsForStatus) ->
                            if (locsForStatus.isEmpty()) return@forEach
                            item(key = "card_${status}") {
                                StatusSectionCard(
                                    status = status,
                                    locations = locsForStatus,
                                    expandedTrayStatuses = expandedTrayStatuses.value,
                                    trayEvents = state.trayEvents,
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
    data class Row(
        val type: String,
        val date: String,
        val current: Int,
        val total: Int,
        val fromLoc: String?,
        val toLoc: String?,
        val notes: String?,
    )
    val rows = allEvents
        .groupBy { listOf(it.eventType, it.eventDate, it.fromLocationName, it.toLocationName, it.notes) }
        .map { (_, entries) ->
            val first = entries.first()
            Row(
                type = first.eventType,
                date = first.eventDate,
                current = entries.filter { it.currentStatus == currentStatus }.sumOf { it.count },
                total = entries.sumOf { it.count },
                fromLoc = first.fromLocationName,
                toLoc = first.toLocationName,
                notes = first.notes,
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
        val visibleRows = if (rows.size <= 1 || showAll) rows else rows.take(1)

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEventTap(e.type, e.date, currentStatus, e.current) }
                        .padding(vertical = 2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = countLabel,
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (isLatest) 13.sp else 11.sp,
                            color = if (isLatest) FaltetAccent else FaltetInk,
                            fontWeight = if (isLatest) androidx.compose.ui.text.font.FontWeight.SemiBold
                                else androidx.compose.ui.text.font.FontWeight.Normal,
                            modifier = Modifier.width(80.dp),
                        )
                        val displayLabel = when {
                            e.type == "MOVED" && e.fromLoc != null && e.toLoc != null ->
                                "Flyttade · ${e.fromLoc} → ${e.toLoc}"
                            e.type == "MOVED" && e.toLoc != null ->
                                "Flyttade · till ${e.toLoc}"
                            e.type == "MOVED" && e.fromLoc != null ->
                                "Flyttade · ut ur ${e.fromLoc}"
                            e.type == "NOTE" && !e.notes.isNullOrBlank() -> "“${e.notes}”"
                            else -> eventLabelSv(e.type)
                        }
                        Text(
                            text = displayLabel,
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
                    if (!e.notes.isNullOrBlank() && e.type != "NOTE") {
                        Text(
                            text = "“${e.notes}”",
                            fontFamily = FaltetDisplay,
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            color = FaltetForest,
                            modifier = Modifier.padding(start = 80.dp, top = 2.dp, end = 18.dp),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (rows.size > 1) {
                TextButton(
                    onClick = { showAll = !showAll },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = if (showAll) "Visa färre" else "Visa ${rows.size - 1} till",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        color = FaltetAccent,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
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
            val locationLabel = if (isTray) {
                loc.trayLocationName?.let { "Bricka · $it" } ?: "Bricka"
            } else listOfNotNull(loc.gardenName, loc.bedName).joinToString(" / ")
            FaltetListRow(
                title = locationLabel.ifBlank { "—" },
                meta = null,
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
