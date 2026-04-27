package app.verdant.android.ui.season

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateSeasonRequest
import app.verdant.android.data.model.SeasonResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetDatePicker
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetClay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showFormDialog by remember { mutableStateOf(false) }
    var editingSeason by remember { mutableStateOf<SeasonResponse?>(null) }
    var formName by remember { mutableStateOf("") }
    var formYear by remember { mutableStateOf("") }
    var formLastFrost by remember { mutableStateOf<LocalDate?>(null) }
    var formFirstFrost by remember { mutableStateOf<LocalDate?>(null) }

    val openCreate: () -> Unit = {
        editingSeason = null
        formName = ""
        formYear = java.time.Year.now().value.toString()
        formLastFrost = null
        formFirstFrost = null
        showFormDialog = true
    }

    val openEdit: (SeasonResponse) -> Unit = { season ->
        editingSeason = season
        formName = season.name
        formYear = season.year.toString()
        formLastFrost = season.lastFrostDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        formFirstFrost = season.firstFrostDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        showFormDialog = true
    }

    val closeDialog: () -> Unit = {
        showFormDialog = false
        editingSeason = null
    }

    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = closeDialog,
            title = { Text(if (editingSeason != null) "Redigera säsong" else "Ny säsong") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(label = "Namn", value = formName, onValueChange = { formName = it }, required = true)
                    Field(
                        label = "År",
                        value = formYear,
                        onValueChange = { formYear = it.filter { c -> c.isDigit() } },
                        keyboardType = KeyboardType.Number,
                        required = true,
                    )
                    FaltetDatePicker(label = "Sista frost (valfri)", value = formLastFrost, onValueChange = { formLastFrost = it })
                    FaltetDatePicker(label = "Första frost (valfri)", value = formFirstFrost, onValueChange = { formFirstFrost = it })
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val year = formYear.toIntOrNull() ?: return@TextButton
                        val lastFrost = formLastFrost?.toString()
                        val firstFrost = formFirstFrost?.toString()
                        val editing = editingSeason
                        if (editing != null) {
                            viewModel.updateSeason(editing.id, buildMap {
                                put("name", formName)
                                put("year", year)
                                put("lastFrostDate", lastFrost)
                                put("firstFrostDate", firstFrost)
                            })
                        } else {
                            viewModel.createSeason(CreateSeasonRequest(
                                name = formName,
                                year = year,
                                lastFrostDate = lastFrost,
                                firstFrostDate = firstFrost,
                            ))
                        }
                        closeDialog()
                    },
                    enabled = formName.isNotBlank() && formYear.toIntOrNull() != null && !uiState.saving,
                ) { Text(if (editingSeason != null) "Spara" else "Skapa", color = FaltetAccent) }
            },
            dismissButton = {
                Row {
                    val editing = editingSeason
                    if (editing != null) {
                        TextButton(onClick = {
                            viewModel.deleteSeason(editing.id)
                            closeDialog()
                        }) { Text("Ta bort", color = FaltetClay) }
                    }
                    TextButton(onClick = closeDialog) { Text("Avbryt") }
                }
            },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Säsonger",
        fab = { FaltetFab(onClick = openCreate, contentDescription = "Skapa säsong") },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            uiState.seasons.isEmpty() -> FaltetEmptyState(
                headline = "Inga säsonger",
                subtitle = "Skapa din första säsong.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(uiState.seasons, key = { it.id }) { season ->
                    FaltetListRow(
                        title = season.name,
                        meta = buildString {
                            append(season.year.toString())
                            season.lastFrostDate?.let { append(" · Sista frost $it") }
                            season.firstFrostDate?.let { append(" · Första frost $it") }
                        },
                        leading = if (season.isActive) {
                            {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .drawBehind { drawCircle(FaltetAccent) },
                                )
                            }
                        } else null,
                        stat = if (season.isActive) {
                            {
                                Text(
                                    text = "AKTIV",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.2.sp,
                                    color = FaltetAccent,
                                )
                            }
                        } else null,
                        onClick = { openEdit(season) },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun SeasonRowPreview() {
    FaltetListRow(
        title = "Sommar",
        meta = "2026 · Sista frost 2026-05-15",
        leading = {
            Box(
                Modifier
                    .size(10.dp)
                    .drawBehind { drawCircle(FaltetAccent) },
            )
        },
        stat = {
            Text(
                text = "AKTIV",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                color = FaltetAccent,
            )
        },
        onClick = {},
    )
}
