package app.verdant.android.ui.activity

import android.graphics.Bitmap
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.*
import app.verdant.android.data.repository.GardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddSpeciesState(
    val isLoading: Boolean = false,
    val created: Boolean = false,
    val error: String? = null,
    val groups: List<SpeciesGroupResponse> = emptyList(),
    val tags: List<SpeciesTagResponse> = emptyList(),
    val identifying: Boolean = false,
    val suggestions: List<PlantSuggestion> = emptyList(),
)

@HiltViewModel
class AddSpeciesViewModel @Inject constructor(
    private val repo: GardenRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddSpeciesState())
    val uiState = _uiState.asStateFlow()

    init { loadGroupsAndTags() }

    private fun loadGroupsAndTags() {
        viewModelScope.launch {
            try {
                val groups = repo.getSpeciesGroups()
                val tags = repo.getSpeciesTags()
                _uiState.value = _uiState.value.copy(groups = groups, tags = tags)
            } catch (_: Exception) {}
        }
    }

    fun createSpecies(request: CreateSpeciesRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repo.createSpecies(request)
                _uiState.value = _uiState.value.copy(isLoading = false, created = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun createGroup(name: String) {
        viewModelScope.launch {
            try {
                repo.createSpeciesGroup(CreateSpeciesGroupRequest(name))
                loadGroupsAndTags()
            } catch (_: Exception) {}
        }
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            try {
                repo.createSpeciesTag(CreateSpeciesTagRequest(name))
                loadGroupsAndTags()
            } catch (_: Exception) {}
        }
    }

    fun identifyPlant(imageBase64: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(identifying = true, suggestions = emptyList())
            try {
                val suggestions = repo.identifyPlant(IdentifyPlantRequest(imageBase64))
                _uiState.value = _uiState.value.copy(identifying = false, suggestions = suggestions)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(identifying = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpeciesScreen(
    onBack: () -> Unit,
    viewModel: AddSpeciesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var commonName by remember { mutableStateOf("") }
    var scientificName by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var daysToSprout by remember { mutableStateOf("") }
    var daysToHarvest by remember { mutableStateOf("") }
    var germinationTimeDays by remember { mutableStateOf("") }
    var sowingDepthMm by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    var bloomTime by remember { mutableStateOf("") }
    var germinationRate by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var selectedTagIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }

    val growingPositions = listOf("SUNNY", "PARTIALLY_SUNNY", "SHADOWY")
    val growingPositionLabels = listOf("Sunny", "Partial sun", "Shadowy")
    var selectedPosition by remember { mutableStateOf<String?>(null) }

    val soilTypes = listOf("CLAY", "SANDY", "LOAMY", "CHALKY", "PEATY", "SILTY")
    var selectedSoil by remember { mutableStateOf<String?>(null) }

    // Auto-populate from AI suggestions
    LaunchedEffect(uiState.suggestions) {
        if (uiState.suggestions.isNotEmpty() && commonName.isBlank()) {
            val top = uiState.suggestions.first()
            commonName = top.commonName
            scientificName = top.species
        }
    }

    LaunchedEffect(uiState.created) {
        if (uiState.created) onBack()
    }

    if (showNewGroupDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            title = { Text("New Group") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Group name") },
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.createGroup(newName)
                        showNewGroupDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showNewTagDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewTagDialog = false },
            title = { Text("New Tag") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Tag name") },
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.createTag(newName)
                        showNewTagDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewTagDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Species") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo + AI identification
            Text("Photo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            PhotoPicker(
                imageBase64 = imageBase64,
                onImageCaptured = { b64, _ ->
                    imageBase64 = b64
                    viewModel.identifyPlant(b64)
                }
            )

            if (uiState.identifying) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp))
                    Text("Identifying...", fontSize = 14.sp)
                }
            }

            if (uiState.suggestions.isNotEmpty()) {
                Text("AI Suggestions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                uiState.suggestions.forEach { s ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            commonName = s.commonName
                            scientificName = s.species
                        }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${s.commonName} (${s.species})", fontWeight = FontWeight.Medium)
                            Text("${(s.confidence * 100).toInt()}%", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // Names
            OutlinedTextField(
                value = commonName,
                onValueChange = { commonName = it },
                label = { Text("Common Name *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = scientificName,
                onValueChange = { scientificName = it },
                label = { Text("Scientific Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Growth timings
            Text("Growth Information", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = daysToSprout,
                    onValueChange = { daysToSprout = it.filter { c -> c.isDigit() } },
                    label = { Text("Days to sprout") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = daysToHarvest,
                    onValueChange = { daysToHarvest = it.filter { c -> c.isDigit() } },
                    label = { Text("Days to harvest") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = germinationTimeDays,
                    onValueChange = { germinationTimeDays = it.filter { c -> c.isDigit() } },
                    label = { Text("Germ. time (days)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = sowingDepthMm,
                    onValueChange = { sowingDepthMm = it.filter { c -> c.isDigit() } },
                    label = { Text("Sowing depth (mm)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = heightCm,
                    onValueChange = { heightCm = it.filter { c -> c.isDigit() } },
                    label = { Text("Height (cm)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = bloomTime,
                    onValueChange = { bloomTime = it },
                    label = { Text("Bloom time") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = germinationRate,
                onValueChange = { germinationRate = it.filter { c -> c.isDigit() } },
                label = { Text("Germination rate (%)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Growing position
            Text("Growing Position", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                growingPositions.forEachIndexed { i, pos ->
                    FilterChip(
                        selected = selectedPosition == pos,
                        onClick = { selectedPosition = if (selectedPosition == pos) null else pos },
                        label = { Text(growingPositionLabels[i]) }
                    )
                }
            }

            // Soil type
            Text("Soil Type", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                soilTypes.forEach { soil ->
                    FilterChip(
                        selected = selectedSoil == soil,
                        onClick = { selectedSoil = if (selectedSoil == soil) null else soil },
                        label = { Text(soil.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Group picker
            Text("Group", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            ExposedDropdownMenuBox(
                expanded = groupExpanded,
                onExpandedChange = { groupExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.groups.find { it.id == selectedGroupId }?.name ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = groupExpanded,
                    onDismissRequest = { groupExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = { selectedGroupId = null; groupExpanded = false }
                    )
                    uiState.groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = { selectedGroupId = group.id; groupExpanded = false }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("+ Create new group", color = MaterialTheme.colorScheme.primary) },
                        onClick = { showNewGroupDialog = true; groupExpanded = false }
                    )
                }
            }

            // Tag picker
            Text("Tags", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                uiState.tags.forEach { tag ->
                    FilterChip(
                        selected = tag.id in selectedTagIds,
                        onClick = {
                            selectedTagIds = if (tag.id in selectedTagIds)
                                selectedTagIds - tag.id else selectedTagIds + tag.id
                        },
                        label = { Text(tag.name) }
                    )
                }
                SuggestionChip(
                    onClick = { showNewTagDialog = true },
                    label = { Text("+ New tag") }
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.createSpecies(
                        CreateSpeciesRequest(
                            commonName = commonName,
                            scientificName = scientificName.ifBlank { null },
                            imageBase64 = imageBase64,
                            daysToSprout = daysToSprout.toIntOrNull(),
                            daysToHarvest = daysToHarvest.toIntOrNull(),
                            germinationTimeDays = germinationTimeDays.toIntOrNull(),
                            sowingDepthMm = sowingDepthMm.toIntOrNull(),
                            growingPosition = selectedPosition,
                            soil = selectedSoil,
                            heightCm = heightCm.toIntOrNull(),
                            bloomTime = bloomTime.ifBlank { null },
                            germinationRate = germinationRate.toIntOrNull(),
                            groupId = selectedGroupId,
                            tagIds = selectedTagIds.toList(),
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = commonName.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Species")
                }
            }

            uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
            Spacer(Modifier.height(32.dp))
        }
    }
}
