package app.verdant.android.ui.season

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import app.verdant.android.data.model.CreateSeasonRequest
import app.verdant.android.data.model.SeasonResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.theme.verdantTopAppBarColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Year
import javax.inject.Inject

data class SeasonSelectorState(
    val isLoading: Boolean = true,
    val seasons: List<SeasonResponse> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class SeasonSelectorViewModel @Inject constructor(
    private val repo: GardenRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SeasonSelectorState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val seasons = repo.getSeasons()
                _uiState.value = _uiState.value.copy(isLoading = false, seasons = seasons)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun createSeason(request: CreateSeasonRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                repo.createSeason(request)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun updateSeason(id: Long, fields: Map<String, Any?>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            try {
                repo.updateSeason(id, fields)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun deleteSeason(id: Long) {
        viewModelScope.launch {
            try {
                repo.deleteSeason(id)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonSelectorScreen(
    onBack: () -> Unit,
    viewModel: SeasonSelectorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingSeason by remember { mutableStateOf<SeasonResponse?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.seasons)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = verdantTopAppBarColors()
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingSeason = null; showDialog = true }) {
                Icon(Icons.Default.Add, stringResource(R.string.new_season))
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null && uiState.seasons.isEmpty() -> {
                ConnectionErrorState(
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.seasons.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.no_seasons), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.seasons, key = { it.id }) { season ->
                        SeasonCard(
                            season = season,
                            onClick = { editingSeason = season; showDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        SeasonFormDialog(
            season = editingSeason,
            saving = uiState.saving,
            onDismiss = { showDialog = false },
            onSave = { name, year, lastFrost, firstFrost ->
                if (editingSeason != null) {
                    viewModel.updateSeason(editingSeason!!.id, buildMap {
                        put("name", name)
                        put("year", year)
                        put("lastFrostDate", lastFrost.ifBlank { null })
                        put("firstFrostDate", firstFrost.ifBlank { null })
                    })
                } else {
                    viewModel.createSeason(CreateSeasonRequest(
                        name = name,
                        year = year,
                        lastFrostDate = lastFrost.ifBlank { null },
                        firstFrostDate = firstFrost.ifBlank { null },
                    ))
                }
                showDialog = false
            },
            onDelete = if (editingSeason != null) {
                { viewModel.deleteSeason(editingSeason!!.id); showDialog = false }
            } else null
        )
    }
}

@Composable
private fun SeasonCard(season: SeasonResponse, onClick: () -> Unit) {
    val borderColor = if (season.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (season.isActive) 2.dp else 1.dp

    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(season.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (season.isActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            stringResource(R.string.active_season),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("${season.year}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            if (season.lastFrostDate != null || season.firstFrostDate != null) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    season.lastFrostDate?.let {
                        Text("${stringResource(R.string.last_frost)}: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    season.firstFrostDate?.let {
                        Text("${stringResource(R.string.first_frost)}: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonFormDialog(
    season: SeasonResponse?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, year: Int, lastFrost: String, firstFrost: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(season?.name ?: "") }
    var year by remember { mutableStateOf(season?.year?.toString() ?: Year.now().value.toString()) }
    var lastFrost by remember { mutableStateOf(season?.lastFrostDate ?: "") }
    var firstFrost by remember { mutableStateOf(season?.firstFrostDate ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (season != null) R.string.edit_season else R.string.new_season)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.season_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.season_year)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lastFrost,
                    onValueChange = { lastFrost = it },
                    label = { Text(stringResource(R.string.last_frost)) },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = firstFrost,
                    onValueChange = { firstFrost = it },
                    label = { Text(stringResource(R.string.first_frost)) },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, year.toIntOrNull() ?: Year.now().value, lastFrost, firstFrost) },
                enabled = name.isNotBlank() && year.isNotBlank() && !saving
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
