package app.verdant.android.ui.targets

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.ProductionForecastResponse
import app.verdant.android.data.model.ProductionTargetResponse
import app.verdant.android.data.model.SeasonResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.sortedBySwedishName
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import javax.inject.Inject

data class TargetsState(
    val isLoading: Boolean = true,
    val items: List<ProductionTargetResponse> = emptyList(),
    val species: List<SpeciesResponse> = emptyList(),
    val seasons: List<SeasonResponse> = emptyList(),
    val activeSeasonId: Long? = null,
    val forecasts: Map<Long, ProductionForecastResponse> = emptyMap(),
    val forecastLoadingId: Long? = null,
    val error: String? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class TargetsViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TargetsState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val seasons = repo.getSeasons()
                val active = seasons.firstOrNull { it.isActive }
                val items = repo.getProductionTargets(active?.id)
                val species = repo.getSpecies().sortedBySwedishName()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = items,
                    species = species,
                    seasons = seasons,
                    activeSeasonId = active?.id,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun create(payload: Map<String, Any?>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                repo.createProductionTarget(payload)
                _uiState.value = _uiState.value.copy(saving = false)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                repo.deleteProductionTarget(id)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadForecast(id: Long) {
        if (_uiState.value.forecasts.containsKey(id)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(forecastLoadingId = id)
            try {
                val forecast = repo.getProductionForecast(id)
                _uiState.value = _uiState.value.copy(
                    forecastLoadingId = null,
                    forecasts = _uiState.value.forecasts + (id to forecast),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(forecastLoadingId = null, error = e.message)
            }
        }
    }
}

/** Formats a target's date range as e.g. "2026 · V. 14–26". */
private fun periodLabel(startDate: String, endDate: String): String {
    return try {
        val isoWeek = WeekFields.ISO
        val start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val year = start.get(isoWeek.weekBasedYear())
        val startWeek = start.get(isoWeek.weekOfWeekBasedYear())
        val endWeek = end.get(isoWeek.weekOfWeekBasedYear())
        "$year · V. $startWeek–$endWeek"
    } catch (_: Exception) {
        "$startDate – $endDate"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionTargetsScreen(
    onBack: () -> Unit,
    viewModel: TargetsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Mål",
        fab = { FaltetFab(onClick = { showDialog = true }, contentDescription = "Nytt mål") },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))

            uiState.error != null && uiState.items.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }

            uiState.items.isEmpty() -> FaltetEmptyState(
                headline = "Inga produktionsmål",
                subtitle = "Sätt upp mål för säsongens produktion.",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(uiState.items, key = { it.id }) { target ->
                    FaltetListRow(
                        leading = null,
                        title = target.speciesName ?: "Art #${target.speciesId}",
                        meta = periodLabel(target.startDate, target.endDate),
                        stat = {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = target.stemsPerWeek.toString(),
                                    fontFamily = FaltetDisplay,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 16.sp,
                                    color = FaltetInk,
                                )
                                Text(
                                    text = " stk/v",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    color = FaltetForest,
                                )
                            }
                        },
                        actions = null,
                        onClick = null,
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showDialog) {
        TargetDialog(
            species = uiState.species,
            seasons = uiState.seasons,
            defaultSeasonId = uiState.activeSeasonId,
            saving = uiState.saving,
            onDismiss = { showDialog = false },
            onSave = { payload ->
                viewModel.create(payload)
                showDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetDialog(
    species: List<SpeciesResponse>,
    seasons: List<SeasonResponse>,
    defaultSeasonId: Long?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any?>) -> Unit,
) {
    var speciesId by remember { mutableStateOf<Long?>(null) }
    var seasonId by remember { mutableStateOf(defaultSeasonId) }
    var speciesExpanded by remember { mutableStateOf(false) }
    var seasonExpanded by remember { mutableStateOf(false) }
    var stemsPerWeek by remember { mutableStateOf("100") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusMonths(3).toString()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    @Composable
    fun datePickerFor(current: String, onPicked: (String) -> Unit, onClose: () -> Unit) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = current.takeIf { it.isNotBlank() }?.let {
                runCatching { LocalDate.parse(it).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() }.getOrNull()
            }
        )
        DatePickerDialog(
            onDismissRequest = onClose,
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onPicked(Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString())
                    }
                    onClose()
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = onClose) { Text("Avbryt") }
            },
        ) { DatePicker(state = state) }
    }

    if (showStartPicker) datePickerFor(startDate, { startDate = it }, { showStartPicker = false })
    if (showEndPicker) datePickerFor(endDate, { endDate = it }, { showEndPicker = false })

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nytt mål") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = speciesExpanded,
                    onExpandedChange = { speciesExpanded = it },
                ) {
                    OutlinedTextField(
                        value = species.find { it.id == speciesId }?.let { it.commonNameSv ?: it.commonName } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Art") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable, true),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(speciesExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = speciesExpanded,
                        onDismissRequest = { speciesExpanded = false },
                    ) {
                        species.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.commonNameSv ?: s.commonName) },
                                onClick = { speciesId = s.id; speciesExpanded = false },
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = seasonExpanded,
                    onExpandedChange = { seasonExpanded = it },
                ) {
                    OutlinedTextField(
                        value = seasons.find { it.id == seasonId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Säsong") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable, true),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(seasonExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = seasonExpanded,
                        onDismissRequest = { seasonExpanded = false },
                    ) {
                        seasons.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = { seasonId = s.id; seasonExpanded = false },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = stemsPerWeek,
                    onValueChange = { stemsPerWeek = it.filter { c -> c.isDigit() } },
                    label = { Text("Stjälkar per vecka") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Leveransfönster start") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showStartPicker = true }) {
                            Icon(Icons.Default.CalendarMonth, null)
                        }
                    },
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("Leveransfönster slut") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showEndPicker = true }) {
                            Icon(Icons.Default.CalendarMonth, null)
                        }
                    },
                )
            }
        },
        confirmButton = {
            val valid = speciesId != null && seasonId != null && stemsPerWeek.toIntOrNull() != null
                && startDate.isNotBlank() && endDate.isNotBlank()
            Button(
                enabled = valid && !saving,
                onClick = {
                    onSave(
                        mapOf(
                            "seasonId" to seasonId,
                            "speciesId" to speciesId,
                            "stemsPerWeek" to (stemsPerWeek.toIntOrNull() ?: 0),
                            "startDate" to startDate,
                            "endDate" to endDate,
                        )
                    )
                },
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp))
                } else {
                    Text("Spara")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun ProductionTargetsScreenPreview() {
    val targets = listOf(
        ProductionTargetResponse(
            id = 1L,
            seasonId = 1L,
            speciesId = 10L,
            speciesName = "Solros",
            stemsPerWeek = 120,
            startDate = "2026-04-01",
            endDate = "2026-06-30",
            notes = null,
            createdAt = "2026-01-01T00:00:00Z",
        ),
        ProductionTargetResponse(
            id = 2L,
            seasonId = 1L,
            speciesId = 11L,
            speciesName = "Pion",
            stemsPerWeek = 80,
            startDate = "2026-05-15",
            endDate = "2026-07-20",
            notes = null,
            createdAt = "2026-01-01T00:00:00Z",
        ),
    )
    LazyColumn {
        items(targets, key = { it.id }) { target ->
            FaltetListRow(
                leading = null,
                title = target.speciesName ?: "Art #${target.speciesId}",
                meta = periodLabel(target.startDate, target.endDate),
                stat = {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = target.stemsPerWeek.toString(),
                            fontFamily = FaltetDisplay,
                            fontStyle = FontStyle.Italic,
                            fontSize = 16.sp,
                            color = FaltetInk,
                        )
                        Text(
                            text = " stk/v",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = FaltetForest,
                        )
                    }
                },
                actions = null,
                onClick = null,
            )
        }
    }
}
