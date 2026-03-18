package app.verdant.android.ui.plants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.BatchEventRequest
import app.verdant.android.data.model.BedWithGardenResponse
import app.verdant.android.data.model.PlantLocationGroup
import app.verdant.android.ui.theme.verdantTopAppBarColors
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year
import android.util.Log
import javax.inject.Inject

private const val TAG = "PlantedSpeciesDetail"

data class PlantedSpeciesDetailState(
    val isLoading: Boolean = true,
    val speciesName: String = "",
    val tasks: List<ScheduledTaskResponse> = emptyList(),
    val locations: List<PlantLocationGroup> = emptyList(),
    val beds: List<BedWithGardenResponse> = emptyList(),
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
                // Resolve species name from tasks or locations summary
                val name = tasks.firstOrNull()?.speciesName
                    ?: repo.getSpeciesPlantSummary().find { it.speciesId == speciesId }?.speciesName
                    ?: ""
                _uiState.value = PlantedSpeciesDetailState(
                    isLoading = false,
                    speciesName = name,
                    tasks = tasks,
                    locations = locations,
                    beds = beds,
                )
            } catch (e: Exception) {
                _uiState.value = PlantedSpeciesDetailState(isLoading = false, error = e.message)
            }
        }
    }
}

private fun statusLabel(status: String): Int = when (status) {
    "SEEDED" -> R.string.event_seeded
    "POTTED_UP" -> R.string.event_potted_up
    "PLANTED_OUT", "GROWING" -> R.string.event_growing
    "HARVESTED" -> R.string.event_harvested
    "RECOVERED" -> R.string.event_recovered
    "REMOVED" -> R.string.event_removed
    else -> R.string.plant
}

private fun statusColor(status: String): Color = when (status) {
    "SEEDED" -> Color(0xFF8D6E63)
    "POTTED_UP" -> Color(0xFFFF8F00)
    "PLANTED_OUT", "GROWING" -> Color(0xFF43A047)
    "HARVESTED" -> Color(0xFF1565C0)
    "RECOVERED" -> Color(0xFF00897B)
    "REMOVED" -> Color(0xFF757575)
    else -> Color.Gray
}

private fun activityIcon(type: String): ImageVector = when (type) {
    "SOW" -> Icons.Default.Grain
    "POT_UP" -> Icons.Default.Inventory2
    "PLANT" -> Icons.Default.Park
    "HARVEST" -> Icons.Default.Agriculture
    "RECOVER" -> Icons.Default.Shield
    "DISCARD" -> Icons.Default.Delete
    else -> Icons.Default.Task
}

private fun activityLabel(type: String): Int = when (type) {
    "SOW" -> R.string.activity_sow
    "POT_UP" -> R.string.activity_pot_up
    "PLANT" -> R.string.activity_plant
    "HARVEST" -> R.string.activity_harvest
    "RECOVER" -> R.string.activity_recover
    "DISCARD" -> R.string.activity_discard
    else -> R.string.task_activity_type
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantedSpeciesDetailScreen(
    onBack: () -> Unit,
    viewModel: PlantedSpeciesDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentYear = remember { Year.now().value }
    var selectedLocationItems by remember { mutableStateOf<List<PlantLocationGroup>>(emptyList()) }
    var selectedLocationName by remember { mutableStateOf("") }
    var selectedSubItem by remember { mutableStateOf<PlantLocationGroup?>(null) }
    var actionCount by remember { mutableStateOf("") }
    var actionSubmitting by remember { mutableStateOf(false) }
    var selectedTargetBedId by remember { mutableStateOf<Long?>(null) }
    var bedExpanded by remember { mutableStateOf(false) }
    var plantOutMode by remember { mutableStateOf(false) }

    val dismissModal = {
        selectedLocationItems = emptyList()
        selectedLocationName = ""
        selectedSubItem = null
        actionCount = ""
        actionSubmitting = false
        selectedTargetBedId = null
        bedExpanded = false
        plantOutMode = false
    }

    if (selectedLocationItems.isNotEmpty() && selectedSubItem == null) {
        // Step 1: pick a status group within the location
        AlertDialog(
            onDismissRequest = dismissModal,
            title = { Text(selectedLocationName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedLocationItems.filter { it.status != "REMOVED" }.forEach { item ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedSubItem = item
                                actionCount = item.count.toString()
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(statusLabel(item.status)),
                                    fontSize = 14.sp,
                                    color = statusColor(item.status)
                                )
                                Text("${item.count}", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = dismissModal) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (selectedSubItem != null && !plantOutMode) {
        // Action selection
        val item = selectedSubItem!!
        val actions = buildList {
            when (item.status) {
                "SEEDED" -> {
                    if (item.bedId == null) add("POTTED_UP" to R.string.pot_up)
                    add("PLANTED_OUT" to R.string.plant_out)
                }
                "POTTED_UP" -> add("PLANTED_OUT" to R.string.plant_out)
                "PLANTED_OUT", "GROWING" -> {
                    add("HARVESTED" to R.string.harvest)
                    add("RECOVERED" to R.string.recover)
                }
                "HARVESTED" -> add("RECOVERED" to R.string.recover)
            }
            add("REMOVED" to R.string.discard)
        }
        AlertDialog(
            onDismissRequest = dismissModal,
            title = { Text("${stringResource(statusLabel(item.status))} \u00B7 ${item.count}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = actionCount,
                        onValueChange = { actionCount = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.plant_count)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    val count = actionCount.toIntOrNull() ?: 0
                    actions.forEach { (eventType, labelRes) ->
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
                            shape = RoundedCornerShape(12.dp),
                            enabled = count in 1..item.count && !actionSubmitting,
                            colors = if (eventType == "REMOVED") ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                     else ButtonDefaults.buttonColors()
                        ) {
                            Text(stringResource(labelRes))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    if (selectedLocationItems.size > 1) {
                        selectedSubItem = null
                        actionCount = ""
                    } else {
                        dismissModal()
                    }
                }) {
                    Text(stringResource(if (selectedLocationItems.size > 1) R.string.back else R.string.cancel))
                }
            }
        )
    }

    if (selectedSubItem != null && plantOutMode) {
        // Bed selection for Plant Out
        val item = selectedSubItem!!
        val count = actionCount.toIntOrNull() ?: 0
        AlertDialog(
            onDismissRequest = dismissModal,
            title = { Text(stringResource(R.string.plant_out)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.select_bed),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    ExposedDropdownMenuBox(
                        expanded = bedExpanded,
                        onExpandedChange = { bedExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.beds.find { it.id == selectedTargetBedId }?.let { "${it.gardenName} - ${it.name}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text(stringResource(R.string.select_bed)) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(bedExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = bedExpanded,
                            onDismissRequest = { bedExpanded = false }
                        ) {
                            uiState.beds.forEach { bed ->
                                DropdownMenuItem(
                                    text = { Text("${bed.gardenName} - ${bed.name}") },
                                    onClick = { selectedTargetBedId = bed.id; bedExpanded = false }
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
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedTargetBedId != null && count in 1..item.count && !actionSubmitting
                    ) {
                        Text(stringResource(R.string.plant_out))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { plantOutMode = false; selectedTargetBedId = null }) {
                    Text(stringResource(R.string.back))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.speciesName.ifEmpty { stringResource(R.string.plant) }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = verdantTopAppBarColors()
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                app.verdant.android.ui.common.ConnectionErrorState(onRetry = { viewModel.load() })
            }

            else -> {
                val currentLocations = remember(uiState.locations) {
                    uiState.locations.filter { it.year == currentYear && it.status != "REMOVED" }
                }
                val previousYears = remember(uiState.locations) {
                    uiState.locations.filter { it.year < currentYear }
                        .groupBy { it.year }
                        .toSortedMap(compareByDescending { it })
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Next Activities ──
                    if (uiState.tasks.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.next_activities),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        items(uiState.tasks, key = { it.id }) { task ->
                            TaskCard(task)
                        }
                    }

                    // ── Current Locations ──
                    item {
                        Text(
                            stringResource(R.string.current_locations),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    if (currentLocations.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_current_plants),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        val grouped = currentLocations.groupBy { it.bedId }
                        grouped.forEach { (_, items) ->
                            item {
                                val first = items.first()
                                val location = if (first.bedId == null) stringResource(R.string.tray)
                                    else "${first.gardenName} / ${first.bedName}"
                                val actionable = items.filter { it.status != "REMOVED" }
                                LocationCard(
                                    location = location,
                                    items = items,
                                    onClick = if (actionable.isNotEmpty()) {{
                                        selectedLocationName = location
                                        selectedLocationItems = actionable
                                        if (actionable.size == 1) {
                                            selectedSubItem = actionable.first()
                                            actionCount = actionable.first().count.toString()
                                        }
                                    }} else null
                                )
                            }
                        }
                    }

                    // ── Previous Years ──
                    if (previousYears.isNotEmpty()) {
                        previousYears.forEach { (year, yearLocations) ->
                            item {
                                Text(
                                    "$year",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            val grouped = yearLocations.groupBy { it.bedId }
                            grouped.forEach { (_, items) ->
                                item {
                                    val first = items.first()
                                    val location = if (first.bedId == null) stringResource(R.string.tray)
                                        else "${first.gardenName} / ${first.bedName}"
                                    LocationCard(location, items)
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: ScheduledTaskResponse) {
    val deadline = remember(task.deadline) { LocalDate.parse(task.deadline) }
    val today = remember { LocalDate.now() }
    val isOverdue = deadline.isBefore(today)
    val isDueToday = deadline.isEqual(today)

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                activityIcon(task.activityType),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(activityLabel(task.activityType)),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    stringResource(R.string.task_remaining, task.remainingCount, task.targetCount),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    isOverdue -> MaterialTheme.colorScheme.errorContainer
                    isDueToday -> Color(0xFFFFF3E0)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    task.deadline,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isOverdue -> MaterialTheme.colorScheme.error
                        isDueToday -> Color(0xFFE65100)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LocationCard(location: String, items: List<PlantLocationGroup>, onClick: (() -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                location,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor(item.status).copy(alpha = 0.15f),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Text(
                            stringResource(statusLabel(item.status)),
                            fontSize = 13.sp,
                            color = statusColor(item.status)
                        )
                    }
                    Text(
                        "${item.count}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
