package app.verdant.android.ui.plants

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import app.verdant.android.ui.faltet.FaltetHero
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.faltet.PhotoPlaceholder
import app.verdant.android.ui.faltet.PhotoTone
import app.verdant.android.ui.theme.FaltetBerry
import app.verdant.android.ui.theme.FaltetClay
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

    FaltetScreenScaffold(
        mastheadLeft = "§ Art",
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
                        FaltetHero(
                            title = uiState.speciesName,
                            subtitle = "${aggregateCount} plantor",
                            leading = {
                                PhotoPlaceholder(
                                    label = uiState.speciesName,
                                    tone = PhotoTone.Sage,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            },
                        )
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
                                val locationLabel = if (loc.bedId == null) {
                                    "Bricka"
                                } else {
                                    listOfNotNull(loc.gardenName, loc.bedName).joinToString(" / ")
                                }
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
    "HARVESTED" -> FaltetClay
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
