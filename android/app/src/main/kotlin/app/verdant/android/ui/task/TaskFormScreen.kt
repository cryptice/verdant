package app.verdant.android.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateScheduledTaskRequest
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.UpdateScheduledTaskRequest
import app.verdant.android.data.model.sortedBySwedishName
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.activity.Activity
import app.verdant.android.ui.faltet.FaltetDatePicker
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.util.Log
import javax.inject.Inject

private const val TAG = "TaskFormScreen"

data class TaskFormState(
    val isLoading: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val species: List<SpeciesResponse> = emptyList(),
    val beds: List<app.verdant.android.data.model.BedWithGardenResponse> = emptyList(),
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
                val species = repo.getSpecies().sortedBySwedishName()
                val beds = runCatching { repo.getAllBeds() }.getOrDefault(emptyList())
                val task = taskId?.let { repo.getTask(it) }
                _uiState.value = _uiState.value.copy(species = species, beds = beds, existingTask = task)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load task form data", e)
            }
        }
    }

    fun save(
        speciesId: Long?,
        bedId: Long?,
        activityType: String,
        deadline: String,
        targetCount: Int,
        notes: String?,
    ) {
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
                        bedId = bedId,
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

private fun activityTypeLabelSvStr(name: String): String = when (name) {
    Activity.SOW.name -> "Så"
    Activity.POT_UP.name -> "Skola om"
    Activity.PLANT.name -> "Plantera"
    Activity.HARVEST.name -> "Skörda"
    Activity.RECOVER.name -> "Återhämta"
    Activity.DISCARD.name -> "Kassera"
    "WATER" -> "Vattna"
    "WEED" -> "Rensa ogräs"
    "FERTILIZE" -> "Gödsla"
    else -> name
}

private val ACTIVITY_TYPES: List<String> = Activity.entries.map { it.name } +
    listOf("WATER", "WEED", "FERTILIZE")
private val BED_ACTIVITY_TYPES: Set<String> = setOf("WATER", "WEED", "FERTILIZE")

private fun speciesDisplayName(s: SpeciesResponse): String {
    val name = s.commonNameSv ?: s.commonName
    val variant = s.variantNameSv ?: s.variantName
    return if (variant.isNullOrBlank()) name else "$name – $variant"
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

    var selectedActivityType by remember { mutableStateOf<String?>(null) }
    var selectedSpecies by remember { mutableStateOf<SpeciesResponse?>(null) }
    var selectedBed by remember { mutableStateOf<app.verdant.android.data.model.BedWithGardenResponse?>(null) }
    var deadline by remember { mutableStateOf<LocalDate?>(null) }
    var targetCountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val isBedActivity = selectedActivityType in BED_ACTIVITY_TYPES

    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(existing, uiState.species, uiState.beds) {
        if (existing != null && !prefilled && uiState.species.isNotEmpty()) {
            selectedActivityType = existing.activityType
            selectedSpecies = existing.speciesId?.let { id -> uiState.species.find { it.id == id } }
            selectedBed = existing.bedId?.let { id -> uiState.beds.find { it.id == id } }
            deadline = runCatching { LocalDate.parse(existing.deadline) }.getOrNull()
            targetCountText = existing.targetCount.toString()
            notes = existing.notes ?: ""
            prefilled = true
        }
    }

    val canSubmit = selectedActivityType != null &&
        deadline != null &&
        !uiState.isLoading &&
        (if (isBedActivity) selectedBed != null else selectedSpecies != null)

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.saved) { if (uiState.saved) onBack() }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = if (isEdit) (existing?.let { it.speciesName ?: "Uppgift" } ?: "Uppgift") else "Ny uppgift",
        bottomBar = {
            FaltetFormSubmitBar(
                label = if (isEdit) "Spara" else "Skapa",
                onClick = {
                    viewModel.save(
                        speciesId = if (isBedActivity) null else selectedSpecies?.id,
                        bedId = if (isBedActivity) selectedBed?.id else null,
                        activityType = selectedActivityType!!,
                        deadline = deadline!!.toString(),
                        targetCount = targetCountText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                        notes = notes.ifBlank { null },
                    )
                },
                enabled = canSubmit,
                submitting = uiState.isLoading,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoading && isEdit) {
            FaltetLoadingState(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    FaltetDropdown(
                        label = "Aktivitet",
                        options = ACTIVITY_TYPES,
                        selected = selectedActivityType,
                        onSelectedChange = { selectedActivityType = it },
                        labelFor = { activityTypeLabelSvStr(it) },
                        searchable = false,
                        required = true,
                    )
                }
                if (isBedActivity) {
                    item {
                        FaltetDropdown(
                            label = "Bädd",
                            options = uiState.beds,
                            selected = selectedBed,
                            onSelectedChange = { selectedBed = it },
                            labelFor = { "${it.gardenName} · ${it.name}" },
                            searchable = true,
                            required = true,
                        )
                    }
                } else {
                    item {
                        FaltetDropdown(
                            label = "Art",
                            options = uiState.species,
                            selected = selectedSpecies,
                            onSelectedChange = { selectedSpecies = it },
                            labelFor = { speciesDisplayName(it) },
                            searchable = true,
                            required = true,
                        )
                    }
                }
                item {
                    FaltetDatePicker(
                        label = "Deadline",
                        value = deadline,
                        onValueChange = { deadline = it },
                        required = true,
                    )
                }
                item {
                    Field(
                        label = "Målantal (valfri)",
                        value = targetCountText,
                        onValueChange = { targetCountText = it.filter { c -> c.isDigit() } },
                        keyboardType = KeyboardType.Number,
                    )
                }
                item {
                    Field(
                        label = "Anteckningar (valfri)",
                        value = notes,
                        onValueChange = { notes = it },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun TaskFormScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Ny uppgift",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Skapa",
                onClick = {},
                enabled = true,
                submitting = false,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                FaltetDropdown(
                    label = "Aktivitet",
                    options = Activity.entries.map { it.name },
                    selected = Activity.SOW.name,
                    onSelectedChange = {},
                    labelFor = { activityTypeLabelSvStr(it) },
                    searchable = false,
                    required = true,
                )
            }
            item {
                FaltetDropdown(
                    label = "Art",
                    options = emptyList<SpeciesResponse>(),
                    selected = null,
                    onSelectedChange = {},
                    labelFor = { speciesDisplayName(it) },
                    searchable = true,
                    required = true,
                )
            }
            item {
                FaltetDatePicker(
                    label = "Deadline",
                    value = LocalDate.of(2026, 6, 1),
                    onValueChange = {},
                    required = true,
                )
            }
            item {
                Field(
                    label = "Målantal (valfri)",
                    value = "20",
                    onValueChange = {},
                    keyboardType = KeyboardType.Number,
                )
            }
            item {
                Field(
                    label = "Anteckningar (valfri)",
                    value = "",
                    onValueChange = {},
                )
            }
        }
    }
}
