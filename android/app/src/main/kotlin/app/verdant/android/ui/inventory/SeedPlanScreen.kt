package app.verdant.android.ui.inventory

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
import app.verdant.android.data.repository.SeedInventoryRepository
import app.verdant.android.data.repository.SpeciesRepository
import app.verdant.android.ui.common.ConnectionErrorState
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
    val batch: SeedInventoryResponse? = null,
    val germinationRate: Int? = null,
    val loadFailed: Boolean = false,
    val seeds: String = "",
    val plants: String = "",
    val readyDate: LocalDate? = null,
    val preview: SeedBatchPlanResponse? = null,
    val error: String? = null,
    val creating: Boolean = false,
    val created: SeedBatchPlanResponse? = null,
) {
    val inputError: String? get() = batch?.let { seedPlanInputError(seeds.toIntOrNull(), plants.toIntOrNull(), it.quantity) }

    /** The request the inputs make, or null while they are incomplete or wrong. */
    val request: SeedBatchPlanRequest? get() {
        val s = seeds.toIntOrNull()?.takeIf { it >= 1 } ?: return null
        val p = plants.toIntOrNull()?.takeIf { it >= 1 } ?: return null
        val date = readyDate ?: return null
        return if (inputError == null) SeedBatchPlanRequest(s, p, date.toString()) else null
    }

    fun startsInPast(today: LocalDate): Boolean =
        preview?.steps?.firstOrNull()?.let { LocalDate.parse(it.date).isBefore(today) } ?: false

    val suggestedPlants: Int? get() {
        val rate = germinationRate ?: return null
        val s = seeds.toIntOrNull()?.takeIf { it > 0 } ?: return null
        return s * rate / 100
    }
}

/** The mistakes worth catching before asking the server; the same rules the server applies. */
internal fun seedPlanInputError(seeds: Int?, plants: Int?, batchQuantity: Int): String? = when {
    seeds != null && seeds > batchQuantity -> "Partiet har bara $batchQuantity frön."
    seeds != null && plants != null && plants > seeds -> "Fler plantor än frön går inte."
    else -> null
}

/**
 * Plans a seed batch: "sow this many seeds to have this many plants ready on this date". The server
 * counts back through the variety's workflow; the preview shows the dates, and creating makes the tasks.
 */
@HiltViewModel
class SeedPlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val seedInventoryRepository: SeedInventoryRepository,
    private val speciesRepository: SpeciesRepository,
) : ViewModel() {
    private val batchId: Long = checkNotNull(savedStateHandle.get<Long>("batchId"))
    private val _uiState = MutableStateFlow(SeedPlanState())
    val uiState = _uiState.asStateFlow()
    private var previewJob: Job? = null

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadFailed = false)
            try {
                val batch = seedInventoryRepository.list().first { it.id == batchId }
                _uiState.value = _uiState.value.copy(batch = batch, seeds = batch.quantity.toString())
                val rate = runCatching { speciesRepository.get(batch.speciesId).germinationRate }.getOrNull()
                _uiState.value = _uiState.value.copy(germinationRate = rate)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loadFailed = true)
            }
        }
    }

    fun setSeeds(value: String) = update(_uiState.value.copy(seeds = value.filter { it.isDigit() }))
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
                val preview = seedInventoryRepository.previewPlan(batchId, request)
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
                val created = seedInventoryRepository.createPlan(batchId, request)
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

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Planera fröparti",
        mastheadRight = {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka", tint = FaltetAccent, modifier = Modifier.size(18.dp))
            }
        },
        bottomBar = {
            if (created == null && uiState.batch != null) {
                val count = uiState.preview?.steps?.count { !it.optional } ?: 0
                FaltetFormSubmitBar(
                    label = if (count == 1) "Skapa 1 uppgift" else "Skapa $count uppgifter",
                    onClick = viewModel::create,
                    enabled = canCreate,
                    submitting = uiState.creating,
                )
            }
        },
    ) { padding ->
        val batch = uiState.batch
        when {
            uiState.loadFailed -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                ConnectionErrorState(onRetry = viewModel::load)
            }
            batch == null -> FaltetLoadingState(Modifier.padding(padding))
            created != null -> CreatedPlan(created, onShowTasks, onBack, Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Text(
                        text = "Ange hur många frön av ${batch.speciesName} du vill så, hur många plantor de ska ge och när " +
                            "plantorna ska vara klara. Uppgifterna räknas bakåt genom sortens arbetsflöde. Partiet har ${batch.quantity} frön.",
                        fontSize = 14.sp,
                        color = FaltetInk,
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
                (uiState.inputError ?: uiState.error)?.let { message ->
                    item { Text(message, color = FaltetClay, fontSize = 13.sp) }
                }
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
                    items(preview.steps) { step -> PlanStepRow(step) }
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
    }
}

@Composable
private fun PlanStepRow(step: SeedBatchPlanStep) {
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
