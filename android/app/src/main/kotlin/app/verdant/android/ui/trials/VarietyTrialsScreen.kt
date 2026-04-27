package app.verdant.android.ui.trials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.CreateVarietyTrialRequest
import app.verdant.android.data.model.Reception
import app.verdant.android.data.model.SeasonResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.UpdateVarietyTrialRequest
import app.verdant.android.data.model.VarietyTrialResponse
import app.verdant.android.data.model.Verdict
import app.verdant.android.data.model.sortedBySwedishName
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.Chip
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetTone
import app.verdant.android.ui.theme.FaltetCream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Year
import javax.inject.Inject

data class TrialsState(
    val isLoading: Boolean = true,
    val items: List<VarietyTrialResponse> = emptyList(),
    val species: List<SpeciesResponse> = emptyList(),
    val seasons: List<SeasonResponse> = emptyList(),
    val activeSeasonId: Long? = null,
    val error: String? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class TrialsViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrialsState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val seasons = repo.getSeasons()
                val active = seasons.firstOrNull { it.isActive }
                val items = repo.getVarietyTrials(active?.id)
                val species = repo.getSpecies().sortedBySwedishName()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = items,
                    species = species,
                    seasons = seasons,
                    activeSeasonId = active?.id,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun create(request: CreateVarietyTrialRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                repo.createVarietyTrial(request)
                _uiState.value = _uiState.value.copy(saving = false)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun update(id: Long, request: UpdateVarietyTrialRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                repo.updateVarietyTrial(id, request)
                _uiState.value = _uiState.value.copy(saving = false)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                repo.deleteVarietyTrial(id)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

private fun verdictTone(verdict: String): FaltetTone = when (verdict) {
    Verdict.KEEP -> FaltetTone.Sage
    Verdict.EXPAND -> FaltetTone.Clay
    Verdict.DROP -> FaltetTone.Berry
    Verdict.REDUCE -> FaltetTone.Mustard
    else -> FaltetTone.Forest
}

private fun verdictLabelSv(verdict: String): String = when (verdict) {
    Verdict.KEEP -> "Behåll"
    Verdict.EXPAND -> "Utöka"
    Verdict.REDUCE -> "Minska"
    Verdict.DROP -> "Avveckla"
    else -> "Ej beslutad"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VarietyTrialsScreen(
    onBack: () -> Unit,
    viewModel: TrialsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<VarietyTrialResponse?>(null) }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Försök",
        fab = {
            FaltetFab(
                onClick = { editing = null; showDialog = true },
                contentDescription = stringResource(R.string.new_trial),
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))

            uiState.error != null && uiState.items.isEmpty() -> ConnectionErrorState(
                onRetry = { viewModel.refresh() },
                modifier = Modifier.padding(padding),
            )

            uiState.items.isEmpty() -> FaltetEmptyState(
                headline = "Inga sortförsök",
                subtitle = "Starta ditt första försök.",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(uiState.items, key = { it.id }) { trial ->
                    val year = uiState.seasons.find { it.id == trial.seasonId }?.year
                    val meta = if (year != null) "$year · ${trial.speciesName ?: "Art #${trial.speciesId}"}"
                               else trial.speciesName ?: "Art #${trial.speciesId}"
                    FaltetListRow(
                        title = trial.speciesName ?: "Art #${trial.speciesId}",
                        meta = meta,
                        leading = null,
                        stat = {
                            Chip(
                                text = verdictLabelSv(trial.verdict),
                                tone = verdictTone(trial.verdict),
                            )
                        },
                        actions = null,
                        onClick = { editing = trial; showDialog = true },
                    )
                }
            }
        }
    }

    if (showDialog) {
        TrialDialog(
            existing = editing,
            species = uiState.species,
            seasons = uiState.seasons,
            defaultSeasonId = uiState.activeSeasonId,
            saving = uiState.saving,
            onDismiss = { showDialog = false },
            onSave = { payload ->
                if (editing != null) {
                    viewModel.update(
                        editing!!.id,
                        UpdateVarietyTrialRequest(
                            seasonId = payload.seasonId,
                            speciesId = payload.speciesId,
                            qualityScore = payload.qualityScore,
                            customerReception = payload.customerReception,
                            verdict = payload.verdict,
                            notes = payload.notes,
                        ),
                    )
                } else {
                    viewModel.create(payload)
                }
                showDialog = false
            },
            onDelete = editing?.let { e ->
                { viewModel.delete(e.id); showDialog = false }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TrialDialog(
    existing: VarietyTrialResponse?,
    species: List<SpeciesResponse>,
    seasons: List<SeasonResponse>,
    defaultSeasonId: Long?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (CreateVarietyTrialRequest) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var speciesId by remember { mutableStateOf(existing?.speciesId) }
    var seasonId by remember { mutableStateOf(existing?.seasonId ?: defaultSeasonId) }
    var speciesExpanded by remember { mutableStateOf(false) }
    var seasonExpanded by remember { mutableStateOf(false) }
    var qualityScore by remember { mutableStateOf(existing?.qualityScore?.toString() ?: "") }
    var reception by remember { mutableStateOf(existing?.customerReception) }
    var verdict by remember { mutableStateOf(existing?.verdict ?: Verdict.UNDECIDED) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var year by remember { mutableStateOf(seasons.find { it.id == (existing?.seasonId ?: defaultSeasonId) }?.year?.toString() ?: Year.now().value.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing != null) R.string.edit_trial else R.string.new_trial)) },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = speciesExpanded,
                    onExpandedChange = { speciesExpanded = it },
                ) {
                    OutlinedTextField(
                        value = species.find { it.id == speciesId }?.let { it.commonNameSv ?: it.commonName } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.species)) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(speciesExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = speciesExpanded,
                        onDismissRequest = { speciesExpanded = false },
                    ) {
                        species.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.commonNameSv ?: s.commonName) },
                                onClick = { speciesId = s.id; speciesExpanded = false },
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = seasonExpanded,
                    onExpandedChange = { seasonExpanded = it },
                ) {
                    OutlinedTextField(
                        value = seasons.find { it.id == seasonId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.seasons)) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(seasonExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = seasonExpanded,
                        onDismissRequest = { seasonExpanded = false },
                    ) {
                        seasons.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = { seasonId = s.id; year = s.year.toString(); seasonExpanded = false },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.trial_year)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    readOnly = true,
                )

                OutlinedTextField(
                    value = qualityScore,
                    onValueChange = { qualityScore = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text(stringResource(R.string.quality_score)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )

                Text(stringResource(R.string.customer_reception), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    (listOf<String?>(null) + Reception.values).forEach { r ->
                        FilterChip(
                            selected = r == reception,
                            onClick = { reception = r },
                            label = { Text(if (r == null) stringResource(R.string.none) else receptionLabel(r), fontSize = 12.sp) },
                        )
                    }
                }

                Text(stringResource(R.string.verdict), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Verdict.values.forEach { v ->
                        FilterChip(
                            selected = v == verdict,
                            onClick = { verdict = v },
                            label = { Text(verdictLabelSv(v), fontSize = 12.sp) },
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.delete)) }
                }
            }
        },
        confirmButton = {
            val valid = speciesId != null && seasonId != null
            Button(
                enabled = valid && !saving,
                onClick = {
                    onSave(
                        CreateVarietyTrialRequest(
                            seasonId = seasonId!!,
                            speciesId = speciesId!!,
                            qualityScore = qualityScore.toIntOrNull(),
                            customerReception = reception,
                            verdict = verdict,
                            notes = notes.ifBlank { null },
                        )
                    )
                },
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun receptionLabel(r: String): String = when (r) {
    Reception.LOVED -> stringResource(R.string.reception_loved)
    Reception.LIKED -> stringResource(R.string.reception_liked)
    Reception.NEUTRAL -> stringResource(R.string.reception_neutral)
    Reception.DISLIKED -> stringResource(R.string.reception_disliked)
    else -> r
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun VarietyTrialsScreenPreview() {
    val trial = VarietyTrialResponse(
        id = 1L,
        seasonId = 1L,
        speciesId = 1L,
        speciesName = "Cosmos bipinnatus",
        bedId = null,
        plantCount = null,
        stemYield = null,
        avgStemLengthCm = null,
        avgVaseLifeDays = null,
        qualityScore = 8,
        customerReception = null,
        verdict = Verdict.EXPAND,
        notes = null,
        createdAt = "2025-01-01T00:00:00Z",
    )
    FaltetListRow(
        title = trial.speciesName ?: "Art #${trial.speciesId}",
        meta = "2025 · ${trial.speciesName ?: "Art #${trial.speciesId}"}",
        leading = null,
        stat = {
            Chip(
                text = verdictLabelSv(trial.verdict),
                tone = verdictTone(trial.verdict),
            )
        },
        actions = null,
        onClick = {},
    )
}
