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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.R
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
    val extracting: Boolean = false,
    val suggestions: List<PlantSuggestion> = emptyList(),
    val extractedInfo: ExtractedSpeciesInfo? = null,
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

    fun extractSpeciesInfo(imageBase64: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(extracting = true, extractedInfo = null)
            try {
                val info = repo.extractSpeciesInfo(ExtractSpeciesInfoRequest(imageBase64))
                _uiState.value = _uiState.value.copy(extracting = false, extractedInfo = info)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(extracting = false)
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
    val currentLocale = java.util.Locale.getDefault().language // "sv" or "en"
    var imageFrontBase64 by remember { mutableStateOf<String?>(null) }
    var imageBackBase64 by remember { mutableStateOf<String?>(null) }
    var frontBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var backBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
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
    val growingPositionLabelRes = listOf(R.string.sunny, R.string.partial_sun, R.string.shadowy)
    var selectedPosition by remember { mutableStateOf<String?>(null) }

    val soilTypes = listOf("CLAY", "SANDY", "LOAMY", "CHALKY", "PEATY", "SILTY")
    val soilTypeLabelRes = listOf(R.string.clay, R.string.sandy, R.string.loamy, R.string.chalky, R.string.peaty, R.string.silty)
    var selectedSoil by remember { mutableStateOf<String?>(null) }

    // Auto-populate from AI suggestions (front photo) + crop
    LaunchedEffect(uiState.suggestions) {
        if (uiState.suggestions.isNotEmpty()) {
            val top = uiState.suggestions.first()
            if (commonName.isBlank()) {
                commonName = top.commonName
                scientificName = top.species
            }
            // Crop front photo to seed package bounds
            top.cropBox?.let { box ->
                frontBitmap?.let { bmp ->
                    val cropped = bmp.cropToBox(box)
                    frontBitmap = cropped
                    imageFrontBase64 = cropped.toCompressedBase64()
                }
            }
        }
    }

    // Auto-populate from extracted info (back photo) + crop
    LaunchedEffect(uiState.extractedInfo) {
        val info = uiState.extractedInfo ?: return@LaunchedEffect
        if (commonName.isBlank()) info.commonName?.let { commonName = it }
        if (scientificName.isBlank()) info.scientificName?.let { scientificName = it }
        info.daysToSprout?.let { daysToSprout = it.toString() }
        info.daysToHarvest?.let { daysToHarvest = it.toString() }
        info.germinationTimeDays?.let { germinationTimeDays = it.toString() }
        info.sowingDepthMm?.let { sowingDepthMm = it.toString() }
        info.heightCm?.let { heightCm = it.toString() }
        info.bloomTime?.let { bloomTime = it }
        info.germinationRate?.let { germinationRate = it.toString() }
        info.growingPosition?.let { selectedPosition = it }
        info.soil?.let { selectedSoil = it }
        // Crop back photo to seed package bounds
        info.cropBox?.let { box ->
            backBitmap?.let { bmp ->
                val cropped = bmp.cropToBox(box)
                backBitmap = cropped
                imageBackBase64 = cropped.toCompressedBase64()
            }
        }
    }

    LaunchedEffect(uiState.created) {
        if (uiState.created) onBack()
    }

    if (showNewGroupDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            title = { Text(stringResource(R.string.new_group)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.group_name)) },
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.createGroup(newName)
                        showNewGroupDialog = false
                    }
                }) { Text(stringResource(R.string.create)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showNewTagDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewTagDialog = false },
            title = { Text(stringResource(R.string.new_tag_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.tag_name)) },
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.createTag(newName)
                        showNewTagDialog = false
                    }
                }) { Text(stringResource(R.string.create)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewTagDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_species)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
            // Photos side by side
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.front_photo), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    PhotoPicker(
                        imageBase64 = imageFrontBase64,
                        onImageCaptured = { b64, bmp ->
                            imageFrontBase64 = b64
                            frontBitmap = bmp
                            viewModel.identifyPlant(b64)
                        }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.back_photo), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    PhotoPicker(
                        imageBase64 = imageBackBase64,
                        onImageCaptured = { b64, bmp ->
                            imageBackBase64 = b64
                            backBitmap = bmp
                            viewModel.extractSpeciesInfo(b64)
                        }
                    )
                }
            }

            if (uiState.identifying || uiState.extracting) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp))
                    Text(stringResource(R.string.identifying), fontSize = 14.sp)
                }
            }

            if (uiState.suggestions.isNotEmpty()) {
                Text(stringResource(R.string.ai_suggestions), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                label = { Text(stringResource(R.string.common_name_required)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = scientificName,
                onValueChange = { scientificName = it },
                label = { Text(stringResource(R.string.scientific_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Growth timings
            Text(stringResource(R.string.growth_information), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = daysToSprout,
                    onValueChange = { daysToSprout = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.days_to_sprout)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = daysToHarvest,
                    onValueChange = { daysToHarvest = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.days_to_harvest)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = germinationTimeDays,
                    onValueChange = { germinationTimeDays = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.germination_time_days)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = sowingDepthMm,
                    onValueChange = { sowingDepthMm = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.sowing_depth_mm)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = heightCm,
                    onValueChange = { heightCm = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.height_cm)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = bloomTime,
                    onValueChange = { bloomTime = it },
                    label = { Text(stringResource(R.string.bloom_time)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = germinationRate,
                onValueChange = { germinationRate = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.germination_rate_percent)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Growing position
            Text(stringResource(R.string.growing_position), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                growingPositions.forEachIndexed { i, pos ->
                    FilterChip(
                        selected = selectedPosition == pos,
                        onClick = { selectedPosition = if (selectedPosition == pos) null else pos },
                        label = { Text(stringResource(growingPositionLabelRes[i])) }
                    )
                }
            }

            // Soil type
            Text(stringResource(R.string.soil_type), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                soilTypes.forEachIndexed { i, soil ->
                    FilterChip(
                        selected = selectedSoil == soil,
                        onClick = { selectedSoil = if (selectedSoil == soil) null else soil },
                        label = { Text(stringResource(soilTypeLabelRes[i])) }
                    )
                }
            }

            // Group picker
            Text(stringResource(R.string.group), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            ExposedDropdownMenuBox(
                expanded = groupExpanded,
                onExpandedChange = { groupExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.groups.find { it.id == selectedGroupId }?.name ?: stringResource(R.string.none),
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
                        text = { Text(stringResource(R.string.none)) },
                        onClick = { selectedGroupId = null; groupExpanded = false }
                    )
                    uiState.groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = { selectedGroupId = group.id; groupExpanded = false }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.create_new_group), color = MaterialTheme.colorScheme.primary) },
                        onClick = { showNewGroupDialog = true; groupExpanded = false }
                    )
                }
            }

            // Tag picker
            Text(stringResource(R.string.tags), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                    label = { Text(stringResource(R.string.new_tag)) }
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.createSpecies(
                        CreateSpeciesRequest(
                            commonName = commonName,
                            commonNameSv = if (currentLocale == "sv") commonName else null,
                            scientificName = scientificName.ifBlank { null },
                            imageFrontBase64 = imageFrontBase64,
                            imageBackBase64 = imageBackBase64,
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
                    Text(stringResource(R.string.save_species))
                }
            }

            uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
            Spacer(Modifier.height(32.dp))
        }
    }
}
