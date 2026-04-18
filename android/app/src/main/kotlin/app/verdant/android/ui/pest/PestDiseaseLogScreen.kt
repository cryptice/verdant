package app.verdant.android.ui.pest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class PestDiseaseLogState(
    val isLoading: Boolean = true,
    val items: List<PestDiseaseLogResponse> = emptyList(),
    val species: List<SpeciesResponse> = emptyList(),
    val activeSeasonId: Long? = null,
    val error: String? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class PestDiseaseLogViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PestDiseaseLogState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val seasons = repo.getSeasons()
                val active = seasons.firstOrNull { it.isActive }
                val items = repo.getPestDiseaseLogs(active?.id)
                val species = repo.getSpecies()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = items,
                    species = species,
                    activeSeasonId = active?.id,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun create(request: CreatePestDiseaseLogRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                repo.createPestDiseaseLog(request)
                _uiState.value = _uiState.value.copy(saving = false)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun update(id: Long, request: UpdatePestDiseaseLogRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                repo.updatePestDiseaseLog(id, request)
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
                repo.deletePestDiseaseLog(id)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PestDiseaseLogScreen(
    onBack: () -> Unit,
    viewModel: PestDiseaseLogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PestDiseaseLogResponse?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pest_disease_log)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = verdantTopAppBarColors(),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, stringResource(R.string.new_pest_disease))
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
                        stringResource(R.string.no_pest_disease),
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
                    items(uiState.items, key = { it.id }) { log ->
                        PestDiseaseLogCard(
                            log = log,
                            speciesName = uiState.species.find { it.id == log.speciesId }?.let { s ->
                                s.commonNameSv ?: s.commonName
                            },
                            onClick = { editing = log; showDialog = true },
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        PestDiseaseLogDialog(
            existing = editing,
            species = uiState.species,
            activeSeasonId = uiState.activeSeasonId,
            saving = uiState.saving,
            onDismiss = { showDialog = false },
            onSave = { req ->
                if (editing != null) {
                    viewModel.update(
                        editing!!.id,
                        UpdatePestDiseaseLogRequest(
                            category = req.category,
                            name = req.name,
                            seasonId = req.seasonId,
                            bedId = req.bedId,
                            speciesId = req.speciesId,
                            observedDate = req.observedDate,
                            severity = req.severity,
                            treatment = req.treatment,
                            outcome = req.outcome,
                            notes = req.notes,
                        ),
                    )
                } else {
                    viewModel.create(req)
                }
                showDialog = false
            },
            onDelete = editing?.let { e ->
                { viewModel.delete(e.id); showDialog = false }
            },
        )
    }
}

@Composable
private fun PestDiseaseLogCard(
    log: PestDiseaseLogResponse,
    speciesName: String?,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(log.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                SeverityBadge(log.severity)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${categoryLabel(log.category)} • ${log.observedDate}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            if (speciesName != null) {
                Text(
                    speciesName,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            log.treatment?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text("${stringResource(R.string.treatment)}: $it", fontSize = 13.sp)
            }
            log.outcome?.let {
                Text("${stringResource(R.string.outcome)}: ${outcomeLabel(it)}", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun categoryLabel(category: String): String = when (category) {
    PestCategory.PEST -> stringResource(R.string.pest_category_pest)
    PestCategory.DISEASE -> stringResource(R.string.pest_category_disease)
    PestCategory.DEFICIENCY -> stringResource(R.string.pest_category_deficiency)
    else -> stringResource(R.string.pest_category_other)
}

@Composable
private fun severityLabel(severity: String): String = when (severity) {
    Severity.LOW -> stringResource(R.string.severity_low)
    Severity.HIGH -> stringResource(R.string.severity_high)
    Severity.CRITICAL -> stringResource(R.string.severity_critical)
    else -> stringResource(R.string.severity_moderate)
}

@Composable
private fun outcomeLabel(outcome: String): String = when (outcome) {
    Outcome.RESOLVED -> stringResource(R.string.outcome_resolved)
    Outcome.ONGOING -> stringResource(R.string.outcome_ongoing)
    Outcome.CROP_LOSS -> stringResource(R.string.outcome_crop_loss)
    Outcome.MONITORING -> stringResource(R.string.outcome_monitoring)
    else -> outcome
}

@Composable
private fun SeverityBadge(severity: String) {
    val color = when (severity) {
        Severity.LOW -> MaterialTheme.colorScheme.secondaryContainer
        Severity.CRITICAL, Severity.HIGH -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val onColor = when (severity) {
        Severity.LOW -> MaterialTheme.colorScheme.onSecondaryContainer
        Severity.CRITICAL, Severity.HIGH -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color) {
        Text(
            severityLabel(severity),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = onColor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PestDiseaseLogDialog(
    existing: PestDiseaseLogResponse?,
    species: List<SpeciesResponse>,
    activeSeasonId: Long?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (CreatePestDiseaseLogRequest) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: PestCategory.PEST) }
    var severity by remember { mutableStateOf(existing?.severity ?: Severity.MODERATE) }
    var outcome by remember { mutableStateOf(existing?.outcome) }
    var observedDate by remember { mutableStateOf(existing?.observedDate ?: LocalDate.now().toString()) }
    var treatment by remember { mutableStateOf(existing?.treatment ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var speciesId by remember { mutableStateOf(existing?.speciesId) }
    var speciesExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = observedDate.takeIf { it.isNotBlank() }?.let {
                runCatching { LocalDate.parse(it).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() }.getOrNull()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        observedDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()
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
        title = { Text(stringResource(if (existing != null) R.string.edit_pest_disease else R.string.new_pest_disease)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.species)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )

                Text(stringResource(R.string.category), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                ChipRow(
                    values = PestCategory.values,
                    selected = category,
                    labelFor = { categoryLabel(it) },
                    onSelect = { category = it },
                )

                Text(stringResource(R.string.severity), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                ChipRow(
                    values = Severity.values,
                    selected = severity,
                    labelFor = { severityLabel(it) },
                    onSelect = { severity = it },
                )

                OutlinedTextField(
                    value = observedDate,
                    onValueChange = { observedDate = it },
                    label = { Text(stringResource(R.string.observed_date)) },
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

                // Species picker (optional)
                ExposedDropdownMenuBox(
                    expanded = speciesExpanded,
                    onExpandedChange = { speciesExpanded = it },
                ) {
                    OutlinedTextField(
                        value = species.find { it.id == speciesId }?.let {
                            it.commonNameSv ?: it.commonName
                        } ?: stringResource(R.string.none),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.affected_plant)) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(speciesExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = speciesExpanded,
                        onDismissRequest = { speciesExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.none)) },
                            onClick = { speciesId = null; speciesExpanded = false },
                        )
                        species.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.commonNameSv ?: s.commonName) },
                                onClick = { speciesId = s.id; speciesExpanded = false },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = treatment,
                    onValueChange = { treatment = it },
                    label = { Text(stringResource(R.string.treatment)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                Text(stringResource(R.string.outcome), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                ChipRow(
                    values = listOf(null) + Outcome.values,
                    selected = outcome,
                    labelFor = { if (it == null) stringResource(R.string.none) else outcomeLabel(it) },
                    onSelect = { outcome = it },
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.delete)) }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && !saving,
                onClick = {
                    onSave(
                        CreatePestDiseaseLogRequest(
                            category = category,
                            name = name,
                            seasonId = activeSeasonId,
                            speciesId = speciesId,
                            observedDate = observedDate.ifBlank { null },
                            severity = severity,
                            treatment = treatment.ifBlank { null },
                            outcome = outcome,
                            notes = notes.ifBlank { null },
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(
    values: List<T>,
    selected: T?,
    labelFor: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        values.forEach { v ->
            FilterChip(
                selected = v == selected,
                onClick = { onSelect(v) },
                label = { Text(labelFor(v), fontSize = 12.sp) },
            )
        }
    }
}
