package app.verdant.android.ui.workflow

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CompleteWorkflowStepRequest
import app.verdant.android.data.model.SpeciesWorkflowResponse
import app.verdant.android.data.model.SpeciesWorkflowStepResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.theme.verdantTopAppBarColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WorkflowProgress"

data class WorkflowProgressState(
    val isLoading: Boolean = true,
    val speciesName: String? = null,
    val workflow: SpeciesWorkflowResponse? = null,
    val plantCountsByStep: Map<Long, List<Long>> = emptyMap(),
    // For each step, a map of bedId -> list of plantIds in that bed.
    // Plants without a bed are excluded (can't be fertilized from workflow).
    val plantsByBedByStep: Map<Long, Map<Long, List<Long>>> = emptyMap(),
    val error: String? = null,
    val completingStepId: Long? = null,
    val completionSuccess: Boolean = false,
)

@HiltViewModel
class WorkflowProgressViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: GardenRepository,
) : ViewModel() {
    val speciesId: Long = savedStateHandle.get<Long>("speciesId")!!
    private val _uiState = MutableStateFlow(WorkflowProgressState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val workflow = repo.getSpeciesWorkflow(speciesId)
                // Fetch plant counts for each step
                val plantCounts = mutableMapOf<Long, List<Long>>()
                for (step in workflow.steps) {
                    try {
                        val plantIds = repo.getPlantsAtStep(step.id, speciesId)
                        plantCounts[step.id] = plantIds
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to fetch plants for step ${step.id}", e)
                        plantCounts[step.id] = emptyList()
                    }
                }
                // Build a plantId -> bedId map (bedId nullable; drop plants without a bed).
                val allPlantIds = plantCounts.values.flatten().toSet()
                val plantBed: Map<Long, Long> = if (allPlantIds.isEmpty()) emptyMap() else {
                    runCatching { repo.getAllPlants() }
                        .getOrDefault(emptyList())
                        .filter { it.id in allPlantIds && it.bedId != null }
                        .associate { it.id to it.bedId!! }
                }
                val plantsByBedByStep: Map<Long, Map<Long, List<Long>>> = plantCounts.mapValues { (_, ids) ->
                    ids.mapNotNull { pid -> plantBed[pid]?.let { bed -> bed to pid } }
                        .groupBy({ it.first }, { it.second })
                }
                _uiState.value = WorkflowProgressState(
                    isLoading = false,
                    speciesName = workflow.templateName,
                    workflow = workflow,
                    plantCountsByStep = plantCounts,
                    plantsByBedByStep = plantsByBedByStep,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load workflow", e)
                _uiState.value = WorkflowProgressState(isLoading = false, error = e.message)
            }
        }
    }

    fun completeStep(stepId: Long, plantIds: List<Long>, notes: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(completingStepId = stepId, completionSuccess = false)
            try {
                val response = repo.completeWorkflowStep(
                    stepId,
                    CompleteWorkflowStepRequest(plantIds = plantIds, notes = notes)
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(completingStepId = null, completionSuccess = true)
                    load() // Refresh data
                } else {
                    _uiState.value = _uiState.value.copy(
                        completingStepId = null,
                        error = "Failed to complete step"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to complete step", e)
                _uiState.value = _uiState.value.copy(
                    completingStepId = null,
                    error = e.message
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowProgressScreen(
    onBack: () -> Unit,
    onApplySupplyStep: ((bedId: Long, stepId: Long, plantIds: List<Long>, supplyTypeId: Long?, quantity: Double?) -> Unit)? = null,
    viewModel: WorkflowProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmStep by remember { mutableStateOf<SpeciesWorkflowStepResponse?>(null) }
    var completionNotes by remember { mutableStateOf("") }
    var bedPickerStep by remember { mutableStateOf<SpeciesWorkflowStepResponse?>(null) }

    // Central dispatcher: if single bed, route directly; if multi-bed, open picker.
    fun handleApplySupply(step: SpeciesWorkflowStepResponse) {
        if (onApplySupplyStep == null) { confirmStep = step; return }
        val byBed = uiState.plantsByBedByStep[step.id].orEmpty()
        when (byBed.size) {
            0 -> confirmStep = step // no plants in beds — fall back to plain completion
            1 -> {
                val (bedId, plantIds) = byBed.entries.single()
                onApplySupplyStep(bedId, step.id, plantIds, step.suggestedSupplyTypeId, step.suggestedQuantity)
            }
            else -> bedPickerStep = step
        }
    }

    // Show confirmation dialog
    if (confirmStep != null) {
        val step = confirmStep!!
        val plantIds = uiState.plantCountsByStep[step.id] ?: emptyList()
        AlertDialog(
            onDismissRequest = { confirmStep = null; completionNotes = "" },
            title = { Text("Complete Step") },
            text = {
                Column {
                    Text("Complete \"${step.name}\" for ${plantIds.size} plant(s)?")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = completionNotes,
                        onValueChange = { completionNotes = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.completeStep(
                            step.id,
                            plantIds,
                            completionNotes.ifBlank { null }
                        )
                        confirmStep = null
                        completionNotes = ""
                    }
                ) { Text("Complete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmStep = null; completionNotes = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bed picker for APPLIED_SUPPLY steps whose plants span multiple beds.
    if (bedPickerStep != null && onApplySupplyStep != null) {
        val step = bedPickerStep!!
        val byBed = uiState.plantsByBedByStep[step.id].orEmpty()
        AlertDialog(
            onDismissRequest = { bedPickerStep = null },
            title = { Text(step.name) },
            text = {
                Column {
                    Text("Plants span ${byBed.size} beds — pick one to fertilize:")
                    Spacer(Modifier.height(8.dp))
                    byBed.forEach { (bedId, plants) ->
                        TextButton(
                            onClick = {
                                onApplySupplyStep(bedId, step.id, plants, step.suggestedSupplyTypeId, step.suggestedQuantity)
                                bedPickerStep = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Bed $bedId — ${plants.size} plant(s)")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { bedPickerStep = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workflow Progress") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = verdantTopAppBarColors()
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null && uiState.workflow == null -> {
                app.verdant.android.ui.common.ConnectionErrorState(
                    onRetry = { viewModel.load() },
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.workflow != null -> {
                val workflow = uiState.workflow!!
                val mainSteps = workflow.steps.filter { !it.isSideBranch }.sortedBy { it.sortOrder }
                val sideBranches = workflow.steps.filter { it.isSideBranch }
                    .groupBy { it.sideBranchName ?: "Side Branch" }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Species / template name header
                    item {
                        workflow.templateName?.let { name ->
                            Text(
                                name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    // Main flow header
                    item {
                        Text(
                            "Main Flow",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    items(mainSteps, key = { it.id }) { step ->
                        val plantIds = uiState.plantCountsByStep[step.id] ?: emptyList()
                        WorkflowStepCard(
                            step = step,
                            plantIds = plantIds,
                            isCompleting = uiState.completingStepId == step.id,
                            onComplete = {
                                if (step.eventType == "APPLIED_SUPPLY") handleApplySupply(step)
                                else confirmStep = step
                            },
                        )
                    }

                    // Side branches
                    sideBranches.forEach { (branchName, steps) ->
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                branchName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }

                        items(steps.sortedBy { it.sortOrder }, key = { it.id }) { step ->
                            val plantIds = uiState.plantCountsByStep[step.id] ?: emptyList()
                            WorkflowStepCard(
                                step = step,
                                plantIds = plantIds,
                                isCompleting = uiState.completingStepId == step.id,
                                onComplete = {
                                    if (step.eventType == "APPLIED_SUPPLY") handleApplySupply(step)
                                    else confirmStep = step
                                },
                            )
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun WorkflowStepCard(
    step: SpeciesWorkflowStepResponse,
    plantIds: List<Long>,
    isCompleting: Boolean,
    onComplete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (plantIds.isEmpty()) Icons.Default.RadioButtonUnchecked
                    else Icons.Default.RadioButtonChecked,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (plantIds.isNotEmpty()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    step.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            // Metadata row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 28.dp, top = 4.dp)
            ) {
                step.eventType?.let { eventType ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(eventType.replace("_", " "), fontSize = 11.sp) },
                        modifier = Modifier.height(24.dp),
                    )
                }
                if ((step.daysAfterPrevious ?: 0) > 0) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("+${step.daysAfterPrevious}d", fontSize = 11.sp) },
                        modifier = Modifier.height(24.dp),
                    )
                }
                if (step.isOptional) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Optional", fontSize = 11.sp) },
                        modifier = Modifier.height(24.dp),
                    )
                }
            }

            // Description
            step.description?.let { desc ->
                Text(
                    desc,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 28.dp, top = 4.dp)
                )
            }

            // Plant count and complete button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(start = 28.dp, top = 8.dp)
            ) {
                Text(
                    "${plantIds.size} plant(s) at this step",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                if (plantIds.isNotEmpty()) {
                    if (isCompleting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        FilledTonalButton(
                            onClick = onComplete,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Complete for ${plantIds.size}", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
