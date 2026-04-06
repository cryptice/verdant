package app.verdant.android.ui.plant

import coil.compose.AsyncImage
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

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.PlantEventResponse
import app.verdant.android.data.model.PlantWorkflowProgressResponse
import app.verdant.android.ui.theme.verdantTopAppBarColors
import app.verdant.android.data.model.PlantResponse
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlantDetailState(
    val isLoading: Boolean = true,
    val plant: PlantResponse? = null,
    val events: List<PlantEventResponse> = emptyList(),
    val workflowProgress: PlantWorkflowProgressResponse? = null,
    val error: String? = null,
    val deleted: Boolean = false
)

@HiltViewModel
class PlantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gardenRepository: GardenRepository
) : ViewModel() {
    private val plantId: Long = savedStateHandle.get<Long>("plantId")!!
    private val _uiState = MutableStateFlow(PlantDetailState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = PlantDetailState(isLoading = true)
            try {
                val plant = gardenRepository.getPlant(plantId)
                val events = gardenRepository.getPlantEvents(plantId)
                val workflowProgress = try {
                    gardenRepository.getPlantWorkflowProgress(plantId)
                } catch (_: Exception) {
                    null
                }
                _uiState.value = PlantDetailState(
                    isLoading = false,
                    plant = plant,
                    events = events,
                    workflowProgress = workflowProgress,
                )
            } catch (e: Exception) {
                _uiState.value = PlantDetailState(isLoading = false, error = e.message)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                gardenRepository.deletePlant(plantId)
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                gardenRepository.deletePlantEvent(plantId, eventId)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    onBack: () -> Unit,
    onAddEvent: (Long) -> Unit,
    onWorkflowProgress: ((Long) -> Unit)? = null,
    refreshKey: Boolean? = null,
    viewModel: PlantDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey) {
        if (refreshKey == true) viewModel.refresh()
    }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_plant)) },
            text = { Text(stringResource(R.string.delete_plant_confirm)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.plant?.name ?: stringResource(R.string.plant)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = verdantTopAppBarColors()
            )
        },
        floatingActionButton = {
            if (uiState.plant != null) {
                FloatingActionButton(onClick = { onAddEvent(uiState.plant!!.id) }) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_event))
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null && uiState.plant == null -> {
                app.verdant.android.ui.common.ConnectionErrorState(
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.plant != null -> {
                val plant = uiState.plant!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Header card
                    item {
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(20.dp)) {
                                Text(plant.name, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                plant.speciesName?.let {
                                    Spacer(Modifier.height(4.dp))
                                    Text(it, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(plant.status.replace("_", " ")) },
                                        leadingIcon = { Icon(eventTypeIcon(plant.status), null, Modifier.size(16.dp)) }
                                    )
                                    plant.seedCount?.let {
                                        AssistChip(onClick = {}, label = { Text(stringResource(R.string.seeds_count, it)) })
                                    }
                                    plant.survivingCount?.let {
                                        AssistChip(onClick = {}, label = { Text(stringResource(R.string.alive_count, it)) })
                                    }
                                }
                            }
                        }
                    }

                    // Workflow progress section
                    uiState.workflowProgress?.let { progress ->
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Workflow Progress",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                        )
                                        if (onWorkflowProgress != null && plant.speciesId != null) {
                                            TextButton(
                                                onClick = { onWorkflowProgress(plant.speciesId) }
                                            ) {
                                                Text("View All")
                                                Icon(
                                                    Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    val currentStepName = progress.steps
                                        .firstOrNull { it.id == progress.currentStepId }?.name
                                    val completedCount = progress.completedStepIds.size
                                    val totalCount = progress.steps.size
                                    if (currentStepName != null) {
                                        Text(
                                            "Current step: $currentStepName",
                                            fontSize = 14.sp,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = {
                                            if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                                        },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "$completedCount / $totalCount steps completed",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }
                    }

                    // Timeline header
                    item {
                        Text(stringResource(R.string.timeline), fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            modifier = Modifier.padding(top = 8.dp))
                    }

                    if (uiState.events.isEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Text(
                                    stringResource(R.string.no_events_yet),
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    items(uiState.events.reversed()) { event ->
                        EventCard(event = event, onDelete = { viewModel.deleteEvent(event.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: PlantEventResponse, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_event)) },
            text = { Text(stringResource(R.string.remove_event_confirm)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(eventTypeIcon(event.eventType), null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(event.eventType.replace("_", " "), fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(event.eventDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, stringResource(R.string.delete), Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }

            // Data row
            val dataItems = buildList {
                event.plantCount?.let { add("Count: $it") }
                event.weightGrams?.let { add("${it}g") }
                event.quantity?.let { add("Qty: $it") }
            }
            if (dataItems.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(dataItems.joinToString(" · "), fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }

            event.notes?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, fontSize = 14.sp)
            }

            // Photo thumbnail
            event.imageUrl?.let { url ->
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = url,
                    contentDescription = "Event photo",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

fun eventTypeIcon(type: String) = when (type) {
    "SEEDED" -> Icons.Default.Grain
    "POTTED_UP" -> Icons.Default.Inventory2
    "PLANTED_OUT" -> Icons.Default.Park
    "GROWING" -> Icons.Default.Grass
    "HARVESTED" -> Icons.Default.Agriculture
    "RECOVERED" -> Icons.Default.Shield
    "REMOVED" -> Icons.Default.Delete
    "NOTE" -> Icons.Default.StickyNote2
    else -> Icons.Default.Circle
}
