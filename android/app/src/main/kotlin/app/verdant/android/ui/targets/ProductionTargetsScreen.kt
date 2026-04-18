package app.verdant.android.ui.targets

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import app.verdant.android.data.model.ProductionForecastResponse
import app.verdant.android.data.model.ProductionTargetResponse
import app.verdant.android.data.model.SeasonResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.theme.verdantTopAppBarColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class TargetsState(
    val isLoading: Boolean = true,
    val items: List<ProductionTargetResponse> = emptyList(),
    val species: List<SpeciesResponse> = emptyList(),
    val seasons: List<SeasonResponse> = emptyList(),
    val activeSeasonId: Long? = null,
    val forecasts: Map<Long, ProductionForecastResponse> = emptyMap(),
    val forecastLoadingId: Long? = null,
    val error: String? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class TargetsViewModel @Inject constructor(
    private val repo: GardenRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TargetsState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val seasons = repo.getSeasons()
                val active = seasons.firstOrNull { it.isActive }
                val items = repo.getProductionTargets(active?.id)
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

    fun create(payload: Map<String, Any?>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                repo.createProductionTarget(payload)
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
                repo.deleteProductionTarget(id)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadForecast(id: Long) {
        if (_uiState.value.forecasts.containsKey(id)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(forecastLoadingId = id)
            try {
                val forecast = repo.getProductionForecast(id)
                _uiState.value = _uiState.value.copy(
                    forecastLoadingId = null,
                    forecasts = _uiState.value.forecasts + (id to forecast),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(forecastLoadingId = null, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionTargetsScreen(
    onBack: () -> Unit,
    viewModel: TargetsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.production_targets)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = verdantTopAppBarColors(),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, stringResource(R.string.new_target))
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
                        stringResource(R.string.no_targets),
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
                        TargetCard(
                            target = t,
                            forecast = uiState.forecasts[t.id],
                            isLoadingForecast = uiState.forecastLoadingId == t.id,
                            onExpand = { viewModel.loadForecast(t.id) },
                            onDelete = { viewModel.delete(t.id) },
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        TargetDialog(
            species = uiState.species,
            seasons = uiState.seasons,
            defaultSeasonId = uiState.activeSeasonId,
            saving = uiState.saving,
            onDismiss = { showDialog = false },
            onSave = { payload ->
                viewModel.create(payload)
                showDialog = false
            },
        )
    }
}

@Composable
private fun TargetCard(
    target: ProductionTargetResponse,
    forecast: ProductionForecastResponse?,
    isLoadingForecast: Boolean,
    onExpand: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable {
            expanded = !expanded
            if (expanded) onExpand()
        },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(target.speciesName ?: "Species #${target.speciesId}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "${target.stemsPerWeek} ${stringResource(R.string.stems_per_week).lowercase()}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${target.startDate} → ${target.endDate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.forecast), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    when {
                        isLoadingForecast -> {
                            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                        forecast != null -> {
                            ForecastRow(stringResource(R.string.total_stems_needed), forecast.totalStemsNeeded.toString())
                            ForecastRow(stringResource(R.string.plants_needed), forecast.plantsNeeded.toString())
                            ForecastRow(stringResource(R.string.seeds_needed), forecast.seedsNeeded.toString())
                            forecast.suggestedSowDate?.let {
                                ForecastRow(stringResource(R.string.suggested_sow_date), it)
                            }
                            if (forecast.warnings.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(stringResource(R.string.warnings), fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                forecast.warnings.forEach { w ->
                                    Text("• $w", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.delete)) }
                }
            }
        }
    }
}

@Composable
private fun ForecastRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetDialog(
    species: List<SpeciesResponse>,
    seasons: List<SeasonResponse>,
    defaultSeasonId: Long?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any?>) -> Unit,
) {
    var speciesId by remember { mutableStateOf<Long?>(null) }
    var seasonId by remember { mutableStateOf(defaultSeasonId) }
    var speciesExpanded by remember { mutableStateOf(false) }
    var seasonExpanded by remember { mutableStateOf(false) }
    var stemsPerWeek by remember { mutableStateOf("100") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusMonths(3).toString()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    @Composable
    fun datePickerFor(current: String, onPicked: (String) -> Unit, onClose: () -> Unit) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = current.takeIf { it.isNotBlank() }?.let {
                runCatching { LocalDate.parse(it).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() }.getOrNull()
            }
        )
        DatePickerDialog(
            onDismissRequest = onClose,
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onPicked(Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString())
                    }
                    onClose()
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = state) }
    }

    if (showStartPicker) datePickerFor(startDate, { startDate = it }, { showStartPicker = false })
    if (showEndPicker) datePickerFor(endDate, { endDate = it }, { showEndPicker = false })

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_target)) },
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
                                onClick = { seasonId = s.id; seasonExpanded = false },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = stemsPerWeek,
                    onValueChange = { stemsPerWeek = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.stems_per_week)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text(stringResource(R.string.delivery_window_start)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showStartPicker = true }) {
                            Icon(Icons.Default.CalendarMonth, null)
                        }
                    },
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text(stringResource(R.string.delivery_window_end)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showEndPicker = true }) {
                            Icon(Icons.Default.CalendarMonth, null)
                        }
                    },
                )
            }
        },
        confirmButton = {
            val valid = speciesId != null && seasonId != null && stemsPerWeek.toIntOrNull() != null
                && startDate.isNotBlank() && endDate.isNotBlank()
            Button(
                enabled = valid && !saving,
                onClick = {
                    onSave(
                        mapOf(
                            "seasonId" to seasonId,
                            "speciesId" to speciesId,
                            "stemsPerWeek" to (stemsPerWeek.toIntOrNull() ?: 0),
                            "startDate" to startDate,
                            "endDate" to endDate,
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
