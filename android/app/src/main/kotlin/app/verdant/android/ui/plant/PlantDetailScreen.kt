package app.verdant.android.ui.plant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.PlantEventResponse
import app.verdant.android.data.model.PlantResponse
import app.verdant.android.data.model.PlantWorkflowProgressResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetHero
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.faltet.PhotoPlaceholder
import app.verdant.android.ui.faltet.PhotoTone
import app.verdant.android.ui.theme.FaltetBerry
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetMustard
import app.verdant.android.ui.theme.FaltetSage
import app.verdant.android.ui.theme.FaltetSky
import coil.compose.AsyncImage
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
    onEditPlant: ((Long) -> Unit)? = null,
    onWorkflowProgress: ((Long) -> Unit)? = null,
    refreshKey: Boolean? = null,
    viewModel: PlantDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var eventsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey) {
        if (refreshKey == true) viewModel.refresh()
    }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }

    if (showDeleteDialog && uiState.plant != null) {
        val plantName = uiState.plant!!.name
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Ta bort planta") },
            text = { Text("Vill du ta bort plantan \"${plantName}\"?") },
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
        mastheadCenter = uiState.plant?.name ?: "",
        mastheadRight = {
            if (uiState.plant != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onEditPlant != null) {
                        IconButton(
                            onClick = { onEditPlant(uiState.plant!!.id) },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Default.Edit, "Redigera", tint = FaltetAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Default.DeleteOutline, "Ta bort", tint = FaltetClay, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        fab = {
            uiState.plant?.let { plant ->
                FaltetFab(onClick = { onAddEvent(plant.id) }, contentDescription = "Lägg till händelse")
            }
        },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null && uiState.plant == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            uiState.plant == null -> FaltetEmptyState(
                headline = "Plantan hittades inte",
                subtitle = "Plantan kan ha tagits bort.",
                modifier = Modifier.padding(padding),
            )
            else -> {
                val plant = uiState.plant!!

                val eventsByMonth: List<Pair<java.time.YearMonth, List<PlantEventResponse>>> = remember(uiState.events) {
                    uiState.events
                        .sortedByDescending { it.eventDate }
                        .groupBy {
                            try {
                                java.time.YearMonth.from(java.time.LocalDate.parse(it.eventDate.take(10)))
                            } catch (e: Exception) {
                                java.time.YearMonth.now()
                            }
                        }
                        .toList()
                }

                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item {
                        FaltetHero(
                            title = plant.name,
                            subtitle = plant.speciesName?.takeIf { it.isNotBlank() },
                            leading = {
                                PhotoPlaceholder(
                                    label = plant.name,
                                    tone = PhotoTone.Blush,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            },
                        )
                    }

                    item {
                        val stages = listOf("SÅDD", "OMSKOLAD", "UTPLANTERAD", "SKÖRDAD")
                        val currentIdx = when (plant.status) {
                            "SEEDED" -> 0
                            "POTTED_UP" -> 1
                            "PLANTED_OUT", "GROWING" -> 2
                            "HARVESTED" -> 3
                            else -> -1
                        }
                        Text(
                            text = buildAnnotatedString {
                                stages.forEachIndexed { i, stage ->
                                    val color = when {
                                        i < currentIdx -> FaltetInk
                                        i == currentIdx -> FaltetAccent
                                        else -> FaltetForest.copy(alpha = 0.4f)
                                    }
                                    withStyle(SpanStyle(color = color)) { append(stage) }
                                    if (i < stages.size - 1) {
                                        withStyle(SpanStyle(color = FaltetForest.copy(alpha = 0.4f))) {
                                            append("  ·  ")
                                        }
                                    }
                                }
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 1.4.sp,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        )
                    }

                    if (eventsByMonth.isEmpty()) {
                        item { FaltetSectionHeader(label = "Händelser") }
                        item { InlineEmpty("Inga händelser ännu.") }
                    } else {
                        val totalEvents = eventsByMonth.sumOf { it.second.size }
                        val visibleByMonth = if (eventsExpanded || totalEvents <= 1) {
                            eventsByMonth
                        } else {
                            // Show only the latest event (top of the first month group)
                            eventsByMonth.firstOrNull()
                                ?.let { (ym, evts) -> listOf(ym to evts.take(1)) }
                                ?: emptyList()
                        }
                        if (totalEvents > 1) {
                            item(key = "events_toggle") {
                                androidx.compose.material3.TextButton(
                                    onClick = { eventsExpanded = !eventsExpanded },
                                ) {
                                    androidx.compose.material3.Text(
                                        text = if (eventsExpanded)
                                            "Visa färre"
                                        else
                                            "Visa fler (${totalEvents - 1})",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = FaltetAccent,
                                    )
                                }
                            }
                        }
                        visibleByMonth.forEach { (yearMonth, events) ->
                            item(key = "header_${yearMonth}_${eventsExpanded}") {
                                FaltetSectionHeader(label = monthLabelSv(yearMonth))
                            }
                            items(events, key = { "ev_${it.id}" }) { event ->
                                FaltetListRow(
                                    leading = {
                                        Box(
                                            Modifier
                                                .size(10.dp)
                                                .drawBehind { drawCircle(eventToneColor(event.eventType)) },
                                        )
                                    },
                                    title = eventTypeLabelSv(event.eventType),
                                    meta = buildString {
                                        append(formattedDate(event.eventDate))
                                        event.notes?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                                    },
                                    metaMaxLines = 2,
                                    stat = {
                                        val statText = listOfNotNull(
                                            event.plantCount?.takeIf { it > 0 }?.let { "$it" },
                                            event.weightGrams?.takeIf { it > 0 }?.let { "${it.toInt()}g" },
                                        ).joinToString(" · ")
                                        if (statText.isNotBlank()) {
                                            Text(
                                                text = statText,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 14.sp,
                                                color = FaltetInk,
                                            )
                                        }
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

fun eventTypeIcon(type: String) = when (type) {
    "SEEDED" -> Icons.Default.Grain
    "POTTED_UP" -> Icons.Default.Inventory2
    "PLANTED_OUT" -> Icons.Default.Park
    "GROWING" -> Icons.Default.Grass
    "HARVESTED" -> Icons.Default.Agriculture
    "RECOVERED" -> Icons.Default.Shield
    "REMOVED" -> Icons.Default.Delete
    "NOTE" -> Icons.Default.StickyNote2
    "APPLIED_SUPPLY" -> Icons.Default.WaterDrop
    else -> Icons.Default.Circle
}

private fun eventToneColor(type: String?): androidx.compose.ui.graphics.Color = when (type) {
    "SEEDED", "SOWED" -> FaltetMustard
    "POTTED_UP" -> FaltetSky
    "PLANTED_OUT" -> FaltetSage
    "HARVESTED" -> FaltetAccent
    "FERTILIZED" -> FaltetBerry
    "WATERED" -> FaltetSky
    "NOTE" -> FaltetForest
    else -> FaltetForest
}

private fun eventTypeLabelSv(type: String?): String = when (type) {
    "SEEDED", "SOWED" -> "Sådd"
    "POTTED_UP" -> "Omskolad"
    "PLANTED_OUT" -> "Utplanterad"
    "HARVESTED" -> "Skördad"
    "FERTILIZED" -> "Gödslad"
    "WATERED" -> "Vattnad"
    "NOTE" -> "Anteckning"
    null -> "—"
    else -> type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

private fun formattedDate(date: String?): String {
    if (date == null) return "—"
    return try {
        val parsed = java.time.LocalDate.parse(date.take(10))
        "${parsed.dayOfMonth} ${monthShortSv(parsed.monthValue)}"
    } catch (e: Exception) {
        date
    }
}

private fun monthShortSv(month: Int): String = arrayOf(
    "jan", "feb", "mar", "apr", "maj", "jun",
    "jul", "aug", "sep", "okt", "nov", "dec",
)[month - 1]

private fun monthLabelSv(yearMonth: java.time.YearMonth): String {
    val names = arrayOf(
        "Januari", "Februari", "Mars", "April", "Maj", "Juni",
        "Juli", "Augusti", "September", "Oktober", "November", "December",
    )
    return "${names[yearMonth.monthValue - 1]} ${yearMonth.year}"
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
private fun WorkflowStripPreview() {
    val stages = listOf("SÅDD", "OMSKOLAD", "UTPLANTERAD", "SKÖRDAD")
    val currentIdx = 1
    Text(
        text = buildAnnotatedString {
            stages.forEachIndexed { i, stage ->
                val color = when {
                    i < currentIdx -> FaltetInk
                    i == currentIdx -> FaltetAccent
                    else -> FaltetForest.copy(alpha = 0.4f)
                }
                withStyle(SpanStyle(color = color)) { append(stage) }
                if (i < stages.size - 1) {
                    withStyle(SpanStyle(color = FaltetForest.copy(alpha = 0.4f))) {
                        append("  ·  ")
                    }
                }
            }
        },
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
    )
}
