package app.verdant.android.ui.plants

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BatchEventRequest
import app.verdant.android.data.model.BatchSowRequest
import app.verdant.android.data.model.SpeciesPlantSummary
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.SupplyInventoryResponse
import app.verdant.android.data.model.sortedBySwedishName
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSearchField
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.voice.VoiceCommandOverlay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlantedSpeciesListState(
    val isLoading: Boolean = true,
    val species: List<SpeciesPlantSummary> = emptyList(),
    val error: String? = null,
    val speciesList: List<SpeciesResponse> = emptyList(),
    val supplyList: List<SupplyInventoryResponse> = emptyList(),
)

@HiltViewModel
class PlantedSpeciesListViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlantedSpeciesListState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // Load the primary list first so the screen has something to show.
            // The supporting lists (species + supplies, used only by the voice
            // overlay) are loaded after — failures there must never block the UI.
            try {
                val species = repo.getSpeciesPlantSummary()
                _uiState.value = _uiState.value.copy(species = species, error = null)
            } catch (e: Exception) {
                Log.e("PlantedSpeciesList", "species-summary failed", e)
                _uiState.value = _uiState.value.copy(error = e.message ?: e.javaClass.simpleName)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            try {
                _uiState.value = _uiState.value.copy(speciesList = repo.getSpecies().sortedBySwedishName())
            } catch (e: Exception) {
                Log.w("PlantedSpeciesList", "species list failed", e)
            }
            try {
                _uiState.value = _uiState.value.copy(supplyList = repo.getSupplyInventory())
            } catch (e: Exception) {
                Log.w("PlantedSpeciesList", "supply list failed", e)
            }
        }
    }

    suspend fun executePlantActivity(action: String, quantity: Int, species: SpeciesResponse) {
        if (action == "SOW") {
            val name = species.variantName?.let { "${species.commonName} $it" } ?: species.commonName
            repo.batchSow(
                BatchSowRequest(
                    speciesId = species.id,
                    name = name,
                    seedCount = quantity,
                )
            )
        } else {
            val statusForAction = when (action) {
                "SOAK" -> "SEEDED"
                "POT_UP" -> "SEEDED"
                "PLANT" -> "POTTED_UP"
                "HARVEST" -> "PLANTED"
                else -> "SEEDED"
            }
            val eventType = when (action) {
                "SOAK" -> "NOTE"
                "POT_UP" -> "POTTED_UP"
                "PLANT" -> "PLANTED_OUT"
                "HARVEST" -> "HARVESTED"
                else -> "NOTE"
            }
            repo.batchEvent(
                BatchEventRequest(
                    speciesId = species.id,
                    status = statusForAction,
                    eventType = eventType,
                    count = quantity,
                    notes = if (action == "SOAK") "Soaked" else null,
                )
            )
        }
    }

    suspend fun executeSupplyUsage(supply: SupplyInventoryResponse, quantity: Double) {
        repo.decrementSupply(supply.id, quantity)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantedSpeciesListScreen(
    onBack: () -> Unit,
    onSpeciesClick: (Long) -> Unit,
    viewModel: PlantedSpeciesListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.load() }

    VoiceCommandOverlay(
        speciesList = uiState.speciesList,
        supplyList = uiState.supplyList,
        onPlantActivity = { action, quantity, species ->
            viewModel.executePlantActivity(action, quantity, species)
        },
        onSupplyUsage = { supply, quantity ->
            viewModel.executeSupplyUsage(supply, quantity)
        },
    ) {
        val filtered = remember(uiState.species, searchQuery) {
            val tokens = searchQuery.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (tokens.isEmpty()) uiState.species
            else uiState.species.filter { item ->
                val haystack = listOfNotNull(item.speciesName, item.variantName, item.scientificName)
                    .joinToString(" ").lowercase()
                tokens.all { haystack.contains(it) }
            }
        }

        FaltetScreenScaffold(
            mastheadLeft = "",
            mastheadCenter = "Växter",
        ) { padding ->
            when {
                uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
                uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    ConnectionErrorState(onRetry = { viewModel.load() })
                }
                filtered.isEmpty() && searchQuery.isBlank() -> FaltetEmptyState(
                    headline = "Inga utplanterade arter",
                    subtitle = "Börja med att plantera en sådd utomhus.",
                    modifier = Modifier.padding(padding),
                )
                else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item {
                        FaltetSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "SÖK ART",
                        )
                    }
                    items(filtered) { species ->
                        FaltetListRow(
                            title = species.variantName?.let { "${species.speciesName} – $it" } ?: species.speciesName,
                            meta = species.scientificName,
                            stat = {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = species.activePlantCount.toString(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 16.sp,
                                        color = FaltetInk,
                                    )
                                    Text(
                                        text = " STK",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        letterSpacing = 1.2.sp,
                                        color = FaltetForest,
                                    )
                                }
                            },
                            onClick = { onSpeciesClick(species.speciesId) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
