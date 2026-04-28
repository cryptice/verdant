package app.verdant.android.ui.task
import app.verdant.android.data.repository.BedRepository
import app.verdant.android.data.repository.SpeciesRepository
import app.verdant.android.data.repository.TaskRepository

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import app.verdant.android.ui.activity.Activity
import app.verdant.android.ui.faltet.FaltetChecklistGroup
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
    private val speciesRepository: SpeciesRepository,
    private val bedRepository: BedRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {
    val taskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0 }
    private val _uiState = MutableStateFlow(TaskFormState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val species = speciesRepository.list().sortedBySwedishName()
                val beds = runCatching { bedRepository.listAll() }.getOrDefault(emptyList())
                val task = taskId?.let { taskRepository.get(it) }
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
                    taskRepository.update(taskId, UpdateScheduledTaskRequest(
                        speciesId = speciesId,
                        activityType = activityType,
                        deadline = deadline,
                        targetCount = targetCount,
                        notes = notes,
                    ))
                } else {
                    taskRepository.create(CreateScheduledTaskRequest(
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

    /**
     * Create the same bed-scoped task once per (bedId, deadline) pair. Used
     * when the user schedules an activity across a family of `<stem>#<n>`
     * beds in one go, optionally with the deadlines staggered. Stops on
     * the first failure.
     */
    fun saveForBeds(
        bedDeadlines: List<Pair<Long, String>>,
        activityType: String,
        targetCount: Int,
        notes: String?,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                for ((id, deadline) in bedDeadlines) {
                    taskRepository.create(CreateScheduledTaskRequest(
                        speciesId = null,
                        bedId = id,
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

/**
 * When a bed name matches `<stem>#<n>` we treat beds sharing that stem
 * (in the same garden) as a family — useful for scheduling the same task
 * across all of them at once.
 */
private val BED_STEM_PATTERN = Regex("^(.*?)#(\\d+)\\s*$")

private fun bedStem(name: String): String? = BED_STEM_PATTERN.matchEntire(name)
    ?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

@Composable
private fun StaggerOption(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    bedsPerBatchText: String,
    onBedsPerBatchChange: (String) -> Unit,
    daysBetweenText: String,
    onDaysBetweenChange: (String) -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sprid ut över flera dagar",
                    fontFamily = app.verdant.android.ui.theme.FaltetDisplay,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    color = app.verdant.android.ui.theme.FaltetInk,
                )
                Text(
                    text = "Schemalägg ett par bäddar i taget istället för alla samtidigt.",
                    fontSize = 11.sp,
                    color = app.verdant.android.ui.theme.FaltetForest,
                )
            }
        }
        if (enabled) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Field(
                    label = "Bäddar per omgång",
                    value = bedsPerBatchText,
                    onValueChange = { onBedsPerBatchChange(it.filter { c -> c.isDigit() }) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                Field(
                    label = "Dagar mellan",
                    value = daysBetweenText,
                    onValueChange = { onDaysBetweenChange(it.filter { c -> c.isDigit() }) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEdit = viewModel.taskId != null
    val existing = uiState.existingTask

    var selectedActivityType by remember { mutableStateOf<String?>(null) }
    var selectedSpecies by remember { mutableStateOf<SpeciesResponse?>(null) }
    var selectedBed by remember { mutableStateOf<app.verdant.android.data.model.BedWithGardenResponse?>(null) }
    var deadline by remember { mutableStateOf<LocalDate?>(null) }
    var targetCountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    // Beds sharing the selected bed's stem (e.g. "Bed #1", "Bed #2", "Bed #3"
    // all share the stem "Bed "). Surfaces a checklist so the user can
    // schedule the same task across the whole family in one go.
    var siblingBedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    // Optional stagger: spread the schedule across multiple days, doing
    // [staggerBedsPerBatch] beds per [staggerDaysBetween]-day batch.
    var staggerEnabled by remember { mutableStateOf(false) }
    var staggerBedsPerBatchText by remember { mutableStateOf("1") }
    var staggerDaysBetweenText by remember { mutableStateOf("7") }

    val isBedActivity = selectedActivityType in BED_ACTIVITY_TYPES

    // Compute the sibling family for the currently-selected bed. Only
    // surfaces in create mode and only when there's at least one other
    // bed sharing the stem in the same garden.
    val bedFamily = remember(selectedBed, uiState.beds, isEdit) {
        val bed = selectedBed ?: return@remember emptyList()
        if (isEdit) return@remember emptyList()
        val stem = bedStem(bed.name) ?: return@remember emptyList()
        uiState.beds
            .filter { it.gardenId == bed.gardenId && bedStem(it.name) == stem }
            .sortedBy { it.name }
            .takeIf { it.size >= 2 }
            .orEmpty()
    }
    // When the family changes (e.g. user picks a different bed), default to
    // selecting every member.
    LaunchedEffect(bedFamily) {
        siblingBedIds = bedFamily.map { it.id }.toSet()
    }

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
        (if (isBedActivity) selectedBed != null && (bedFamily.isEmpty() || siblingBedIds.isNotEmpty())
         else selectedSpecies != null)

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
                    // Bed tasks don't have a meaningful target count — the
                    // task is "do this to the bed", not "do it N times" — so
                    // we send 1 (the backend column requires ≥1).
                    val targetCount = if (isBedActivity) 1
                    else targetCountText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val notesOrNull = notes.ifBlank { null }
                    if (isBedActivity && bedFamily.isNotEmpty()) {
                        // Order siblings by name and compute a deadline per
                        // bed: when stagger is on, beds[0..N-1] keep the
                        // base deadline, beds[N..2N-1] get +D days, etc.
                        val orderedBeds = bedFamily.filter { it.id in siblingBedIds }
                        val perBatch = staggerBedsPerBatchText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val daysBetween = staggerDaysBetweenText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        val base = deadline!!
                        val pairs = orderedBeds.mapIndexed { index, bed ->
                            val batch = if (staggerEnabled) index / perBatch else 0
                            val d = base.plusDays((batch * daysBetween).toLong())
                            bed.id to d.toString()
                        }
                        viewModel.saveForBeds(
                            bedDeadlines = pairs,
                            activityType = selectedActivityType!!,
                            targetCount = targetCount,
                            notes = notesOrNull,
                        )
                    } else {
                        viewModel.save(
                            speciesId = if (isBedActivity) null else selectedSpecies?.id,
                            bedId = if (isBedActivity) selectedBed?.id else null,
                            activityType = selectedActivityType!!,
                            deadline = deadline!!.toString(),
                            targetCount = targetCount,
                            notes = notesOrNull,
                        )
                    }
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
                    if (bedFamily.isNotEmpty()) {
                        item {
                            val familyByBed = bedFamily.associateBy { it.id }
                            FaltetChecklistGroup(
                                label = "Schemalägg även för",
                                options = bedFamily,
                                selected = siblingBedIds.mapNotNull { familyByBed[it] }.toSet(),
                                onSelectedChange = { picks -> siblingBedIds = picks.map { it.id }.toSet() },
                                labelFor = { it.name },
                                selectAllEnabled = true,
                                required = true,
                            )
                        }
                        item {
                            StaggerOption(
                                enabled = staggerEnabled,
                                onEnabledChange = { staggerEnabled = it },
                                bedsPerBatchText = staggerBedsPerBatchText,
                                onBedsPerBatchChange = { staggerBedsPerBatchText = it },
                                daysBetweenText = staggerDaysBetweenText,
                                onDaysBetweenChange = { staggerDaysBetweenText = it },
                            )
                        }
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
                if (!isBedActivity) {
                    item {
                        Field(
                            label = "Målantal (valfri)",
                            value = targetCountText,
                            onValueChange = { targetCountText = it.filter { c -> c.isDigit() } },
                            keyboardType = KeyboardType.Number,
                        )
                    }
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
