package app.verdant.android.ui.bed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
import app.verdant.android.data.model.BedResponse
import app.verdant.android.data.model.UpdateBedRequest
import app.verdant.android.ui.theme.verdantTopAppBarColors
import app.verdant.android.data.model.PlantResponse
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BedDetailState(
    val isLoading: Boolean = true,
    val bed: BedResponse? = null,
    val plants: List<PlantResponse> = emptyList(),
    val error: String? = null,
    val deleted: Boolean = false
)

@HiltViewModel
class BedDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gardenRepository: GardenRepository
) : ViewModel() {
    private val bedId: Long = savedStateHandle.get<Long>("bedId")!!
    private val _uiState = MutableStateFlow(BedDetailState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = BedDetailState(isLoading = true)
            try {
                val bed = gardenRepository.getBed(bedId)
                val plants = gardenRepository.getPlants(bedId)
                _uiState.value = BedDetailState(isLoading = false, bed = bed, plants = plants)
            } catch (e: Exception) {
                _uiState.value = BedDetailState(isLoading = false, error = e.message)
            }
        }
    }

    fun update(name: String, description: String?) {
        viewModelScope.launch {
            try {
                gardenRepository.updateBed(bedId, UpdateBedRequest(name = name, description = description))
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                gardenRepository.deleteBed(bedId)
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedDetailScreen(
    onBack: () -> Unit,
    onPlantClick: (Long) -> Unit,
    onSowInBed: (Long) -> Unit = {},
    onPlantFromTray: (Long) -> Unit = {},
    viewModel: BedDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(R.string.bed_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text(stringResource(R.string.description_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.update(editName, editDescription.ifBlank { null })
                        editing = false
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showAddDialog) {
        val bedId = uiState.bed?.id ?: 0L
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_plant)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showAddDialog = false; onSowInBed(bedId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(stringResource(R.string.sow_seeds_in_bed)) }
                    Button(
                        onClick = { showAddDialog = false; onPlantFromTray(bedId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(stringResource(R.string.plant_from_tray)) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_bed)) },
            text = { Text(stringResource(R.string.delete_bed_confirm)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.bed?.name ?: stringResource(R.string.bed)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        uiState.bed?.let { bed ->
                            editName = bed.name
                            editDescription = bed.description ?: ""
                            editing = true
                        }
                    }) {
                        Icon(Icons.Default.Edit, stringResource(R.string.edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = verdantTopAppBarColors()
            )
        },
        floatingActionButton = {
            if (uiState.bed != null) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_plant))
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> {
                app.verdant.android.ui.common.ConnectionErrorState(
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                val groupedPlants = uiState.plants.groupBy { it.speciesName ?: it.name }
                var expandedGroups by remember { mutableStateOf(setOf<String>()) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.bed?.let { bed ->
                        item {
                            Spacer(Modifier.height(4.dp))
                            bed.description?.let {
                                Text(it, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                                Spacer(Modifier.height(16.dp))
                            }
                            Text(stringResource(R.string.plants), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }

                    if (uiState.plants.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_plants_yet),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }


                    items(groupedPlants.entries.toList(), key = { it.key }) { (species, plantsInGroup) ->
                        val isExpanded = species in expandedGroups
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedGroups = if (isExpanded) expandedGroups - species else expandedGroups + species
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(species, fontWeight = FontWeight.SemiBold)
                                        val count = plantsInGroup.size
                                        val label = if (count == 1) stringResource(R.string.plant_singular) else stringResource(R.string.plant_plural)
                                        Text("$count $label", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Icon(
                                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                AnimatedVisibility(visible = isExpanded) {
                                    Column {
                                        HorizontalDivider()
                                        plantsInGroup.forEachIndexed { index, plant ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onPlantClick(plant.id) }
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(plant.name, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                                AssistChip(
                                                    onClick = {},
                                                    label = { Text(plant.status, fontSize = 12.sp) }
                                                )
                                            }
                                            if (index < plantsInGroup.lastIndex) {
                                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
