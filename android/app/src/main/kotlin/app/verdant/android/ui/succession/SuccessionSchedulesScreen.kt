package app.verdant.android.ui.succession

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.SeasonResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.SuccessionScheduleResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.theme.verdantTopAppBarColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SuccessionState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val seasons = repo.getSeasons()
                val active = seasons.firstOrNull { it.isActive }
                val items = repo.getSuccessionSchedules(active?.id)
                val species = repo.getSpecies()
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
                repo.createSuccessionSchedule(request)
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
                repo.deleteSuccessionSchedule(id)
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
                val ids = repo.generateSuccessionTasks(id)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessionSchedulesScreen(
    onBack: () -> Unit,
    viewModel: SuccessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val tasksGeneratedLabel = stringResource(R.string.tasks_generated)

    LaunchedEffect(uiState.generatedCount) {
        uiState.generatedCount?.let { count ->
            snackbarHostState.showSnackbar("$tasksGeneratedLabel: $count")
            viewModel.clearGenerated()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.succession_schedules)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = verdantTopAppBarColors(),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, stringResource(R.string.new_succession))
            }
        },
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            uiState.error != null && uiState.items.isEmpty() -> {
                ConnectionErrorState(
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(padding),
                )
            }

            uiState.items.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_successions),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.items, key = { it.id }) { s ->
                        SuccessionCard(
                            schedule = s,
                            isGenerating = uiState.generatingId == s.id,
                            onDelete = { viewModel.delete(s.id) },
                            onGenerate = { viewModel.generate(s.id) },
                        )
                    }
                }
            }
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
}

@Composable
private fun SuccessionCard(
    schedule: SuccessionScheduleResponse,
    isGenerating: Boolean,
    onDelete: () -> Unit,
    onGenerate: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(schedule.speciesName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "${stringResource(R.string.first_sow_date)}: ${schedule.firstSowDate}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Text(
                "${stringResource(R.string.interval_days)}: ${schedule.intervalDays}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Text(
                "${stringResource(R.string.total_successions)}: ${schedule.totalSuccessions}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Text(
                "${stringResource(R.string.seeds_per_succession)}: ${schedule.seedsPerSuccession}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }

                FilledTonalButton(
                    onClick = onGenerate,
                    enabled = !isGenerating,
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Autorenew, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.generate_tasks))
                    }
                }
            }
        }
    }
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
                        label = { Text(stringResource(R.string.species)) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, null)
                        }
                    },
                )

                OutlinedTextField(
                    value = interval,
                    onValueChange = { interval = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.interval_days)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = total,
                    onValueChange = { total = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.total_successions)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = seedsPer,
                    onValueChange = { seedsPer = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.seeds_per_succession)) },
                    modifier = Modifier.fillMaxWidth(),
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
                    CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
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
