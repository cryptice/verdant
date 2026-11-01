package app.verdant.android.ui.inventory

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.SeedBatchPlanRequest
import app.verdant.android.data.model.SeedBatchPlanResponse
import app.verdant.android.data.model.SeedBatchPlanStep
import app.verdant.android.data.model.SeedInventoryResponse
import app.verdant.android.data.model.SpeciesWorkflowStepResponse
import app.verdant.android.data.repository.SeedInventoryRepository
import app.verdant.android.data.repository.SpeciesRepository
import app.verdant.android.data.repository.WorkflowRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetCheckbox
import app.verdant.android.ui.faltet.FaltetDatePicker
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class SeedPlanState(
    /** The variety's batches that still hold seeds, in the order seeds are drawn from them. */
    val batches: List<SeedInventoryResponse> = emptyList(),
    val selectedIds: List<Long> = emptyList(),
    val loaded: Boolean = false,
    val germinationRate: Int? = null,
    /** Why the variety can't be planned; the form is replaced by this explanation. */
    val workflowProblem: WorkflowProblem? = null,
    val loadFailed: Boolean = false,
    /** What the grower typed; until then the seed count follows the chosen batches. */
    val seedsTyped: String? = null,
    val plants: String = "",
    val readyDate: LocalDate? = null,
    val preview: SeedBatchPlanResponse? = null,
    val error: String? = null,
    val creating: Boolean = false,
    val created: SeedBatchPlanResponse? = null,
) {
    val selected: List<SeedInventoryResponse> get() = batches.filter { it.id in selectedIds }
    val available: Int get() = selected.sumOf { it.quantity }
    val seeds: String get() = seedsTyped ?: if (available > 0) available.toString() else ""

    val inputError: String? get() = seedPlanInputError(selected.size, seeds.toIntOrNull(), plants.toIntOrNull(), available)

    /** The request the inputs make, or null while they are incomplete or wrong. */
    val request: SeedBatchPlanRequest? get() {
        val s = seeds.toIntOrNull()?.takeIf { it >= 1 } ?: return null
        val p = plants.toIntOrNull()?.takeIf { it >= 1 } ?: return null
        val date = readyDate ?: return null
        return if (inputError == null) SeedBatchPlanRequest(s, p, date.toString(), selected.map { it.id }) else null
    }

    /** One task per required step, plus a sowing task for each further batch drawn from. */
    val taskCount: Int get() = preview?.let { p -> p.steps.count { !it.optional } + (p.allocations.size - 1).coerceAtLeast(0) } ?: 0

    fun startsInPast(today: LocalDate): Boolean =
        preview?.steps?.firstOrNull()?.let { LocalDate.parse(it.date).isBefore(today) } ?: false

    val suggestedPlants: Int? get() {
        val rate = germinationRate ?: return null
        val s = seeds.toIntOrNull()?.takeIf { it > 0 } ?: return null
        return s * rate / 100
    }

    fun label(batch: SeedInventoryResponse): String = listOfNotNull(
        batch.providerName ?: "Parti ${batches.indexOf(batch) + 1}",
        batch.expirationDate?.let { "utg. $it" },
    ).joinToString(" · ")
}

/** Why a variety's workflow can't be planned from, mirroring the server's checks. */
sealed interface WorkflowProblem {
    data object Catalogue : WorkflowProblem
    data object None : WorkflowProblem
    data class MissingDays(val step: String) : WorkflowProblem
}

/**
 * Planning counts back through the workflow's main line, so it needs at least one step and a number
 * of days on every step after the first. Side branches don't take part.
 */
internal fun workflowProblem(steps: List<SpeciesWorkflowStepResponse>): WorkflowProblem? {
    val main = steps.filterNot { it.isSideBranch }.sortedBy { it.sortOrder }
    if (main.isEmpty()) return WorkflowProblem.None
    return main.drop(1).firstOrNull { it.daysAfterPrevious == null }?.let { WorkflowProblem.MissingDays(it.name) }
}

/** The mistakes worth catching before asking the server; the same rules the server applies. */
internal fun seedPlanInputError(chosenBatches: Int, seeds: Int?, plants: Int?, available: Int): String? = when {
    chosenBatches == 0 -> "Välj minst ett fröparti."
    seeds != null && seeds > available ->
        if (chosenBatches == 1) "Partiet har bara $available frön." else "De valda partierna har bara $available frön."
    seeds != null && plants != null && plants > seeds -> "Fler plantor än frön går inte."
    else -> null
}

/**
 * Plans a variety's seed batches: "sow this many seeds to have this many plants ready on this date".
 * The seeds are drawn from the chosen batches in order, so a plan can sow more than one batch holds.
 * The server counts back through the variety's workflow; the preview shows the dates, and creating
 * makes the tasks.
 */
@HiltViewModel
class SeedPlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val seedInventoryRepository: SeedInventoryRepository,
    private val speciesRepository: SpeciesRepository,
    private val workflowRepository: WorkflowRepository,
) : ViewModel() {
    private val speciesId: Long = checkNotNull(savedStateHandle.get<Long>("speciesId"))
    private val startBatchId: Long? = savedStateHandle.get<Long>("batchId")?.takeIf { it > 0 }
    private val _uiState = MutableStateFlow(SeedPlanState())
    val uiState = _uiState.asStateFlow()
    private var previewJob: Job? = null

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadFailed = false)
            try {
                val batches = seedInventoryRepository.list(speciesId).filter { it.quantity > 0 }
                // A variety with one batch uses it; with several, the grower picks, starting from the batch opened from.
                val selected = if (batches.size == 1) listOf(batches.first().id) else listOfNotNull(startBatchId?.takeIf { id -> batches.any { it.id == id } })
                // Planning counts back through the variety's workflow, so check it before asking for anything.
                // If the check itself fails, the form shows and the server's own checks still apply.
                val species = runCatching { speciesRepository.get(speciesId) }.getOrNull()
                val problem = if (species?.isSystem == true) WorkflowProblem.Catalogue
                    else runCatching { workflowProblem(workflowRepository.speciesWorkflow(speciesId).steps) }.getOrNull()
                _uiState.value = _uiState.value.copy(
                    batches = batches,
                    selectedIds = selected,
                    germinationRate = species?.germinationRate,
                    workflowProblem = problem,
                    loaded = true,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loadFailed = true)
            }
        }
    }

    fun toggleBatch(id: Long) {
        val ids = _uiState.value.selectedIds
        update(_uiState.value.copy(selectedIds = if (id in ids) ids - id else ids + id))
    }
    fun setSeeds(value: String) = update(_uiState.value.copy(seedsTyped = value.filter { it.isDigit() }))
    fun setPlants(value: String) = update(_uiState.value.copy(plants = value.filter { it.isDigit() }))
    fun setReadyDate(value: LocalDate?) = update(_uiState.value.copy(readyDate = value))

    private fun update(state: SeedPlanState) {
        _uiState.value = state.copy(preview = null, error = null)
        previewJob?.cancel()
        val request = state.request ?: return
        // Wait for typing to settle before asking the server for the dates.
        previewJob = viewModelScope.launch {
            delay(300)
            try {
                val preview = seedInventoryRepository.previewPlan(request)
                _uiState.value = _uiState.value.copy(preview = preview)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = serverMessage(e) ?: "Kunde inte räkna fram schemat")
            }
        }
    }

    fun create() {
        val request = _uiState.value.request ?: return
        if (_uiState.value.creating) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(creating = true, error = null)
            try {
                val created = seedInventoryRepository.createPlan(request)
                _uiState.value = _uiState.value.copy(creating = false, created = created)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(creating = false, error = serverMessage(e) ?: "Kunde inte skapa uppgifterna")
            }
        }
    }

    private fun serverMessage(e: Exception): String? = if (e is HttpException) runCatching {
        JSONObject(e.response()?.errorBody()?.string().orEmpty()).optString("message").takeIf { it.isNotBlank() }
    }.getOrNull() else null
}

private val swedish = Locale("sv", "SE")
private val stepDate = DateTimeFormatter.ofPattern("EEE d MMM", swedish)
private val fullDate = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", swedish)

@Composable
fun SeedPlanScreen(
    onBack: () -> Unit,
    onShowTasks: () -> Unit,
    viewModel: SeedPlanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val created = uiState.created
    val today = LocalDate.now()
    val canCreate = uiState.preview != null && !uiState.startsInPast(today) && !uiState.creating
    val problem = uiState.workflowProblem

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Planera fröparti",
        mastheadRight = {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka", tint = FaltetAccent, modifier = Modifier.size(18.dp))
            }
        },
        bottomBar = {
            if (created == null && uiState.batches.isNotEmpty() && uiState.workflowProblem == null) {
                val count = uiState.taskCount
                FaltetFormSubmitBar(
                    label = if (count == 1) "Skapa 1 uppgift" else "Skapa $count uppgifter",
                    onClick = viewModel::create,
                    enabled = canCreate,
                    submitting = uiState.creating,
                )
            }
        },
    ) { padding ->
        when {
            uiState.loadFailed -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                ConnectionErrorState(onRetry = viewModel::load)
            }
            !uiState.loaded -> FaltetLoadingState(Modifier.padding(padding))
            created != null -> CreatedPlan(created, onShowTasks, onBack, Modifier.padding(padding))
            uiState.batches.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Sorten har inga frön kvar att planera.", color = FaltetForest)
            }
            problem != null -> Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = workflowProblemText(problem, uiState.batches.first().speciesName),
                    fontFamily = FaltetDisplay,
                    fontSize = 17.sp,
                    color = FaltetInk,
                )
                TextButton(onClick = onBack) { Text("Tillbaka", color = FaltetAccent) }
            }
            else -> PlanForm(uiState, viewModel, today, Modifier.padding(padding))
        }
    }
}

@Composable
private fun PlanForm(uiState: SeedPlanState, viewModel: SeedPlanViewModel, today: LocalDate, modifier: Modifier) {
    var pickerOpen by remember { mutableStateOf(uiState.batches.size > 1 && uiState.selectedIds.isEmpty()) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = "Ange hur många frön av ${uiState.batches.first().speciesName} du vill så, hur många plantor de ska ge och när " +
                    "plantorna ska vara klara. Uppgifterna räknas bakåt genom sortens arbetsflöde.",
                fontSize = 14.sp,
                color = FaltetInk,
            )
        }
        item {
            BatchPicker(
                uiState = uiState,
                open = pickerOpen,
                onToggleOpen = { pickerOpen = !pickerOpen },
                onToggleBatch = viewModel::toggleBatch,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Field(
                    label = "Frön att så",
                    value = uiState.seeds,
                    onValueChange = viewModel::setSeeds,
                    keyboardType = KeyboardType.Number,
                    required = true,
                    modifier = Modifier.weight(1f),
                )
                Field(
                    label = "Plantor",
                    value = uiState.plants,
                    onValueChange = viewModel::setPlants,
                    keyboardType = KeyboardType.Number,
                    placeholder = uiState.suggestedPlants?.toString(),
                    required = true,
                    modifier = Modifier.weight(1f),
                )
            }
            val rate = uiState.germinationRate
            val suggested = uiState.suggestedPlants
            if (rate != null && suggested != null && uiState.plants.isEmpty()) {
                Text(
                    text = "Med sortens grobarhet på $rate % ger fröna cirka $suggested plantor.",
                    fontSize = 12.sp,
                    color = FaltetForest,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        item {
            FaltetDatePicker(label = "Klara", value = uiState.readyDate, onValueChange = viewModel::setReadyDate, required = true)
        }
        val message = uiState.inputError?.takeIf { uiState.selectedIds.isNotEmpty() || !pickerOpen } ?: uiState.error
        message?.let { item { Text(it, color = FaltetClay, fontSize = 13.sp) } }
        uiState.preview?.let { preview ->
            item {
                val years = preview.steps.map { it.date.take(4) }.distinct().joinToString("–")
                Text(
                    text = "SCHEMA · $years",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    color = FaltetForest.copy(alpha = 0.7f),
                )
            }
            items(preview.steps) { step ->
                val split = if (step.activityType == "SOW" && preview.allocations.size > 1) {
                    preview.allocations.joinToString(" + ") { a ->
                        val batch = uiState.batches.find { it.id == a.seedBatchId }
                        "${batch?.let(uiState::label) ?: a.seedBatchId}: ${a.seeds} frön"
                    }
                } else null
                PlanStepRow(step, split)
            }
            if (uiState.startsInPast(today)) {
                item {
                    Text(
                        text = "Sådden skulle behöva ske ${LocalDate.parse(preview.steps.first().date).format(fullDate)}, som redan har passerat. Välj ett senare datum.",
                        color = FaltetClay,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** The only batch is named; several are chosen in a dropdown with a checkbox per batch. */
@Composable
private fun BatchPicker(uiState: SeedPlanState, open: Boolean, onToggleOpen: () -> Unit, onToggleBatch: (Long) -> Unit) {
    val label: @Composable (String) -> Unit = { text ->
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest.copy(alpha = 0.7f),
        )
    }
    Column {
        val only = uiState.batches.singleOrNull()
        if (only != null) {
            label("FRÖPARTI")
            Text("${uiState.label(only)} · ${only.quantity} frön", fontSize = 15.sp, color = FaltetInk, modifier = Modifier.padding(top = 6.dp))
            return@Column
        }
        label("FRÖPARTIER")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleOpen)
                .padding(vertical = 10.dp),
        ) {
            val chosen = uiState.selected
            Text(
                text = when (chosen.size) {
                    0 -> "Välj fröpartier"
                    1 -> "1 parti · ${uiState.available} frön"
                    else -> "${chosen.size} partier · ${uiState.available} frön"
                },
                fontFamily = FaltetDisplay,
                fontSize = 18.sp,
                color = FaltetInk,
                modifier = Modifier.weight(1f),
            )
            Text(if (open) "▲" else "▼", fontSize = 10.sp, color = FaltetForest)
        }
        HorizontalDivider(color = FaltetInk)
        if (open) {
            Column(Modifier.padding(top = 4.dp).border(1.dp, FaltetInkLine20)) {
                uiState.batches.forEach { batch ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleBatch(batch.id) }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        FaltetCheckbox(checked = batch.id in uiState.selectedIds, onCheckedChange = { onToggleBatch(batch.id) })
                        Text(uiState.label(batch), fontSize = 14.sp, color = FaltetInk, modifier = Modifier.weight(1f))
                        Text("${batch.quantity} frön", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = FaltetInk)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanStepRow(step: SeedBatchPlanStep, split: String?) {
    val alpha = if (step.optional) 0.55f else 1f
    Column {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(
                text = LocalDate.parse(step.date).format(stepDate),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = FaltetInk.copy(alpha = alpha),
                modifier = Modifier.width(92.dp).padding(top = 3.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(step.name, fontFamily = FaltetDisplay, fontSize = 16.sp, color = FaltetInk.copy(alpha = alpha))
                if (step.optional) {
                    Text("valfritt, ingen uppgift", fontStyle = FontStyle.Italic, fontSize = 12.sp, color = FaltetForest.copy(alpha = alpha))
                }
                split?.let { Text(it, fontSize = 12.sp, color = FaltetForest) }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (step.activityType == "SOW") "${step.count} frön" else "${step.count} plantor",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = FaltetInk.copy(alpha = alpha),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        HorizontalDivider(color = FaltetInkLine20)
    }
}

@Composable
private fun CreatedPlan(plan: SeedBatchPlanResponse, onShowTasks: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val count = plan.taskIds.size
        Text(
            text = (if (count == 1) "1 uppgift skapad." else "$count uppgifter skapade.") +
                " Sådden är ${LocalDate.parse(plan.steps.first().date).format(fullDate)}.",
            fontFamily = FaltetDisplay,
            fontSize = 18.sp,
            color = FaltetInk,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onShowTasks) { Text("Visa uppgifter", color = FaltetAccent) }
            TextButton(onClick = onDone) { Text("Klar", color = FaltetForest) }
        }
    }
}

/** Workflows are assigned in the web app, so the explanation points there. */
internal fun workflowProblemText(problem: WorkflowProblem, name: String): String = when (problem) {
    WorkflowProblem.Catalogue -> "$name är en sort från katalogen och kan inte ha ett eget arbetsflöde. " +
        "Kopiera sorten i webbappen under Arter och tilldela kopian ett arbetsflöde för att kunna planera."
    WorkflowProblem.None -> "$name har inget arbetsflöde, och planen räknas bakåt genom arbetsflödets steg. " +
        "Tilldela sorten ett arbetsflöde i webbappen under Arter för att kunna planera."
    is WorkflowProblem.MissingDays -> "Steget \"${problem.step}\" i sortens arbetsflöde saknar antal dagar, så datumen går inte att räkna fram. " +
        "Ange dagarna för steget i webbappen under Arter."
}
