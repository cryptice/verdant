package app.verdant.android.ui.analytics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.SpeciesComparisonResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.SeasonSummaryResponse
import app.verdant.android.data.model.YieldPerBedResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetMetadataRow
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetAccent
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
    var selectedSpecies by remember { mutableStateOf<SpeciesResponse?>(null) }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Analys",
        mastheadRight = {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Tillbaka",
                    tint = FaltetAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))

            uiState.error != null
                && uiState.seasonSummaries.isEmpty()
                && uiState.yieldPerBed.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    ConnectionErrorState(onRetry = { viewModel.refresh() })
                }
            }

            else -> {
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {

                    // ── Säsonger ──────────────────────────────────────────────
                    item { FaltetSectionHeader(label = "Säsonger") }

                    uiState.seasonSummaries.forEach { season ->
                        item(key = "season_header_${season.seasonId}") {
                            FaltetListRow(
                                title = season.seasonName,
                                meta = season.year.toString(),
                            )
                        }
                        item(key = "season_stems_${season.seasonId}") {
                            FaltetMetadataRow(
                                label = "Stjälkar skördade",
                                value = season.totalStemsHarvested.toString(),
                            )
                        }
                        item(key = "season_plants_${season.seasonId}") {
                            FaltetMetadataRow(
                                label = "Plantor",
                                value = season.totalPlants.toString(),
                            )
                        }
                        item(key = "season_species_${season.seasonId}") {
                            FaltetMetadataRow(
                                label = "Arter",
                                value = season.speciesCount.toString(),
                            )
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }

                    // ── Jämförelse av arter ───────────────────────────────────
                    item { FaltetSectionHeader(label = "Jämförelse av arter") }

                    item(key = "species_dropdown") {
                        FaltetDropdown(
                            label = "Art",
                            options = uiState.species,
                            selected = selectedSpecies,
                            onSelectedChange = { species ->
                                selectedSpecies = species
                                viewModel.selectSpecies(species.id)
                            },
                            labelFor = { speciesDisplayName(it) },
                            searchable = true,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        )
                    }

                    if (uiState.loadingComparison) {
                        item(key = "comparison_loading") {
                            FaltetLoadingState(Modifier.height(80.dp))
                        }
                    } else {
                        uiState.comparison?.seasons?.forEach { seasonComp ->
                            item(key = "comp_header_${seasonComp.seasonId}") {
                                FaltetSectionHeader(label = seasonComp.seasonName)
                            }
                            item(key = "comp_plants_${seasonComp.seasonId}") {
                                FaltetMetadataRow(
                                    label = "Plantor",
                                    value = seasonComp.plantCount.toString(),
                                )
                            }
                            item(key = "comp_stems_${seasonComp.seasonId}") {
                                FaltetMetadataRow(
                                    label = "Stjälkar skördade",
                                    value = seasonComp.stemsHarvested.toString(),
                                )
                            }
                            item(key = "comp_spp_${seasonComp.seasonId}") {
                                FaltetMetadataRow(
                                    label = "Stjälkar per planta",
                                    value = seasonComp.stemsPerPlant?.let { "%.1f".format(it) },
                                )
                            }
                            item(key = "comp_year_${seasonComp.seasonId}") {
                                FaltetMetadataRow(
                                    label = "År",
                                    value = seasonComp.year.toString(),
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }

                    // ── Skörd per bädd ────────────────────────────────────────
                    item { FaltetSectionHeader(label = "Skörd per bädd") }

                    uiState.yieldPerBed.forEach { bed ->
                        item(key = "bed_header_${bed.bedId}") {
                            FaltetListRow(
                                title = bed.bedName,
                                meta = bed.gardenName,
                            )
                        }
                        bed.seasons.forEach { season ->
                            item(key = "bed_season_${bed.bedId}_${season.seasonId}") {
                                FaltetMetadataRow(
                                    label = season.seasonName,
                                    value = buildString {
                                        append("${season.stemsHarvested} stjälkar")
                                        season.stemsPerM2?.let {
                                            append(" · ${formatStemsPerSqm(it)} / m²")
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

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun AnalyticsScreenSasonerPreview() {
    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Analys",
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { FaltetSectionHeader(label = "Säsonger") }
            item { FaltetListRow(title = "Säsong 2024", meta = "2024") }
            item { FaltetMetadataRow(label = "Stjälkar skördade", value = "1 240") }
            item { FaltetMetadataRow(label = "Plantor", value = "320") }
            item { FaltetMetadataRow(label = "Arter", value = "18") }
            item { FaltetListRow(title = "Säsong 2023", meta = "2023") }
            item { FaltetMetadataRow(label = "Stjälkar skördade", value = "980") }
            item { FaltetMetadataRow(label = "Plantor", value = "270") }
            item { FaltetMetadataRow(label = "Arter", value = "14") }
        }
    }
}

private fun speciesDisplayName(species: SpeciesResponse): String {
    val base = species.commonNameSv ?: species.commonName
    return species.variantNameSv?.let { "$base $it" }
        ?: species.variantName?.let { "$base $it" }
        ?: base
}

private fun formatWeight(grams: Double): String {
    return if (grams >= 1000) "${"%.1f".format(grams / 1000)} kg"
    else "${"%.0f".format(grams)} g"
}

private fun formatStemsPerSqm(value: Double): String {
    return "%.1f".format(value)
}
