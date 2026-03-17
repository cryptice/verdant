package app.verdant.android.ui.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.*
import app.verdant.android.ui.theme.verdantTopAppBarColors
import app.verdant.android.data.repository.GardenRepository
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
)

@HiltViewModel
class BatchPlantOutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: GardenRepository
) : ViewModel() {
    val targetBedId: Long = savedStateHandle.get<Long>("bedId") ?: 0L
    private val _uiState = MutableStateFlow(BatchPlantOutState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // Load tray plants with SEEDED and POTTED_UP status
                val seeded = repo.getPlantGroups("SEEDED", trayOnly = true)
                val pottedUp = repo.getPlantGroups("POTTED_UP", trayOnly = true)
                _uiState.value = BatchPlantOutState(isLoading = false, groups = seeded + pottedUp)
            } catch (e: Exception) {
                _uiState.value = BatchPlantOutState(isLoading = false, error = e.message)
            }
        }
    }

    fun submit(group: PlantGroupResponse, count: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(submitting = true, error = null)
            try {
                repo.batchEvent(
                    BatchEventRequest(
                        speciesId = group.speciesId,
                        bedId = group.bedId,
                        plantedDate = group.plantedDate,
                        status = group.status,
                        eventType = "PLANTED_OUT",
                        count = count,
                        targetBedId = targetBedId,
                    )
                )
                _uiState.value = _uiState.value.copy(submitting = false, created = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(submitting = false, error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchPlantOutScreen(
    onBack: () -> Unit,
    viewModel: BatchPlantOutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedGroup by remember { mutableStateOf<PlantGroupResponse?>(null) }
    var count by remember { mutableStateOf("") }

    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    LaunchedEffect(uiState.groups) {
        if (uiState.groups.size == 1 && selectedGroup == null) {
            selectedGroup = uiState.groups.first()
            count = uiState.groups.first().count.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plant_from_tray)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = verdantTopAppBarColors()
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.error != null && uiState.groups.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { app.verdant.android.ui.common.ConnectionErrorState(onRetry = onBack) }

            selectedGroup == null -> {
                if (uiState.groups.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_plants_in_trays), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                        items(uiState.groups) { group ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedGroup = group
                                        count = group.count.toString()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        group.speciesName ?: stringResource(R.string.unknown_species),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        buildString {
                                            append(stringResource(R.string.tray))
                                            append(" · ${group.status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}")
                                            group.plantedDate?.let { append(" · $it") }
                                        },
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                Text(
                                    "${group.count}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        selectedGroup!!.speciesName ?: stringResource(R.string.unknown_species),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        buildString {
                            append(stringResource(R.string.tray))
                            append(" · ${stringResource(R.string.n_available, selectedGroup!!.count)}")
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    CountField(
                        value = count,
                        onValueChange = { count = it },
                        label = stringResource(R.string.plant_count)
                    )

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val c = count.toIntOrNull() ?: 0
                            if (c > 0) viewModel.submit(selectedGroup!!, c.coerceAtMost(selectedGroup!!.count))
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = (count.toIntOrNull() ?: 0) in 1..selectedGroup!!.count && !uiState.submitting
                    ) {
                        if (uiState.submitting) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(stringResource(R.string.plant_out))
                        }
                    }

                    uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
