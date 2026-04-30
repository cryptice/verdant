package app.verdant.android.ui.activity
import app.verdant.android.ui.bed.sortedByNaturalName
import app.verdant.android.data.repository.BedRepository
import app.verdant.android.data.repository.PlantRepository
import app.verdant.android.data.repository.SpeciesRepository
import app.verdant.android.data.repository.TaskRepository

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import app.verdant.android.data.model.BedWithGardenResponse
import app.verdant.android.data.model.CompleteTaskPartiallyRequest
import app.verdant.android.data.model.CreatePlantEventRequest
import app.verdant.android.data.model.PlantResponse
import app.verdant.android.data.model.RecordCommentRequest
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.ui.faltet.Chip
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetImagePicker
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.util.Log
import javax.inject.Inject

private const val TAG = "GenericActivityScreen"

data class GenericActivityState(
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
    val plant: PlantResponse? = null,
    val comments: List<String> = emptyList(),
    val beds: List<BedWithGardenResponse> = emptyList(),
    val task: ScheduledTaskResponse? = null,
)

@HiltViewModel
class GenericActivityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val plantRepository: PlantRepository,
    private val speciesRepository: SpeciesRepository,
    private val bedRepository: BedRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {
    val plantId: Long = savedStateHandle.get<Long>("plantId")!!
    private val taskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0 }
    private val _uiState = MutableStateFlow(GenericActivityState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val plant = plantRepository.get(plantId)
                val comments = speciesRepository.frequentComments().map { it.text }
                val beds = bedRepository.listAll().sortedByNaturalName()
                val task = taskId?.let { taskRepository.get(it) }
                _uiState.value = _uiState.value.copy(plant = plant, comments = comments, beds = beds, task = task)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load activity data", e)
            }
        }
    }

    fun submitEvent(
        eventType: String,
        plantCount: Int? = null,
        weightGrams: Double? = null,
        quantity: Int? = null,
        notes: String? = null,
        imageBase64: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                plantRepository.addEvent(
                    plantId,
                    CreatePlantEventRequest(
                        eventType = eventType,
                        eventDate = LocalDate.now().toString(),
                        plantCount = plantCount,
                        weightGrams = weightGrams,
                        quantity = quantity,
                        notes = notes,
                        imageBase64 = imageBase64,
                    )
                )
                if (!notes.isNullOrBlank()) {
                    speciesRepository.recordComment(RecordCommentRequest(notes))
                }
                // Complete task partially if performing from a scheduled task
                val speciesId = _uiState.value.plant?.speciesId
                if (taskId != null && plantCount != null && plantCount > 0 && speciesId != null) {
                    taskRepository.completePartially(taskId, CompleteTaskPartiallyRequest(plantCount, speciesId))
                }
                _uiState.value = _uiState.value.copy(isLoading = false, created = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PotUpActivityScreen(
    onBack: () -> Unit,
    viewModel: GenericActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var count by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.task) {
        if (uiState.task != null && !prefilled) { count = uiState.task!!.remainingCount.toString(); prefilled = true }
    }
    var notes by remember { mutableStateOf("") }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    ActivityScaffold(
        mastheadLeft = "",
        mastheadCenter = uiState.plant?.name ?: "",
        submitLabel = "Skola om",
        onSubmit = {
            viewModel.submitEvent(
                "POTTED_UP",
                plantCount = count.toIntOrNull(),
                notes = notes.ifBlank { null },
                imageBase64 = photoBitmap?.toCompressedBase64(),
            )
        },
        submitEnabled = !uiState.isLoading,
        submitting = uiState.isLoading,
        snackbarHostState = snackbarHostState,
    ) {
        item {
            PhotoSection(
                bitmap = photoBitmap,
                onBitmapChange = { photoBitmap = it },
            )
        }
        item {
            CountField(
                label = "Antal",
                value = count,
                onValueChange = { count = it },
                required = true,
            )
        }
        item {
            FrequentCommentsField(
                value = notes,
                onValueChange = { notes = it },
                suggestions = uiState.comments,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantActivityScreen(
    onBack: () -> Unit,
    viewModel: GenericActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var count by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.task) {
        if (uiState.task != null && !prefilled) { count = uiState.task!!.remainingCount.toString(); prefilled = true }
    }
    var notes by remember { mutableStateOf("") }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    ActivityScaffold(
        mastheadLeft = "",
        mastheadCenter = uiState.plant?.name ?: "",
        submitLabel = "Plantera ut",
        onSubmit = {
            viewModel.submitEvent(
                "PLANTED_OUT",
                plantCount = count.toIntOrNull(),
                notes = notes.ifBlank { null },
                imageBase64 = photoBitmap?.toCompressedBase64(),
            )
        },
        submitEnabled = !uiState.isLoading,
        submitting = uiState.isLoading,
        snackbarHostState = snackbarHostState,
    ) {
        item {
            PhotoSection(
                bitmap = photoBitmap,
                onBitmapChange = { photoBitmap = it },
            )
        }
        item {
            CountField(
                label = "Antal",
                value = count,
                onValueChange = { count = it },
                required = true,
            )
        }
        item {
            FrequentCommentsField(
                value = notes,
                onValueChange = { notes = it },
                suggestions = uiState.comments,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarvestActivityScreen(
    onBack: () -> Unit,
    viewModel: GenericActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var count by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.task) {
        if (uiState.task != null && !prefilled) { count = uiState.task!!.remainingCount.toString(); prefilled = true }
    }
    var weightGrams by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    ActivityScaffold(
        mastheadLeft = "",
        mastheadCenter = uiState.plant?.name ?: "",
        submitLabel = "Skörda",
        onSubmit = {
            viewModel.submitEvent(
                "HARVESTED",
                plantCount = count.toIntOrNull(),
                weightGrams = weightGrams.toDoubleOrNull(),
                quantity = quantity.toIntOrNull(),
                notes = notes.ifBlank { null },
                imageBase64 = photoBitmap?.toCompressedBase64(),
            )
        },
        submitEnabled = !uiState.isLoading,
        submitting = uiState.isLoading,
        snackbarHostState = snackbarHostState,
    ) {
        item {
            PhotoSection(
                bitmap = photoBitmap,
                onBitmapChange = { photoBitmap = it },
            )
        }
        item {
            CountField(
                label = "Antal",
                value = count,
                onValueChange = { count = it },
                required = true,
            )
        }
        item {
            Field(
                label = "Vikt g (valfri)",
                value = weightGrams,
                onValueChange = { weightGrams = it.filter { c -> c.isDigit() || c == '.' } },
                keyboardType = KeyboardType.Decimal,
            )
        }
        item {
            Field(
                label = "Antal stjälkar (valfri)",
                value = quantity,
                onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                keyboardType = KeyboardType.Number,
            )
        }
        item {
            FrequentCommentsField(
                value = notes,
                onValueChange = { notes = it },
                suggestions = uiState.comments,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverActivityScreen(
    onBack: () -> Unit,
    viewModel: GenericActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var count by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.task) {
        if (uiState.task != null && !prefilled) { count = uiState.task!!.remainingCount.toString(); prefilled = true }
    }
    var notes by remember { mutableStateOf("") }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    ActivityScaffold(
        mastheadLeft = "",
        mastheadCenter = uiState.plant?.name ?: "",
        submitLabel = "Återhämta",
        onSubmit = {
            viewModel.submitEvent(
                "RECOVERED",
                plantCount = count.toIntOrNull(),
                notes = notes.ifBlank { null },
                imageBase64 = photoBitmap?.toCompressedBase64(),
            )
        },
        submitEnabled = !uiState.isLoading,
        submitting = uiState.isLoading,
        snackbarHostState = snackbarHostState,
    ) {
        item {
            PhotoSection(
                bitmap = photoBitmap,
                onBitmapChange = { photoBitmap = it },
            )
        }
        item {
            CountField(
                label = "Antal",
                value = count,
                onValueChange = { count = it },
                required = true,
            )
        }
        item {
            FrequentCommentsField(
                value = notes,
                onValueChange = { notes = it },
                suggestions = uiState.comments,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscardActivityScreen(
    onBack: () -> Unit,
    viewModel: GenericActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var count by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.task) {
        if (uiState.task != null && !prefilled) { count = uiState.task!!.remainingCount.toString(); prefilled = true }
    }
    var notes by remember { mutableStateOf("") }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    ActivityScaffold(
        mastheadLeft = "",
        mastheadCenter = uiState.plant?.name ?: "",
        submitLabel = "Kassera",
        onSubmit = {
            viewModel.submitEvent(
                "REMOVED",
                plantCount = count.toIntOrNull(),
                notes = notes.ifBlank { null },
                imageBase64 = photoBitmap?.toCompressedBase64(),
            )
        },
        submitEnabled = !uiState.isLoading,
        submitting = uiState.isLoading,
        snackbarHostState = snackbarHostState,
    ) {
        item {
            PhotoSection(
                bitmap = photoBitmap,
                onBitmapChange = { photoBitmap = it },
            )
        }
        item {
            CountField(
                label = "Antal",
                value = count,
                onValueChange = { count = it },
                required = true,
            )
        }
        item {
            FrequentCommentsField(
                value = notes,
                onValueChange = { notes = it },
                suggestions = uiState.comments,
            )
        }
    }
}

// ── Shared composables for activity screens ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityScaffold(
    mastheadLeft: String,
    mastheadCenter: String,
    submitLabel: String,
    onSubmit: () -> Unit,
    submitEnabled: Boolean,
    submitting: Boolean,
    snackbarHostState: SnackbarHostState,
    content: LazyListScope.() -> Unit,
) {
    FaltetScreenScaffold(
        mastheadLeft = mastheadLeft,
        mastheadCenter = mastheadCenter,
        bottomBar = {
            FaltetFormSubmitBar(
                label = submitLabel,
                onClick = onSubmit,
                enabled = submitEnabled,
                submitting = submitting,
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
            content = content,
        )
    }
}

@Composable
private fun PhotoSection(
    bitmap: Bitmap?,
    onBitmapChange: (Bitmap?) -> Unit,
) {
    FaltetImagePicker(
        label = "Foto (valfri)",
        value = bitmap,
        onValueChange = onBitmapChange,
    )
}

@Composable
private fun CountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    required: Boolean = true,
    error: String? = null,
) {
    Field(
        label = label,
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
        keyboardType = KeyboardType.Number,
        required = required,
        error = error,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrequentCommentsField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Field(
            label = "Anteckningar (valfri)",
            value = value,
            onValueChange = onValueChange,
        )
        if (suggestions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                suggestions.forEach { suggestion ->
                    Box(modifier = Modifier.clickable { onValueChange(suggestion) }) {
                        Chip(text = suggestion)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun HarvestActivityScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    ActivityScaffold(
        mastheadLeft = "",
        mastheadCenter = "Cosmos #1",
        submitLabel = "Skörda",
        onSubmit = {},
        submitEnabled = true,
        submitting = false,
        snackbarHostState = snackbarHostState,
    ) {
        item {
            PhotoSection(bitmap = null, onBitmapChange = {})
        }
        item {
            CountField(
                label = "Antal",
                value = "5",
                onValueChange = {},
                required = true,
            )
        }
        item {
            Field(
                label = "Vikt g (valfri)",
                value = "120.5",
                onValueChange = {},
                keyboardType = KeyboardType.Decimal,
            )
        }
        item {
            Field(
                label = "Antal stjälkar (valfri)",
                value = "12",
                onValueChange = {},
                keyboardType = KeyboardType.Number,
            )
        }
        item {
            FrequentCommentsField(
                value = "Fin skörd",
                onValueChange = {},
                suggestions = listOf("Bra kvalitet", "Tidig skörd", "Lite skadad"),
            )
        }
    }
}
