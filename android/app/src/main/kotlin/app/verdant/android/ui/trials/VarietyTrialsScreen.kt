package app.verdant.android.ui.trials

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.*
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.theme.verdantTopAppBarColors
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
                val species = repo.getSpecies()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VarietyTrialsScreen(
    onBack: () -> Unit,
    viewModel: TrialsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<VarietyTrialResponse?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.variety_trials)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = verdantTopAppBarColors(),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, stringResource(R.string.new_trial))
            }
        },
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            uiState.error != null && uiState.items.isEmpty() -> {
                ConnectionErrorState(
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(padding),
                )
            }

            uiState.items.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_trials),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.items, key = { it.id }) { t ->
                        val seasonName = uiState.seasons.find { it.id == t.seasonId }?.name
                        TrialCard(t, seasonName, onClick = { editing = t; showDialog = true })
                    }
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

@Composable
private fun TrialCard(trial: VarietyTrialResponse, seasonName: String?, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(trial.speciesName ?: "Species #${trial.speciesId}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        verdictLabel(trial.verdict),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            if (seasonName != null) {
                Spacer(Modifier.height(2.dp))
                Text(seasonName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            trial.qualityScore?.let {
                Spacer(Modifier.height(4.dp))
                Text("${stringResource(R.string.quality_score)}: $it/10", fontSize = 13.sp)
            }
            trial.customerReception?.let {
                Text("${stringResource(R.string.customer_reception)}: ${receptionLabel(it)}", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun verdictLabel(v: String): String = when (v) {
    Verdict.KEEP -> stringResource(R.string.verdict_keep)
    Verdict.EXPAND -> stringResource(R.string.verdict_expand)
    Verdict.REDUCE -> stringResource(R.string.verdict_reduce)
    Verdict.DROP -> stringResource(R.string.verdict_drop)
    else -> stringResource(R.string.verdict_undecided)
}

@Composable
private fun receptionLabel(r: String): String = when (r) {
    Reception.LOVED -> stringResource(R.string.reception_loved)
    Reception.LIKED -> stringResource(R.string.reception_liked)
    Reception.NEUTRAL -> stringResource(R.string.reception_neutral)
    Reception.DISLIKED -> stringResource(R.string.reception_disliked)
    else -> r
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
            Column(
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
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
                            label = { Text(verdictLabel(v), fontSize = 12.sp) },
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
