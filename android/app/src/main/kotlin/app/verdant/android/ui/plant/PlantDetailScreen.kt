package app.verdant.android.ui.plant

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.PlantEventResponse
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
                _uiState.value = PlantDetailState(isLoading = false, plant = plant, events = events)
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
            title = { Text("Delete Plant") },
            text = { Text("Are you sure you want to delete this plant and all its events?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.plant?.name ?: "Plant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.plant != null) {
                FloatingActionButton(onClick = { onAddEvent(uiState.plant!!.id) }) {
                    Icon(Icons.Default.Add, "Add Event")
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
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
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
                                        AssistChip(onClick = {}, label = { Text("Seeds: $it") })
                                    }
                                    plant.survivingCount?.let {
                                        AssistChip(onClick = {}, label = { Text("Alive: $it") })
                                    }
                                }
                            }
                        }
                    }

                    // Timeline header
                    item {
                        Text("Timeline", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            modifier = Modifier.padding(top = 8.dp))
                    }

                    if (uiState.events.isEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "No events yet. Tap + to add one.",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    items(uiState.events) { event ->
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
            title = { Text("Delete Event") },
            text = { Text("Remove this event?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
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
                        Icon(Icons.Default.Close, "Delete", Modifier.size(16.dp),
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
            event.imageBase64?.let { b64 ->
                val bmp = remember(b64) {
                    runCatching {
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }.getOrNull()
                }
                if (bmp != null) {
                    Spacer(Modifier.height(8.dp))
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Event photo",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
                        contentScale = ContentScale.Crop
                    )
                }
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
