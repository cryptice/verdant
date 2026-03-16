package app.verdant.android.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.*
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.activity.Activity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class TaskFormState(
    val isLoading: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val species: List<SpeciesResponse> = emptyList(),
    val existingTask: ScheduledTaskResponse? = null,
)

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: GardenRepository,
) : ViewModel() {
    val taskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0 }
    private val _uiState = MutableStateFlow(TaskFormState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val species = repo.getSpecies()
                val task = taskId?.let { repo.getTask(it) }
                _uiState.value = _uiState.value.copy(species = species, existingTask = task)
            } catch (_: Exception) {}
        }
    }

    fun save(speciesId: Long, activityType: String, deadline: String, targetCount: Int, notes: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                if (taskId != null) {
                    repo.updateTask(taskId, UpdateScheduledTaskRequest(
                        speciesId = speciesId,
                        activityType = activityType,
                        deadline = deadline,
                        targetCount = targetCount,
                        notes = notes,
                    ))
                } else {
                    repo.createTask(CreateScheduledTaskRequest(
                        speciesId = speciesId,
                        activityType = activityType,
                        deadline = deadline,
                        targetCount = targetCount,
                        notes = notes,
                    ))
                }
                _uiState.value = _uiState.value.copy(isLoading = false, saved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    onBack: () -> Unit,
    viewModel: TaskFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEdit = viewModel.taskId != null
    val existing = uiState.existingTask

    var selectedSpeciesId by remember { mutableStateOf<Long?>(null) }
    var selectedActivityType by remember { mutableStateOf<String?>(null) }
    var deadline by remember { mutableStateOf("") }
    var targetCount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var speciesExpanded by remember { mutableStateOf(false) }
    var speciesSearch by remember { mutableStateOf("") }
    var activityExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Pre-fill from existing task
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(existing) {
        if (existing != null && !prefilled) {
            selectedSpeciesId = existing.speciesId
            selectedActivityType = existing.activityType
            deadline = existing.deadline
            targetCount = existing.targetCount.toString()
            notes = existing.notes ?: ""
            prefilled = true
        }
    }

    LaunchedEffect(uiState.saved) { if (uiState.saved) onBack() }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadline.takeIf { it.isNotBlank() }?.let {
                LocalDate.parse(it).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        deadline = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isEdit) R.string.edit_task else R.string.create_task)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Activity type picker
            Text(stringResource(R.string.task_activity_type), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            ExposedDropdownMenuBox(
                expanded = activityExpanded,
                onExpandedChange = { activityExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedActivityType?.let { type ->
                        Activity.entries.find { it.name == type }?.let { stringResource(it.labelRes) } ?: type
                    } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(stringResource(R.string.select_activity_type)) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(activityExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = activityExpanded,
                    onDismissRequest = { activityExpanded = false }
                ) {
                    Activity.entries.forEach { activity ->
                        DropdownMenuItem(
                            text = { Text(stringResource(activity.labelRes)) },
                            leadingIcon = { Icon(activity.icon, null, modifier = Modifier.size(20.dp)) },
                            onClick = {
                                selectedActivityType = activity.name
                                activityExpanded = false
                            }
                        )
                    }
                }
            }

            // Species picker
            Text(stringResource(R.string.species_required), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            ExposedDropdownMenuBox(
                expanded = speciesExpanded,
                onExpandedChange = { speciesExpanded = it }
            ) {
                OutlinedTextField(
                    value = speciesSearch.ifBlank {
                        uiState.species.find { it.id == selectedSpeciesId }?.let { s ->
                            if (s.variantName.isNullOrBlank()) s.commonName else "${s.commonName} \u2013 ${s.variantName}"
                        } ?: ""
                    },
                    onValueChange = { speciesSearch = it; speciesExpanded = true },
                    placeholder = { Text(stringResource(R.string.search_species)) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(speciesExpanded) },
                    singleLine = true
                )
                val filtered = uiState.species.filter {
                    speciesSearch.isBlank() || it.commonName.contains(speciesSearch, ignoreCase = true) || (it.variantName?.contains(speciesSearch, ignoreCase = true) == true)
                }
                ExposedDropdownMenu(
                    expanded = speciesExpanded,
                    onDismissRequest = { speciesExpanded = false }
                ) {
                    filtered.forEach { species ->
                        DropdownMenuItem(
                            text = { Text(if (species.variantName.isNullOrBlank()) species.commonName else "${species.commonName} \u2013 ${species.variantName}") },
                            onClick = {
                                selectedSpeciesId = species.id
                                speciesSearch = ""
                                speciesExpanded = false
                            }
                        )
                    }
                    if (filtered.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no_species_found), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                            onClick = {}
                        )
                    }
                }
            }

            // Deadline
            Text(stringResource(R.string.task_deadline), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedTextField(
                value = deadline,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(stringResource(R.string.select_deadline)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, stringResource(R.string.select_deadline))
                    }
                }
            )

            // Target count
            Text(stringResource(R.string.task_target_count), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedTextField(
                value = targetCount,
                onValueChange = { targetCount = it.filter { c -> c.isDigit() } },
                placeholder = { Text(stringResource(R.string.task_target_count)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes_optional)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(Modifier.height(32.dp))
        }

            // Fixed bottom button
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
                Button(
                    onClick = {
                        viewModel.save(
                            speciesId = selectedSpeciesId!!,
                            activityType = selectedActivityType!!,
                            deadline = deadline,
                            targetCount = targetCount.toInt(),
                            notes = notes.ifBlank { null },
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedSpeciesId != null && selectedActivityType != null &&
                            deadline.isNotBlank() && targetCount.toIntOrNull() != null &&
                            targetCount.toInt() > 0 && !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(R.string.save_task))
                    }
                }
            }
        }
    }
}
