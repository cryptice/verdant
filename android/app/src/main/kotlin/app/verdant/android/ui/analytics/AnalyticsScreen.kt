package app.verdant.android.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.*
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.theme.verdantTopAppBarColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsState(
    val isLoading: Boolean = true,
    val seasonSummaries: List<SeasonSummaryResponse> = emptyList(),
    val yieldPerBed: List<YieldPerBedResponse> = emptyList(),
    val species: List<SpeciesResponse> = emptyList(),
    val selectedSpeciesId: Long? = null,
    val comparison: SpeciesComparisonResponse? = null,
    val loadingComparison: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalyticsState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val summaries = repo.getSeasonSummaries()
                val beds = repo.getYieldPerBed()
                val species = repo.getSpecies()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    seasonSummaries = summaries,
                    yieldPerBed = beds,
                    species = species,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun selectSpecies(id: Long) {
        _uiState.value = _uiState.value.copy(selectedSpeciesId = id, loadingComparison = true)
        viewModelScope.launch {
            try {
                val c = repo.getSpeciesComparison(id)
                _uiState.value = _uiState.value.copy(comparison = c, loadingComparison = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loadingComparison = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analytics)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = verdantTopAppBarColors(),
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            uiState.error != null
                && uiState.seasonSummaries.isEmpty()
                && uiState.yieldPerBed.isEmpty() -> {
                ConnectionErrorState(
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(padding),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { SectionHeader(stringResource(R.string.season_summary)) }
                    if (uiState.seasonSummaries.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_season_data),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    } else {
                        items(uiState.seasonSummaries, key = { "season_${it.seasonId}" }) { s ->
                            SeasonSummaryCard(s)
                        }
                    }

                    item { SectionHeader(stringResource(R.string.species_comparison)) }
                    item {
                        SpeciesComparisonSection(
                            species = uiState.species,
                            selectedId = uiState.selectedSpeciesId,
                            loading = uiState.loadingComparison,
                            comparison = uiState.comparison,
                            onSelect = { viewModel.selectSpecies(it) },
                        )
                    }

                    item { SectionHeader(stringResource(R.string.yield_per_bed)) }
                    if (uiState.yieldPerBed.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_bed_data),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    } else {
                        items(uiState.yieldPerBed, key = { "bed_${it.bedId}" }) { b ->
                            YieldPerBedCard(b)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SeasonSummaryCard(s: SeasonSummaryResponse) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(s.seasonName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${s.year}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(4.dp))
            StatRow(stringResource(R.string.total_harvested), "${s.totalStemsHarvested}")
            StatRow(stringResource(R.string.plants), "${s.totalPlants}")
            StatRow(stringResource(R.string.species), "${s.speciesCount}")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeciesComparisonSection(
    species: List<SpeciesResponse>,
    selectedId: Long?,
    loading: Boolean,
    comparison: SpeciesComparisonResponse?,
    onSelect: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = species.find { it.id == selectedId }?.let { it.commonNameSv ?: it.commonName } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.species)) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                species.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.commonNameSv ?: s.commonName) },
                        onClick = {
                            onSelect(s.id)
                            expanded = false
                        },
                    )
                }
            }
        }

        when {
            loading -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            comparison != null -> {
                if (comparison.seasons.isEmpty()) {
                    Text(
                        stringResource(R.string.no_analytics_data),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                } else {
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            comparison.seasons.forEach { sd ->
                                Column {
                                    Text(
                                        "${sd.seasonName} (${sd.year})",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                    )
                                    StatRow(stringResource(R.string.plants), "${sd.plantCount}")
                                    StatRow(stringResource(R.string.total_harvested), "${sd.stemsHarvested}")
                                    sd.stemsPerPlant?.let {
                                        StatRow(stringResource(R.string.stems_per_plant_label), String.format("%.1f", it))
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
            else -> {
                Text(
                    stringResource(R.string.compare_seasons),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun YieldPerBedCard(b: YieldPerBedResponse) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(b.bedName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                b.gardenName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            b.areaM2?.let {
                Text(
                    "${String.format("%.1f", it)} m²",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.height(6.dp))
            b.seasons.forEach { bs ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(bs.seasonName, fontSize = 13.sp)
                    Text(
                        buildString {
                            append("${bs.stemsHarvested}")
                            bs.stemsPerM2?.let { append(" (${String.format("%.1f", it)} ${"/m²"})") }
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
