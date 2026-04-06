package app.verdant.android.ui.plants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.BatchEventRequest
import app.verdant.android.data.model.BatchSowRequest
import app.verdant.android.data.model.SpeciesPlantSummary
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.SupplyInventoryResponse
import app.verdant.android.ui.theme.verdantTopAppBarColors
import app.verdant.android.data.repository.GardenRepository
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
            val showLoading = _uiState.value.species.isEmpty()
            if (showLoading) _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val species = repo.getSpeciesPlantSummary()
                _uiState.value = _uiState.value.copy(isLoading = false, species = species, error = null)
            } catch (e: Exception) {
                if (showLoading) _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(speciesList = repo.getSpecies())
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(supplyList = repo.getSupplyInventory())
            } catch (_: Exception) { }
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
    val uiState by viewModel.uiState.collectAsState()
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
        if (searchQuery.isBlank()) uiState.species
        else {
            val q = searchQuery.lowercase()
            uiState.species.filter {
                it.speciesName.lowercase().contains(q) ||
                    (it.scientificName?.lowercase()?.contains(q) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plants)) },
                colors = verdantTopAppBarColors()
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_plants)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        app.verdant.android.ui.common.ConnectionErrorState(onRetry = { viewModel.load() })
                    }
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Eco, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.no_plants_found),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.speciesId }) { species ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSpeciesClick(species.speciesId) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        species.speciesName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    species.scientificName?.let {
                                        Text(
                                            it,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        "${species.activePlantCount}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
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
    } // VoiceCommandOverlay
}
