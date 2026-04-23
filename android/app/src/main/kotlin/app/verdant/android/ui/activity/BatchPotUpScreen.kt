package app.verdant.android.ui.activity

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.verdant.android.data.model.BatchEventRequest
import app.verdant.android.data.model.CompleteTaskPartiallyRequest
import app.verdant.android.data.model.PlantGroupResponse
import app.verdant.android.data.model.RecordCommentRequest
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetImagePicker
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.supplies.SupplyUsageBottomSheet
import app.verdant.android.ui.theme.FaltetAccent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatchPotUpState(
    val isLoading: Boolean = true,
    val submitting: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
    val groups: List<PlantGroupResponse> = emptyList(),
    val comments: List<String> = emptyList(),
)

@HiltViewModel
class BatchPotUpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val repo: GardenRepository
) : ViewModel() {
    private val taskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0 }
    val preselectedSpeciesId: Long? = savedStateHandle.get<Long>("speciesId")?.takeIf { it > 0 }
    private val _uiState = MutableStateFlow(BatchPotUpState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val groups = repo.getPlantGroups("SEEDED", trayOnly = true)
                val filtered = if (preselectedSpeciesId != null) {
                    groups.filter { it.speciesId == preselectedSpeciesId }
                } else groups
                val comments = repo.getFrequentComments().map { it.text }
                _uiState.value = BatchPotUpState(isLoading = false, groups = filtered, comments = comments)
            } catch (e: Exception) {
                _uiState.value = BatchPotUpState(isLoading = false, error = e.message)
            }
        }
    }

    fun submit(group: PlantGroupResponse, count: Int, notes: String?, imageBase64: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(submitting = true, error = null)
            try {
                repo.batchEvent(
                    BatchEventRequest(
                        speciesId = group.speciesId,
                        bedId = group.bedId,
                        plantedDate = group.plantedDate,
                        status = "SEEDED",
                        eventType = "POTTED_UP",
                        count = count,
                        notes = notes,
                        imageBase64 = imageBase64,
                    )
                )
                if (!notes.isNullOrBlank()) {
                    repo.recordComment(RecordCommentRequest(notes))
                }
                if (taskId != null && count > 0) {
                    repo.completeTaskPartially(taskId, CompleteTaskPartiallyRequest(count, group.speciesId))
                }
                _uiState.value = _uiState.value.copy(submitting = false, created = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(submitting = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchPotUpScreen(
    onBack: () -> Unit,
    viewModel: BatchPotUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedGroup by remember { mutableStateOf<PlantGroupResponse?>(null) }
    var countText by remember { mutableStateOf("") }
    var countError by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }

    var showSupplySheet by remember { mutableStateOf(false) }

    if (showSupplySheet) {
        SupplyUsageBottomSheet(
            repo = viewModel.repo,
            onDismiss = { showSupplySheet = false },
        )
    }

    if (uiState.created) {
        AlertDialog(
            onDismissRequest = { onBack() },
            title = { Text("Kruka upp") },
            text = { Text("Vill du registrera förbrukning av jord eller krukor?") },
            confirmButton = {
                TextButton(onClick = { showSupplySheet = true }) {
                    Text("Registrera förbrukning", color = FaltetAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { onBack() }) { Text("Hoppa över") }
            },
        )
    }

    LaunchedEffect(uiState.groups) {
        if (uiState.groups.size == 1 && selectedGroup == null) {
            selectedGroup = uiState.groups.first()
            countText = uiState.groups.first().count.toString()
        }
    }

    val canSubmit = selectedGroup != null
        && countText.toIntOrNull()?.let { it in 1..selectedGroup!!.count } == true
        && !uiState.submitting

    val submitAction: () -> Unit = {
        val count = countText.toIntOrNull()
        countError = when {
            count == null -> "Antal krävs"
            count !in 1..selectedGroup!!.count -> "Antal måste vara mellan 1 och ${selectedGroup!!.count}"
            else -> null
        }
        if (countError == null) {
            viewModel.submit(
                selectedGroup!!,
                count!!,
                notes.ifBlank { null },
                imageBase64,
            )
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }

    FaltetScreenScaffold(
        mastheadLeft = "§ Kruka upp",
        mastheadCenter = if (selectedGroup == null) "Välj grupp" else selectedGroup!!.speciesName ?: "Okänd art",
        mastheadRight = if (selectedGroup != null) {
            {
                IconButton(
                    onClick = { selectedGroup = null },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Tillbaka",
                        tint = FaltetAccent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        } else null,
        bottomBar = if (selectedGroup != null) {
            {
                FaltetFormSubmitBar(
                    label = "Kruka upp",
                    onClick = submitAction,
                    enabled = canSubmit,
                    submitting = uiState.submitting,
                )
            }
        } else {
            {}
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (selectedGroup == null) {
            if (uiState.groups.isEmpty()) {
                FaltetEmptyState(
                    headline = "Inga grupper att kruka upp",
                    subtitle = "Så först några frön i brätten.",
                    modifier = Modifier.padding(padding),
                )
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    items(uiState.groups, key = { "${it.speciesId}_${it.plantedDate}" }) { group ->
                        FaltetListRow(
                            title = group.speciesName ?: "Okänd art",
                            meta = "${formattedDate(group.plantedDate)} · ${group.count} frön i brätte",
                            onClick = {
                                selectedGroup = group
                                countText = group.count.toString()
                            },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Field(
                        label = "Antal att kruka upp",
                        value = countText,
                        onValueChange = { countText = it.filter { c -> c.isDigit() }; countError = null },
                        keyboardType = KeyboardType.Number,
                        required = true,
                        error = countError,
                    )
                }
                item {
                    FaltetImagePicker(
                        label = "Foto (valfri)",
                        value = photoBitmap,
                        onValueChange = { bitmap ->
                            photoBitmap = bitmap
                            imageBase64 = bitmap?.toCompressedBase64()
                        },
                    )
                }
                item {
                    Field(label = "Anteckningar (valfri)", value = notes, onValueChange = { notes = it })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun BatchPotUpScreenPreview_List() {
    val snackbarHostState = remember { SnackbarHostState() }
    val previewGroups = listOf(
        PlantGroupResponse(speciesId = 1L, speciesName = "Cosmos bipinnatus", bedId = null, bedName = null, gardenName = null, plantedDate = "2026-03-15", status = "SEEDED", count = 48),
        PlantGroupResponse(speciesId = 2L, speciesName = "Zinnia elegans", bedId = null, bedName = null, gardenName = null, plantedDate = "2026-03-20", status = "SEEDED", count = 32),
        PlantGroupResponse(speciesId = 3L, speciesName = "Lathyrus odoratus", bedId = null, bedName = null, gardenName = null, plantedDate = "2026-02-28", status = "SEEDED", count = 60),
    )
    FaltetScreenScaffold(
        mastheadLeft = "§ Kruka upp",
        mastheadCenter = "Välj grupp",
        mastheadRight = null,
        bottomBar = {},
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(previewGroups, key = { "${it.speciesId}_${it.plantedDate}" }) { group ->
                FaltetListRow(
                    title = group.speciesName ?: "Okänd art",
                    meta = "${formattedDate(group.plantedDate)} · ${group.count} frön i brätte",
                    onClick = {},
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun BatchPotUpScreenPreview_Detail() {
    val snackbarHostState = remember { SnackbarHostState() }
    FaltetScreenScaffold(
        mastheadLeft = "§ Kruka upp",
        mastheadCenter = "Cosmos bipinnatus",
        mastheadRight = {
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Tillbaka", tint = FaltetAccent, modifier = Modifier.size(18.dp))
            }
        },
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Kruka upp",
                onClick = {},
                enabled = true,
                submitting = false,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Field(
                    label = "Antal att kruka upp",
                    value = "24",
                    onValueChange = {},
                    keyboardType = KeyboardType.Number,
                    required = true,
                    error = null,
                )
            }
            item {
                FaltetImagePicker(
                    label = "Foto (valfri)",
                    value = null,
                    onValueChange = {},
                )
            }
            item {
                Field(label = "Anteckningar (valfri)", value = "", onValueChange = {})
            }
        }
    }
}

private fun formattedDate(date: String?): String {
    if (date == null) return "—"
    return try {
        val parsed = java.time.LocalDate.parse(date.take(10))
        "${parsed.dayOfMonth} ${monthShortSv(parsed.monthValue)}"
    } catch (e: Exception) {
        date
    }
}

private fun monthShortSv(month: Int): String = arrayOf(
    "jan", "feb", "mar", "apr", "maj", "jun",
    "jul", "aug", "sep", "okt", "nov", "dec",
)[month - 1]
