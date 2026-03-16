package app.verdant.android.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.verdant.android.R
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.*
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

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
    private val repo: GardenRepository
) : ViewModel() {
    val plantId: Long = savedStateHandle.get<Long>("plantId")!!
    private val taskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0 }
    private val _uiState = MutableStateFlow(GenericActivityState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val plant = repo.getPlant(plantId)
                val comments = repo.getFrequentComments().map { it.text }
                val beds = repo.getAllBeds()
                val task = taskId?.let { repo.getTask(it) }
                _uiState.value = _uiState.value.copy(plant = plant, comments = comments, beds = beds, task = task)
            } catch (_: Exception) {}
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
                repo.addPlantEvent(
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
                    repo.recordComment(RecordCommentRequest(notes))
                }
                // Complete task partially if performing from a scheduled task
                if (taskId != null && plantCount != null && plantCount > 0) {
                    repo.completeTaskPartially(taskId, CompleteTaskPartiallyRequest(plantCount))
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
    val uiState by viewModel.uiState.collectAsState()
    var count by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.task) {
        if (uiState.task != null && !prefilled) { count = uiState.task!!.remainingCount.toString(); prefilled = true }
    }
    var notes by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    ActivityScaffold(
        title = stringResource(R.string.pot_up),
        plantName = uiState.plant?.name,
        onBack = onBack,
    ) {
        CountField(value = count, onValueChange = { count = it }, label = stringResource(R.string.plant_count))
        PhotoSection(imageBase64) { imageBase64 = it }
        FrequentCommentsField(value = notes, onValueChange = { notes = it }, suggestions = uiState.comments)
        SubmitButton(
            label = stringResource(R.string.record_pot_up),
            isLoading = uiState.isLoading,
            onClick = {
                viewModel.submitEvent("POTTED_UP", plantCount = count.toIntOrNull(), notes = notes.ifBlank { null }, imageBase64 = imageBase64)
            }
        )
        uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantActivityScreen(
    onBack: () -> Unit,
    viewModel: GenericActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var count by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.task) {
        if (uiState.task != null && !prefilled) { count = uiState.task!!.remainingCount.toString(); prefilled = true }
    }
    var notes by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    ActivityScaffold(
        title = stringResource(R.string.plant_out),
        plantName = uiState.plant?.name,
        onBack = onBack,
    ) {
        CountField(value = count, onValueChange = { count = it }, label = stringResource(R.string.plant_count))
        PhotoSection(imageBase64) { imageBase64 = it }
        FrequentCommentsField(value = notes, onValueChange = { notes = it }, suggestions = uiState.comments)
        SubmitButton(
            label = stringResource(R.string.record_planting),
            isLoading = uiState.isLoading,
            onClick = {
                viewModel.submitEvent("PLANTED_OUT", plantCount = count.toIntOrNull(), notes = notes.ifBlank { null }, imageBase64 = imageBase64)
            }
        )
        uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarvestActivityScreen(
    onBack: () -> Unit,
    viewModel: GenericActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var count by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.task) {
        if (uiState.task != null && !prefilled) { count = uiState.task!!.remainingCount.toString(); prefilled = true }
    }
    var weightGrams by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    ActivityScaffold(
        title = stringResource(R.string.harvest),
        plantName = uiState.plant?.name,
        onBack = onBack,
    ) {
        CountField(value = count, onValueChange = { count = it }, label = stringResource(R.string.harvest_count))
        OutlinedTextField(
            value = weightGrams,
            onValueChange = { weightGrams = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Weight (grams)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it.filter { c -> c.isDigit() } },
            label = { Text("Quantity (fruits/stems)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        PhotoSection(imageBase64) { imageBase64 = it }
        FrequentCommentsField(value = notes, onValueChange = { notes = it }, suggestions = uiState.comments)
        SubmitButton(
            label = stringResource(R.string.record_harvest),
            isLoading = uiState.isLoading,
            onClick = {
                viewModel.submitEvent(
                    "HARVESTED",
                    plantCount = count.toIntOrNull(),
                    weightGrams = weightGrams.toDoubleOrNull(),
                    quantity = quantity.toIntOrNull(),
                    notes = notes.ifBlank { null },
                    imageBase64 = imageBase64,
                )
            }
        )
        uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverActivityScreen(
    onBack: () -> Unit,
    viewModel: GenericActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var count by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.task) {
        if (uiState.task != null && !prefilled) { count = uiState.task!!.remainingCount.toString(); prefilled = true }
    }
    var notes by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    ActivityScaffold(
        title = stringResource(R.string.recover),
        plantName = uiState.plant?.name,
        onBack = onBack,
    ) {
        CountField(value = count, onValueChange = { count = it }, label = stringResource(R.string.surviving_count))
        PhotoSection(imageBase64) { imageBase64 = it }
        FrequentCommentsField(value = notes, onValueChange = { notes = it }, suggestions = uiState.comments)
        SubmitButton(
            label = stringResource(R.string.record_recovery),
            isLoading = uiState.isLoading,
            onClick = {
                viewModel.submitEvent("RECOVERED", plantCount = count.toIntOrNull(), notes = notes.ifBlank { null }, imageBase64 = imageBase64)
            }
        )
        uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscardActivityScreen(
    onBack: () -> Unit,
    viewModel: GenericActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var count by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.task) {
        if (uiState.task != null && !prefilled) { count = uiState.task!!.remainingCount.toString(); prefilled = true }
    }
    var notes by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    ActivityScaffold(
        title = stringResource(R.string.discard),
        plantName = uiState.plant?.name,
        onBack = onBack,
    ) {
        CountField(value = count, onValueChange = { count = it }, label = stringResource(R.string.count_removed))
        PhotoSection(imageBase64) { imageBase64 = it }
        FrequentCommentsField(value = notes, onValueChange = { notes = it }, suggestions = uiState.comments)
        SubmitButton(
            label = stringResource(R.string.record_discard),
            isLoading = uiState.isLoading,
            onClick = {
                viewModel.submitEvent("REMOVED", plantCount = count.toIntOrNull(), notes = notes.ifBlank { null }, imageBase64 = imageBase64)
            }
        )
        uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
    }
}

// ── Shared composables for activity screens ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityScaffold(
    title: String,
    plantName: String?,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (plantName != null) {
                Text(plantName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            content()
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PhotoSection(imageBase64: String?, onCapture: (String) -> Unit) {
    Text(stringResource(R.string.photo), fontWeight = FontWeight.Bold, fontSize = 16.sp)
    PhotoPicker(
        imageUrl = null,
        onImageCaptured = { b64, _ -> onCapture(b64) }
    )
}

@Composable
private fun SubmitButton(label: String, isLoading: Boolean, onClick: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(label)
        }
    }
}
