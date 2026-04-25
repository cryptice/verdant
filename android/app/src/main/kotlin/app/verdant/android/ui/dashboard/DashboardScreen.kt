package app.verdant.android.ui.dashboard

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.DashboardResponse
import app.verdant.android.data.model.HarvestStatRow
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.data.model.TraySummaryEntry
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "DashboardScreen"

data class DashboardState(
    val isLoading: Boolean = true,
    val dashboard: DashboardResponse? = null,
    val pendingTasks: List<ScheduledTaskResponse> = emptyList(),
    val trayPlants: List<TraySummaryEntry> = emptyList(),
    val harvestStats: List<HarvestStatRow> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val showLoading = _uiState.value.dashboard == null
            if (showLoading) _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val dashboard = repo.getDashboard()
                val tasks = runCatching { repo.getTasks() }.getOrDefault(emptyList())
                val tray = runCatching { repo.getTraySummary() }.getOrDefault(emptyList())
                val stats = runCatching { repo.getHarvestStats() }.getOrDefault(emptyList())
                _uiState.value = DashboardState(
                    isLoading = false,
                    dashboard = dashboard,
                    pendingTasks = tasks.filter { it.status != "COMPLETED" && it.remainingCount > 0 }
                        .sortedBy { it.deadline },
                    trayPlants = tray,
                    harvestStats = stats,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load dashboard", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onTaskClick: (ScheduledTaskResponse) -> Unit = {},
    onOpenTasks: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Översikt",
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null && uiState.dashboard == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            uiState.dashboard == null -> FaltetEmptyState(
                headline = "Ingen data ännu",
                subtitle = "Skapa en trädgård för att komma igång.",
                modifier = Modifier.padding(padding),
            )
            else -> {
                val dashboard = uiState.dashboard!!
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item { HeroStat(value = dashboard.stats.totalBeds, label = "Aktiva bäddar") }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                        ) {
                            Box(Modifier.weight(1f)) {
                                FaltetSectionHeader(label = "Pågående uppgifter")
                            }
                            if (uiState.pendingTasks.isNotEmpty()) {
                                TextButton(onClick = onOpenTasks) { Text("Alla", fontSize = 12.sp) }
                            }
                        }
                    }
                    if (uiState.pendingTasks.isEmpty()) {
                        item { InlineMuted("Inget väntar på dig.") }
                    } else {
                        items(uiState.pendingTasks.take(6), key = { "task_${it.id}" }) { task ->
                            FaltetListRow(
                                title = task.speciesName ?: activityLabelSv(task.activityType),
                                meta = "${activityLabelSv(task.activityType)} · ${task.deadline.take(10)}",
                                stat = {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = task.remainingCount.toString(),
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
                                onClick = { onTaskClick(task) },
                            )
                        }
                    }

                    item { FaltetSectionHeader(label = "Plantor i brätten") }
                    if (uiState.trayPlants.isEmpty()) {
                        item { InlineMuted("—") }
                    } else {
                        items(uiState.trayPlants.take(6)) { entry ->
                            FaltetListRow(
                                title = entry.speciesName,
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
                            )
                        }
                    }

                    item { FaltetSectionHeader(label = "Skördestatistik") }
                    if (uiState.harvestStats.isEmpty()) {
                        item { InlineMuted("—") }
                    } else {
                        items(uiState.harvestStats.take(5), key = { "stat_${it.species}" }) { stat ->
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

@Composable
private fun HeroStat(value: Int, label: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value.toString(),
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 64.sp,
            color = FaltetAccent,
        )
        Text(
            text = label.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest,
        )
    }
}

@Composable
private fun InlineMuted(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = FaltetForest,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
    )
}

private fun trayStatusLabelSv(status: String): String = when (status) {
    "SEEDED" -> "Sådd"
    "POTTED_UP" -> "Krukad"
    "PLANTED_OUT", "GROWING" -> "Växer"
    "HARVESTED" -> "Skördad"
    "RECOVERED" -> "Återhämtad"
    "REMOVED" -> "Borttagen"
    else -> status
}

private fun activityLabelSv(activity: String): String = when (activity) {
    "SOW" -> "Så"
    "POT_UP" -> "Skola om"
    "PLANT" -> "Plantera ut"
    "HARVEST" -> "Skörda"
    "RECOVER" -> "Återhämta"
    "DISCARD" -> "Kassera"
    else -> activity
}

private fun formatWeight(grams: Double): String {
    return if (grams >= 1000) "${"%.1f".format(grams / 1000)} KG"
    else "${"%.0f".format(grams)} G"
}
