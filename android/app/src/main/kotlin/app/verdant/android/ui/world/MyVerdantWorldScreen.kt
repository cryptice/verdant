package app.verdant.android.ui.world

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.verdant.android.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.*
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyWorldState(
    val isLoading: Boolean = true,
    val dashboard: DashboardResponse? = null,
    val harvestStats: List<HarvestStatRow> = emptyList(),
    val trayPlants: List<TraySummaryEntry> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class MyWorldViewModel @Inject constructor(
    private val repo: GardenRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyWorldState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val showLoading = _uiState.value.dashboard == null
            if (showLoading) _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val dashboard = repo.getDashboard()
                val stats = repo.getHarvestStats()
                val tray = try { repo.getTraySummary() } catch (_: Exception) { emptyList() }
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
    viewModel: MyWorldViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        uiState.error != null -> {
            app.verdant.android.ui.common.ConnectionErrorState(
                onRetry = { viewModel.refresh() }
            )
        }
        else -> {
            val dashboard = uiState.dashboard!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Gardens section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.gardens), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        TextButton(onClick = onCreateGarden) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.add_garden))
                        }
                    }
                }

                if (dashboard.gardens.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("\uD83C\uDF31", fontSize = 48.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.no_gardens_yet), fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.tap_plus_to_create_garden),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 14.sp)
                            }
                        }
                    }
                }

                items(dashboard.gardens) { garden ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGardenClick(garden.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(garden.emoji ?: "\uD83C\uDF31", fontSize = 36.sp)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(garden.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                val plantLabel = if (garden.plantCount == 1) stringResource(R.string.plant_singular) else stringResource(R.string.plant_plural)
                                val bedLabel = if (garden.bedCount == 1) stringResource(R.string.bed_singular) else stringResource(R.string.bed_plural)
                                Text(
                                    "${garden.plantCount} $plantLabel \u00B7 ${garden.bedCount} $bedLabel",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Tray plants section
                if (uiState.trayPlants.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.plants_in_trays), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                uiState.trayPlants.forEach { entry ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(entry.speciesName, fontSize = 14.sp)
                                        }
                                        Text(
                                            "${entry.count}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        val statusRes = when (entry.status) {
                                            "SEEDED" -> R.string.event_seeded
                                            "POTTED_UP" -> R.string.event_potted_up
                                            "PLANTED_OUT", "GROWING" -> R.string.event_growing
                                            "HARVESTED" -> R.string.event_harvested
                                            "RECOVERED" -> R.string.event_recovered
                                            "REMOVED" -> R.string.event_removed
                                            else -> R.string.plant
                                        }
                                        Text(
                                            stringResource(statusRes),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.width(80.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Harvest stats section
                if (uiState.harvestStats.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.harvest_stats), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    items(uiState.harvestStats) { stat ->
                        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(16.dp)) {
                                Text(stat.species, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(formatWeight(stat.totalWeightGrams), fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary)
                                        Text(stringResource(R.string.weight), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(stat.totalQuantity.toString(), fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary)
                                        Text(stringResource(R.string.quantity), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(stat.harvestCount.toString(), fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary)
                                        Text(stringResource(R.string.harvests), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Agriculture, null, Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.no_harvests_yet), fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

private fun formatWeight(grams: Double): String {
    return if (grams >= 1000) "${"%.1f".format(grams / 1000)}kg"
    else "${"%.0f".format(grams)}g"
}
