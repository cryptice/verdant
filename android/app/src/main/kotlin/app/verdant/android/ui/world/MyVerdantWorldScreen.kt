package app.verdant.android.ui.world
import app.verdant.android.data.repository.AnalyticsRepository
import app.verdant.android.data.repository.PlantRepository

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.DashboardResponse
import app.verdant.android.data.model.GardenSummary
import app.verdant.android.data.model.HarvestStatRow
import app.verdant.android.data.model.TraySummaryEntry
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MyVerdantWorldScreen"

data class MyWorldState(
    val isLoading: Boolean = true,
    val dashboard: DashboardResponse? = null,
    val harvestStats: List<HarvestStatRow> = emptyList(),
    val trayPlants: List<TraySummaryEntry> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class MyWorldViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val plantRepository: PlantRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyWorldState())
    val uiState = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val showLoading = _uiState.value.dashboard == null
            if (showLoading) _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val dashboard = analyticsRepository.dashboard()
                val stats = analyticsRepository.harvestStats()
                val tray = try { plantRepository.traySummary() } catch (e: Exception) { Log.e(TAG, "Failed to load tray summary", e); emptyList() }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dashboard = dashboard,
                    harvestStats = stats,
                    trayPlants = tray,
                )
            } catch (e: Exception) {
                if (showLoading) _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVerdantWorldScreen(
    onGardenClick: (Long) -> Unit,
    onCreateGarden: () -> Unit,
    onSow: () -> Unit = {},
    onSpeciesClick: (Long) -> Unit = {},
    viewModel: MyWorldViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Trädgårdar",
        mastheadRight = {
            TextButton(onClick = onSow) { Text("Så", fontSize = 12.sp) }
        },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            uiState.dashboard == null ||
                (uiState.dashboard!!.gardens.isEmpty() &&
                    uiState.trayPlants.isEmpty() &&
                    uiState.harvestStats.isEmpty()) -> FaltetEmptyState(
                headline = "Inga trädgårdar",
                subtitle = "Skapa din första trädgård.",
                modifier = Modifier.padding(padding),
                action = {
                    androidx.compose.material3.Button(onClick = onCreateGarden) {
                        Text("+ Skapa trädgård")
                    }
                },
            )
            else -> {
                val dashboard = uiState.dashboard!!
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item { FaltetSectionHeader(label = "Trädgårdar") }
                    if (dashboard.gardens.isEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    text = "Inga trädgårdar ännu",
                                    fontSize = 14.sp,
                                    color = FaltetForest,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = onCreateGarden) { Text("+ Skapa") }
                            }
                        }
                    } else {
                        items(dashboard.gardens, key = { "garden_${it.id}" }) { garden ->
                            FaltetListRow(
                                title = garden.name,
                                meta = "${garden.plantCount} plantor · ${garden.bedCount} bäddar",
                                stat = {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = garden.bedCount.toString(),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 16.sp,
                                            color = FaltetInk,
                                        )
                                        Text(
                                            text = " BÄDDAR",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            letterSpacing = 1.2.sp,
                                            color = FaltetForest,
                                        )
                                    }
                                },
                                onClick = { onGardenClick(garden.id) },
                            )
                        }
                    }

                    item { FaltetSectionHeader(label = "Plantor i brätten") }
                    if (uiState.trayPlants.isEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    text = "Inga sådder i brätten ännu.",
                                    fontSize = 14.sp,
                                    color = FaltetForest,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = onSow) { Text("+ Ny sådd") }
                            }
                        }
                    }
                    if (uiState.trayPlants.isNotEmpty()) {
                        items(uiState.trayPlants) { entry ->
                            FaltetListRow(
                                title = entry.variantName?.let { "${entry.speciesName} – $it" } ?: entry.speciesName,
                                meta = trayStatusLabelSv(entry.status),
                                stat = {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = entry.count.toString(),
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
                                },
                                onClick = { entry.speciesId?.let(onSpeciesClick) },
                            )
                        }
                    }

                    if (uiState.harvestStats.isNotEmpty()) {
                        item { FaltetSectionHeader(label = "Skördestatistik") }
                        items(uiState.harvestStats, key = { "stat_${it.species}" }) { stat ->
                            FaltetListRow(
                                title = stat.species,
                                meta = "${stat.harvestCount} skördar · ${stat.totalQuantity} st",
                                stat = {
                                    Text(
                                        text = formatWeight(stat.totalWeightGrams),
                                        fontFamily = FaltetDisplay,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 16.sp,
                                        color = FaltetInk,
                                    )
                                },
                            )
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

private fun trayStatusLabelSv(status: String): String = when (status) {
    "SEEDED" -> "Sådd"
    "POTTED_UP" -> "Omskolad"
    "PLANTED_OUT", "GROWING" -> "Växer"
    "HARVESTED" -> "Skördad"
    "RECOVERED" -> "Återhämtad"
    "REMOVED" -> "Borttagen"
    else -> status
}

private fun formatWeight(grams: Double): String {
    return if (grams >= 1000) "${"%.1f".format(grams / 1000)} KG"
    else "${"%.0f".format(grams)} G"
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun MyVerdantWorldScreenPreview() {
    val dashboard = DashboardResponse(
        user = app.verdant.android.data.model.UserResponse(
            id = 1L, email = "a@b.com", displayName = "Erik", avatarUrl = null, role = "USER", createdAt = "2024-01-01",
        ),
        gardens = listOf(
            GardenSummary(id = 1, name = "Köksträdgården", emoji = "🌿", bedCount = 4, plantCount = 12),
            GardenSummary(id = 2, name = "Bärlandet", emoji = "🍓", bedCount = 2, plantCount = 6),
        ),
        stats = app.verdant.android.data.model.DashboardStats(totalGardens = 2, totalBeds = 6, totalPlants = 18),
    )
    val trayPlants = listOf(TraySummaryEntry(speciesName = "Tomat", status = "SEEDED", count = 24))
    val harvestStats = listOf(HarvestStatRow(species = "Gurka", totalWeightGrams = 1340.0, totalQuantity = 7, harvestCount = 3))

    val uiState = MyWorldState(isLoading = false, dashboard = dashboard, trayPlants = trayPlants, harvestStats = harvestStats)

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Trädgårdar",
        fab = { FaltetFab(onClick = {}, contentDescription = "Skapa trädgård") },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { FaltetSectionHeader(label = "Trädgårdar") }
            items(uiState.dashboard!!.gardens, key = { "garden_${it.id}" }) { garden ->
                FaltetListRow(
                    title = garden.name,
                    meta = "${garden.plantCount} plantor · ${garden.bedCount} bäddar",
                    stat = {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = garden.bedCount.toString(), fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = FaltetInk)
                            Text(text = " BÄDDAR", fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.2.sp, color = FaltetForest)
                        }
                    },
                )
            }
            item { FaltetSectionHeader(label = "Plantor i brätten") }
            items(uiState.trayPlants) { entry ->
                FaltetListRow(
                    title = entry.variantName?.let { "${entry.speciesName} – $it" } ?: entry.speciesName,
                    meta = trayStatusLabelSv(entry.status),
                    stat = {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = entry.count.toString(), fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = FaltetInk)
                            Text(text = " ST", fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.2.sp, color = FaltetForest)
                        }
                    },
                )
            }
            item { FaltetSectionHeader(label = "Skördestatistik") }
            items(uiState.harvestStats, key = { "stat_${it.species}" }) { stat ->
                FaltetListRow(
                    title = stat.species,
                    meta = "${stat.harvestCount} skördar · ${stat.totalQuantity} st",
                    stat = {
                        Text(text = formatWeight(stat.totalWeightGrams), fontFamily = FaltetDisplay, fontStyle = FontStyle.Italic, fontSize = 16.sp, color = FaltetInk)
                    },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
