package app.verdant.android.ui.succession
import app.verdant.android.data.repository.AnalyticsRepository
import app.verdant.android.data.repository.SeasonRepository
import app.verdant.android.data.repository.SpeciesRepository

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.SeasonResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.SuccessionScheduleResponse
import app.verdant.android.data.model.sortedBySwedishName
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine40
import app.verdant.android.ui.theme.FaltetMustard
import app.verdant.android.ui.theme.FaltetSage
import app.verdant.android.ui.theme.FaltetSky
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import javax.inject.Inject

data class SuccessionState(
    val isLoading: Boolean = true,
    val items: List<SuccessionScheduleResponse> = emptyList(),
    val species: List<SpeciesResponse> = emptyList(),
    val seasons: List<SeasonResponse> = emptyList(),
    val activeSeasonId: Long? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val generatingId: Long? = null,
    val generatedCount: Int? = null,
)

@HiltViewModel
class SuccessionViewModel @Inject constructor(
    private val seasonRepository: SeasonRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val speciesRepository: SpeciesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SuccessionState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val seasons = seasonRepository.list()
                val active = seasons.firstOrNull { it.isActive }
                val items = analyticsRepository.successionSchedules(active?.id)
                val species = speciesRepository.list().sortedBySwedishName()
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

    fun create(request: Map<String, Any?>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                analyticsRepository.createSuccessionSchedule(request)
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
                analyticsRepository.deleteSuccessionSchedule(id)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun generate(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(generatingId = id, generatedCount = null)
            try {
                val ids = analyticsRepository.generateSuccessionTasks(id)
                _uiState.value = _uiState.value.copy(generatingId = null, generatedCount = ids.size)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(generatingId = null, error = e.message)
            }
        }
    }

    fun clearGenerated() {
        _uiState.value = _uiState.value.copy(generatedCount = null)
    }
}

// Derive a status bucket from firstSowDate relative to today.
// Planerad  — first sow is still in the future
// Sådd      — first sow was within the last 14 days (active sowing window)
// Utplanterad — first sow was 15–60 days ago
// Avslutad  — first sow was more than 60 days ago
private enum class SuccessionBucket(val label: String) {
    PLANNED("Planerad"),
    SOWN("Sådd"),
    PLANTED("Utplanterad"),
    COMPLETED("Avslutad"),
}

private fun SuccessionScheduleResponse.bucket(today: LocalDate): SuccessionBucket {
    val sowDate = runCatching { LocalDate.parse(firstSowDate) }.getOrNull() ?: return SuccessionBucket.PLANNED
    val daysAgo = today.toEpochDay() - sowDate.toEpochDay()
    return when {
        daysAgo < 0 -> SuccessionBucket.PLANNED
        daysAgo <= 14 -> SuccessionBucket.SOWN
        daysAgo <= 60 -> SuccessionBucket.PLANTED
        else -> SuccessionBucket.COMPLETED
    }
}

private fun SuccessionScheduleResponse.weekNumber(): Int {
    val date = runCatching { LocalDate.parse(firstSowDate) }.getOrElse { LocalDate.now() }
    return date.get(WeekFields.ISO.weekOfWeekBasedYear())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessionSchedulesScreen(
    onBack: () -> Unit,
    viewModel: SuccessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val tasksGeneratedLabel = stringResource(R.string.tasks_generated)

    LaunchedEffect(uiState.generatedCount) {
        uiState.generatedCount?.let { count ->
            snackbarHostState.showSnackbar("$tasksGeneratedLabel: $count")
            viewModel.clearGenerated()
        }
    }

    if (showDialog) {
        SuccessionDialog(
            species = uiState.species,
            activeSeasonId = uiState.activeSeasonId,
            saving = uiState.saving,
            onDismiss = { showDialog = false },
            onSave = { payload ->
                viewModel.create(payload)
                showDialog = false
            },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Successioner",
        fab = {
            FaltetFab(
                onClick = { showDialog = true },
                contentDescription = stringResource(R.string.new_succession),
            )
        },
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
                headline = "Inga successioner",
                subtitle = "Planera din första succession.",
                plate = app.verdant.android.ui.faltet.BotanicalPlate.Trellis,
                modifier = Modifier.padding(padding),
            )

            else -> {
                val today = LocalDate.now()
                val grouped = remember(uiState.items) {
                    val bucketOrder = SuccessionBucket.entries
                    uiState.items
                        .groupBy { it.bucket(today) }
                        .toSortedMap(compareBy { bucketOrder.indexOf(it) })
                }

                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    grouped.forEach { (bucket, schedules) ->
                        item(key = "header_${bucket.name}") {
                            FaltetSectionHeader(label = bucket.label)
                        }
                        items(schedules, key = { it.id }) { schedule ->
                            SuccessionFaltetRow(
                                schedule = schedule,
                                bucket = bucket,
                                isGenerating = uiState.generatingId == schedule.id,
                                onDelete = { viewModel.delete(schedule.id) },
                                onGenerate = { viewModel.generate(schedule.id) },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Snackbar rendered outside scaffold to float above FAB
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(snackbarHostState)
    }
}

@Composable
private fun SuccessionFaltetRow(
    schedule: SuccessionScheduleResponse,
    bucket: SuccessionBucket,
    isGenerating: Boolean,
    onDelete: () -> Unit,
    onGenerate: () -> Unit,
) {
    val dotColor = when (bucket) {
        SuccessionBucket.PLANNED -> FaltetSky
        SuccessionBucket.SOWN -> FaltetMustard
        SuccessionBucket.PLANTED -> FaltetSage
        SuccessionBucket.COMPLETED -> FaltetInkLine40
    }

    FaltetListRow(
        title = schedule.speciesName,
        meta = "Vecka ${schedule.weekNumber()} · ${bucket.label}",
        leading = {
            Box(
                Modifier
                    .size(10.dp)
                    .drawBehind { drawCircle(dotColor) }
            )
        },
        stat = {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = schedule.seedsPerSuccession.toString(),
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
        actions = null,
        onClick = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessionDialog(
    species: List<SpeciesResponse>,
    activeSeasonId: Long?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any?>) -> Unit,
) {
    var speciesId by remember { mutableStateOf<Long?>(null) }
    var speciesExpanded by remember { mutableStateOf(false) }
    var firstSow by remember { mutableStateOf(LocalDate.now().toString()) }
    var interval by remember { mutableStateOf("14") }
    var total by remember { mutableStateOf("4") }
    var seedsPer by remember { mutableStateOf("50") }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = firstSow.takeIf { it.isNotBlank() }?.let {
                runCatching { LocalDate.parse(it).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() }.getOrNull()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        firstSow = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_succession)) },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = speciesExpanded,
                    onExpandedChange = { speciesExpanded = it },
                ) {
                    OutlinedTextField(
                        value = species.find { it.id == speciesId }?.let { it.commonNameSv ?: it.commonName } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.species)) },
                        modifier = Modifier.fillMaxSize().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable, true),
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

                OutlinedTextField(
                    value = firstSow,
                    onValueChange = { firstSow = it },
                    label = { Text(stringResource(R.string.first_sow_date)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            androidx.compose.material3.Icon(Icons.Default.CalendarMonth, null)
                        }
                    },
                )

                OutlinedTextField(
                    value = interval,
                    onValueChange = { interval = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.interval_days)) },
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = total,
                    onValueChange = { total = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.total_successions)) },
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = seedsPer,
                    onValueChange = { seedsPer = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.seeds_per_succession)) },
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            val valid = speciesId != null && firstSow.isNotBlank()
                && interval.toIntOrNull() != null && total.toIntOrNull() != null && seedsPer.toIntOrNull() != null
                && activeSeasonId != null
            Button(
                enabled = valid && !saving,
                onClick = {
                    onSave(
                        mapOf(
                            "seasonId" to activeSeasonId,
                            "speciesId" to speciesId,
                            "firstSowDate" to firstSow,
                            "intervalDays" to (interval.toIntOrNull() ?: 0),
                            "totalSuccessions" to (total.toIntOrNull() ?: 0),
                            "seedsPerSuccession" to (seedsPer.toIntOrNull() ?: 0),
                        )
                    )
                },
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SuccessionSchedulesScreenPreview() {
    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Successioner",
    ) { padding ->
        FaltetEmptyState(
            headline = "Inga successioner",
            subtitle = "Planera din första succession.",
            plate = app.verdant.android.ui.faltet.BotanicalPlate.Trellis,
            modifier = Modifier.padding(padding),
        )
    }
}
