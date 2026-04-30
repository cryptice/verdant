package app.verdant.android.ui.activity
import app.verdant.android.ui.bed.sortedByNaturalName
import app.verdant.android.data.repository.BedRepository
import app.verdant.android.data.repository.PlantRepository

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import app.verdant.android.data.model.BatchEventRequest
import app.verdant.android.data.model.BedWithGardenResponse
import app.verdant.android.data.model.PlantGroupResponse
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetAccent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatchPlantOutState(
    val isLoading: Boolean = true,
    val submitting: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
    val groups: List<PlantGroupResponse> = emptyList(),
    val beds: List<BedWithGardenResponse> = emptyList(),
)

@HiltViewModel
class BatchPlantOutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val plantRepository: PlantRepository,
    private val bedRepository: BedRepository
) : ViewModel() {
    val targetBedId: Long = savedStateHandle.get<Long>("bedId") ?: 0L
    private val _uiState = MutableStateFlow(BatchPlantOutState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val seeded = plantRepository.groupedByStatus("SEEDED", trayOnly = true)
                val pottedUp = plantRepository.groupedByStatus("POTTED_UP", trayOnly = true)
                val beds = bedRepository.listAll().sortedByNaturalName()
                _uiState.value = BatchPlantOutState(isLoading = false, groups = seeded + pottedUp, beds = beds)
            } catch (e: Exception) {
                _uiState.value = BatchPlantOutState(isLoading = false, error = e.message)
            }
        }
    }

    fun submit(group: PlantGroupResponse, count: Int, targetBed: BedWithGardenResponse) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(submitting = true, error = null)
            try {
                plantRepository.batchEvent(
                    BatchEventRequest(
                        speciesId = group.speciesId,
                        bedId = group.bedId,
                        plantedDate = group.plantedDate,
                        status = group.status,
                        eventType = "PLANTED_OUT",
                        count = count,
                        targetBedId = targetBed.id,
                    )
                )
                _uiState.value = _uiState.value.copy(submitting = false, created = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(submitting = false, error = e.message)
            }
        }
    }
}

@Composable
fun BatchPlantOutScreen(
    onBack: () -> Unit,
    viewModel: BatchPlantOutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedGroup by remember { mutableStateOf<PlantGroupResponse?>(null) }
    var selectedTargetBed by remember { mutableStateOf<BedWithGardenResponse?>(null) }
    var countText by remember { mutableStateOf("") }
    var countError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.groups) {
        if (uiState.groups.size == 1 && selectedGroup == null) {
            selectedGroup = uiState.groups.first()
            countText = uiState.groups.first().count.toString()
        }
    }

    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    val canSubmit = selectedGroup != null
        && selectedTargetBed != null
        && countText.toIntOrNull()?.let { it in 1..selectedGroup!!.count } == true
        && !uiState.submitting

    val submitAction: () -> Unit = {
        val count = countText.toIntOrNull()
        countError = when {
            count == null -> "Antal krävs"
            count !in 1..selectedGroup!!.count -> "Antal måste vara mellan 1 och ${selectedGroup!!.count}"
            else -> null
        }
        if (countError == null && selectedTargetBed != null) {
            viewModel.submit(selectedGroup!!, count!!, selectedTargetBed!!)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = if (selectedGroup == null) "Välj grupp" else (selectedGroup!!.variantName?.let { "${selectedGroup!!.speciesName} – $it" } ?: selectedGroup!!.speciesName ?: "Okänd art"),
        mastheadRight = if (selectedGroup != null) {
            {
                IconButton(
                    onClick = { selectedGroup = null },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Tillbaka",
                        tint = FaltetAccent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        } else null,
        bottomBar = if (selectedGroup != null) {
            {
                FaltetFormSubmitBar(
                    label = "Plantera ut",
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
                    headline = "Inga plantor att plantera ut",
                    subtitle = "Så eller skola om först.",
                    modifier = Modifier.padding(padding),
                )
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    items(
                        uiState.groups,
                        key = { "${it.speciesId}_${it.status}_${it.plantedDate ?: ""}" },
                    ) { group ->
                        FaltetListRow(
                            title = (group.variantName?.let { "${group.speciesName} – $it" } ?: group.speciesName ?: "Okänd art"),
                            meta = "${statusLabelSv(group.status)} · ${group.count} plantor",
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
                    FaltetDropdown(
                        label = "Målbädd",
                        options = uiState.beds,
                        selected = selectedTargetBed,
                        onSelectedChange = { selectedTargetBed = it },
                        labelFor = { "${it.gardenName} · ${it.name}" },
                        searchable = true,
                        required = true,
                    )
                }
                item {
                    Field(
                        label = "Antal att plantera ut",
                        value = countText,
                        onValueChange = { countText = it.filter { c -> c.isDigit() }; countError = null },
                        keyboardType = KeyboardType.Number,
                        required = true,
                        error = countError,
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun BatchPlantOutScreenPreview_List() {
    val snackbarHostState = remember { SnackbarHostState() }
    val previewGroups = listOf(
        PlantGroupResponse(speciesId = 1L, speciesName = "Cosmos bipinnatus", bedId = null, bedName = null, gardenName = null, plantedDate = "2026-03-15", status = "SEEDED", count = 48),
        PlantGroupResponse(speciesId = 2L, speciesName = "Zinnia elegans", bedId = null, bedName = null, gardenName = null, plantedDate = "2026-03-20", status = "POTTED_UP", count = 32),
        PlantGroupResponse(speciesId = 3L, speciesName = "Lathyrus odoratus", bedId = null, bedName = null, gardenName = null, plantedDate = "2026-02-28", status = "SEEDED", count = 60),
    )
    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Välj grupp",
        mastheadRight = null,
        bottomBar = {},
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(previewGroups, key = { "${it.speciesId}_${it.status}_${it.plantedDate ?: ""}" }) { group ->
                FaltetListRow(
                    title = (group.variantName?.let { "${group.speciesName} – $it" } ?: group.speciesName ?: "Okänd art"),
                    meta = "${statusLabelSv(group.status)} · ${group.count} plantor",
                    onClick = {},
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun BatchPlantOutScreenPreview_Detail() {
    val snackbarHostState = remember { SnackbarHostState() }
    val previewBed = BedWithGardenResponse(id = 1L, name = "Bädd A", description = null, gardenId = 1L, gardenName = "Trädgård", boundaryJson = null)
    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Cosmos bipinnatus",
        mastheadRight = {
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Tillbaka",
                    tint = FaltetAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        bottomBar = {
            FaltetFormSubmitBar(
                label = "Plantera ut",
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
                FaltetDropdown(
                    label = "Målbädd",
                    options = listOf(previewBed),
                    selected = previewBed,
                    onSelectedChange = {},
                    labelFor = { "${it.gardenName} · ${it.name}" },
                    searchable = true,
                    required = true,
                )
            }
            item {
                Field(
                    label = "Antal att plantera ut",
                    value = "24",
                    onValueChange = {},
                    keyboardType = KeyboardType.Number,
                    required = true,
                    error = null,
                )
            }
        }
    }
}

private fun statusLabelSv(status: String?): String = when (status) {
    "SEEDED" -> "Sådd"
    "POTTED_UP" -> "Omskolad"
    "PLANTED_OUT", "GROWING" -> "Utplanterad"
    "HARVESTED" -> "Skördad"
    null -> "—"
    else -> status
}
