package app.verdant.android.ui.dashboard
import app.verdant.android.ui.faltet.BotanicalPlate
import app.verdant.android.data.repository.AnalyticsRepository
import app.verdant.android.data.repository.PlantRepository
import app.verdant.android.data.repository.SupplyRepository
import app.verdant.android.data.repository.TaskRepository
import app.verdant.android.data.repository.TrayLocationRepository

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Yard
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.verdant.android.ui.theme.FaltetInkLine20
import app.verdant.android.ui.theme.FaltetPaper
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
    /**
     * True when the user has nothing they could currently apply via the
     * Gödsla flow — no inventory rows AND no inexhaustible-type rows.
     * Drives the "add supplies" prompt at the top of the dashboard.
     */
    val suppliesEmpty: Boolean = false,
    val toastMessage: String? = null,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val trayLocationRepository: TrayLocationRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val taskRepository: TaskRepository,
    private val plantRepository: PlantRepository,
    private val supplyRepository: SupplyRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardState())
    val uiState = _uiState.asStateFlow()

    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    fun refresh() {
        viewModelScope.launch {
            val showLoading = _uiState.value.dashboard == null
            if (showLoading) _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val dashboard = analyticsRepository.dashboard()
                val tasks = runCatching { taskRepository.list() }.getOrDefault(emptyList())
                val tray = runCatching { plantRepository.traySummary() }.getOrDefault(emptyList())
                val stats = runCatching { analyticsRepository.harvestStats() }.getOrDefault(emptyList())
                val supplyInv = runCatching { supplyRepository.listInventory() }.getOrDefault(emptyList())
                val supplyTypes = runCatching { supplyRepository.listTypes() }.getOrDefault(emptyList())
                // Tasks whose earliestDate is still in the future ("kommande")
                // belong on the dedicated Tasks list, not the dashboard's
                // at-a-glance feed.
                val today = java.time.LocalDate.now()
                val pending = tasks
                    .filter { it.status != "COMPLETED" && it.remainingCount > 0 }
                    .filter {
                        val earliest = it.earliestDate?.let { d -> runCatching { java.time.LocalDate.parse(d) }.getOrNull() }
                        earliest == null || !earliest.isAfter(today)
                    }
                    .sortedBy { it.deadline }
                _uiState.value = DashboardState(
                    isLoading = false,
                    dashboard = dashboard,
                    pendingTasks = pending,
                    trayPlants = tray,
                    harvestStats = stats,
                    suppliesEmpty = supplyInv.isEmpty() && supplyTypes.none { it.inexhaustible },
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
    onSpeciesClick: (Long) -> Unit = {},
    onOpenTrayLocation: (Long) -> Unit = {},
    onOpenSupplies: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeToast()
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    // Tracks which tray locations have their plant rows expanded. Collapsed
    // by default — header row alone (location name + count + actions) reads
    // as the at-a-glance summary.
    val expandedTrayKeys = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Översikt",
        snackbarHost = { SnackbarHost(snackbarHostState) },
        watermark = BotanicalPlate.EmptyGarden,
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
                    item {
                        HeroStats(
                            beds = dashboard.stats.totalBeds,
                            plants = dashboard.stats.totalActivePlants,
                            species = dashboard.stats.totalActiveSpecies,
                        )
                    }

                    if (uiState.suppliesEmpty) {
                        item {
                            EmptySuppliesBanner(onOpenSupplies = onOpenSupplies)
                        }
                    }

                    item {
                        FaltetSectionHeader(
                            label = "Pågående uppgifter",
                            trailing = if (uiState.pendingTasks.isNotEmpty()) {
                                { TextButton(onClick = onOpenTasks) { Text("Alla", fontSize = 12.sp) } }
                            } else null,
                        )
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
                        val grouped = uiState.trayPlants
                            .groupBy { it.trayLocationId to it.trayLocationName }
                            .toList()
                            .sortedBy { (key, _) -> key.second ?: "￿" }
                        grouped.forEach { (key, entries) ->
                            val (locId, locName) = key
                            val groupKey = "loc_${locId ?: "none"}"
                            val totalCount = entries.sumOf { it.count }
                            item(key = groupKey) {
                                TrayLocationGroup(
                                    locName = locName,
                                    totalCount = totalCount,
                                    entries = entries,
                                    expanded = expandedTrayKeys[groupKey] == true,
                                    onToggleExpanded = {
                                        expandedTrayKeys[groupKey] = !(expandedTrayKeys[groupKey] == true)
                                    },
                                    onOpen = locId?.let { id -> { onOpenTrayLocation(id) } },
                                    onSpeciesClick = onSpeciesClick,
                                )
                            }
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

/**
 * One tray-location row on the Dashboard. Tapping anywhere on the card
 * (outside the chevron and the per-species plant rows) navigates to the
 * tray-location detail screen, where the user can take actions
 * (Vattna alla, Gödsla, …). Tapping the chevron toggles the per-species
 * plant rows in place.
 *
 * Collapsed by default — at-a-glance summary (name + count) is the
 * default view; the user expands the row only when they need details.
 */
@Composable
private fun TrayLocationGroup(
    locName: String?,
    totalCount: Int,
    entries: List<TraySummaryEntry>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onOpen: (() -> Unit)?,
    onSpeciesClick: (Long) -> Unit,
) {
    val cardModifier = Modifier
        .padding(horizontal = 14.dp, vertical = 4.dp)
        .fillMaxWidth()
        .background(FaltetPaper, shape = RoundedCornerShape(12.dp))
        .border(1.dp, FaltetInkLine20, shape = RoundedCornerShape(12.dp))
        .let { if (onOpen != null) it.clickable(onClick = onOpen) else it }

    Column(modifier = cardModifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Chevron is its own tap target so it doesn't navigate to detail.
            // Compose's clickable doesn't bubble up through nested click
            // surfaces, so a tap here only toggles expansion.
            androidx.compose.material3.IconButton(
                onClick = onToggleExpanded,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Dölj plantor" else "Visa plantor",
                    tint = FaltetForest,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = locName ?: "Utan plats",
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = FaltetInk,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$totalCount st",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = FaltetForest,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            ) {
                entries.take(6).forEach { entry ->
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
                                    text = " st",
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
        }
    }
}

/**
 * Notice on the Dashboard when the user has nothing they could currently
 * apply via Gödsla — i.e. no supply inventory rows and no inexhaustible
 * supply types. Tapping the card navigates to the Material screen so the
 * user can add supplies right away.
 */
@Composable
private fun EmptySuppliesBanner(onOpenSupplies: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(FaltetPaper, shape = RoundedCornerShape(12.dp))
            .border(1.dp, FaltetAccent, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenSupplies)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Spa,
            contentDescription = null,
            tint = FaltetAccent,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Inget material",
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = FaltetInk,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Lägg till jord, gödsel eller andra förnödenheter för att kunna gödsla bäddar och brätten.",
                fontSize = 12.sp,
                color = FaltetForest,
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = FaltetAccent,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun HeroStats(beds: Int, plants: Int, species: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeroStatCell(value = beds, label = "Bäddar", modifier = Modifier.weight(1f))
        HeroStatCell(value = plants, label = "Plantor", modifier = Modifier.weight(1f))
        HeroStatCell(value = species, label = "Arter", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HeroStatCell(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .border(
                1.dp,
                FaltetInk,
                androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            )
            .background(
                app.verdant.android.ui.theme.FaltetCream,
                androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 12.dp, vertical = 18.dp),
    ) {
        Text(
            text = value.toString(),
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 36.sp,
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
    "POTTED_UP" -> "Omskolad"
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
