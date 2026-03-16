package app.verdant.android.ui.activity

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val existingSpecies: SpeciesResponse? = null,
)

@HiltViewModel
class AddSpeciesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: GardenRepository
) : ViewModel() {
    val speciesId: Long? = savedStateHandle.get<Long>("speciesId")?.takeIf { it > 0 }
    private val _uiState = MutableStateFlow(AddSpeciesState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val groups = repo.getSpeciesGroups()
                val tags = repo.getSpeciesTags()
                val existing = speciesId?.let { repo.getSpecies().find { s -> s.id == it } }
                _uiState.value = _uiState.value.copy(groups = groups, tags = tags, existingSpecies = existing)
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

    fun updateSpecies(request: UpdateSpeciesRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repo.updateSpecies(speciesId!!, request)
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
                loadData()
            } catch (_: Exception) {}
        }
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            try {
                repo.createSpeciesTag(CreateSpeciesTagRequest(name))
                loadData()
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
    val isEdit = viewModel.speciesId != null

    var commonName by remember { mutableStateOf("") }
    var scientificName by remember { mutableStateOf("") }
    val currentLocale = java.util.Locale.getDefault().language // "sv" or "en"
    var imageFrontBase64 by remember { mutableStateOf<String?>(null) }
    var imageBackBase64 by remember { mutableStateOf<String?>(null) }
    var imageFrontUrl by remember { mutableStateOf<String?>(null) }
    var imageBackUrl by remember { mutableStateOf<String?>(null) }
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
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }

    val growingPositions = listOf("SUNNY", "PARTIALLY_SUNNY", "SHADOWY")
    val growingPositionLabelRes = listOf(R.string.sunny, R.string.partial_sun, R.string.shadowy)
    var selectedPositions by remember { mutableStateOf<Set<String>>(emptySet()) }

    val soilTypes = listOf("CLAY", "SANDY", "LOAMY", "CHALKY", "PEATY", "SILTY")
    val soilTypeLabelRes = listOf(R.string.clay, R.string.sandy, R.string.loamy, R.string.chalky, R.string.peaty, R.string.silty)
    var selectedSoils by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Pre-fill from existing species for edit mode
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.existingSpecies) {
        val s = uiState.existingSpecies
        if (s != null && !prefilled) {
            commonName = s.commonName
            scientificName = s.scientificName ?: ""
            imageFrontUrl = s.imageFrontUrl
            imageBackUrl = s.imageBackUrl
            daysToSprout = s.daysToSprout?.toString() ?: ""
            daysToHarvest = s.daysToHarvest?.toString() ?: ""
            germinationTimeDays = s.germinationTimeDays?.toString() ?: ""
            sowingDepthMm = s.sowingDepthMm?.toString() ?: ""
            heightCm = s.heightCm?.toString() ?: ""
            bloomTime = s.bloomTime ?: ""
            germinationRate = s.germinationRate?.toString() ?: ""
            selectedGroupId = s.groupId
            selectedTagIds = s.tags.map { it.id }.toSet()
            selectedPositions = s.growingPositions.toSet()
            selectedSoils = s.soils.toSet()
            prefilled = true
        }
    }

    val hasData = commonName.isNotBlank() || scientificName.isNotBlank() ||
        imageFrontBase64 != null || imageBackBase64 != null || imageFrontUrl != null || imageBackUrl != null ||
        daysToSprout.isNotBlank() || daysToHarvest.isNotBlank() ||
        germinationTimeDays.isNotBlank() || sowingDepthMm.isNotBlank() ||
        heightCm.isNotBlank() || bloomTime.isNotBlank() || germinationRate.isNotBlank()

    // Validation: all fields mandatory except group and tags
    val isCommonNameValid = commonName.isNotBlank()
    val isScientificNameValid = scientificName.isNotBlank()
    val isDaysToSproutValid = daysToSprout.toIntOrNull() != null
    val isDaysToHarvestValid = daysToHarvest.toIntOrNull() != null
    val isGerminationTimeDaysValid = germinationTimeDays.toIntOrNull() != null
    val isSowingDepthMmValid = sowingDepthMm.toIntOrNull() != null
    val isHeightCmValid = heightCm.toIntOrNull() != null
    val isBloomTimeValid = bloomTime.isNotBlank()
    val isGerminationRateValid = germinationRate.toIntOrNull() != null
    val isPositionsValid = selectedPositions.isNotEmpty()
    val isSoilsValid = selectedSoils.isNotEmpty()
    val isFrontPhotoValid = imageFrontBase64 != null || imageFrontUrl != null

    val isFormValid = isCommonNameValid && isScientificNameValid &&
        isDaysToSproutValid && isDaysToHarvestValid && isGerminationTimeDaysValid &&
        isSowingDepthMmValid && isHeightCmValid && isBloomTimeValid &&
        isGerminationRateValid && isPositionsValid && isSoilsValid && isFrontPhotoValid

    // In edit mode, check whether anything actually changed from the original
    val existing = uiState.existingSpecies
    val hasChanges = existing == null || // create mode = always "changed"
        commonName != existing.commonName ||
        (scientificName) != (existing.scientificName ?: "") ||
        imageFrontBase64 != null ||
        imageBackBase64 != null ||
        daysToSprout != (existing.daysToSprout?.toString() ?: "") ||
        daysToHarvest != (existing.daysToHarvest?.toString() ?: "") ||
        germinationTimeDays != (existing.germinationTimeDays?.toString() ?: "") ||
        sowingDepthMm != (existing.sowingDepthMm?.toString() ?: "") ||
        heightCm != (existing.heightCm?.toString() ?: "") ||
        bloomTime != (existing.bloomTime ?: "") ||
        germinationRate != (existing.germinationRate?.toString() ?: "") ||
        selectedPositions != existing.growingPositions.toSet() ||
        selectedSoils != existing.soils.toSet() ||
        selectedGroupId != existing.groupId ||
        selectedTagIds != existing.tags.map { it.id }.toSet()

    fun tryBack() {
        if (hasData) showDiscardDialog = true else onBack()
    }

    androidx.activity.compose.BackHandler(enabled = true) { tryBack() }

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
        info.growingPositions?.let { selectedPositions = it.toSet() }
        info.soils?.let { selectedSoils = it.toSet() }
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

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_changes)) },
            text = { Text(stringResource(R.string.discard_changes_confirm)) },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onBack() }) {
                    Text(stringResource(R.string.discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
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
                title = { Text(stringResource(if (isEdit) R.string.edit_species else R.string.add_species)) },
                navigationIcon = {
                    IconButton(onClick = { tryBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photos side by side
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.front_photo), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        PhotoPicker(
                            imageUrl = imageFrontUrl,
                            maxImageHeight = 180,
                            onImageCaptured = { b64, bmp ->
                                imageFrontBase64 = b64
                                imageFrontUrl = null
                                frontBitmap = bmp
                                viewModel.identifyPlant(b64)
                            }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.back_photo), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        PhotoPicker(
                            imageUrl = imageBackUrl,
                            maxImageHeight = 180,
                            onImageCaptured = { b64, bmp ->
                                imageBackBase64 = b64
                                imageBackUrl = null
                                backBitmap = bmp
                                viewModel.extractSpeciesInfo(b64)
                            }
                        )
                    }
                }
            }

            if (uiState.identifying || uiState.extracting) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(Modifier.size(16.dp))
                        Text(stringResource(R.string.identifying), fontSize = 14.sp)
                    }
                }
            }

            if (uiState.suggestions.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.ai_suggestions), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                uiState.suggestions.forEach { s ->
                    item {
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
            }

            // Names
            item {
                OutlinedTextField(
                    value = commonName,
                    onValueChange = { commonName = it },
                    label = { Text(stringResource(R.string.common_name_required)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = showValidationErrors && !isCommonNameValid
                )
            }
            item {
                OutlinedTextField(
                    value = scientificName,
                    onValueChange = { scientificName = it },
                    label = { Text(stringResource(R.string.scientific_name) + " *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = showValidationErrors && !isScientificNameValid
                )
            }

            // Growth timings
            item { Text(stringResource(R.string.growth_information), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = daysToSprout,
                        onValueChange = { daysToSprout = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.days_to_sprout) + " *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = showValidationErrors && !isDaysToSproutValid
                    )
                    OutlinedTextField(
                        value = daysToHarvest,
                        onValueChange = { daysToHarvest = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.days_to_harvest) + " *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = showValidationErrors && !isDaysToHarvestValid
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = germinationTimeDays,
                        onValueChange = { germinationTimeDays = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.germination_time_days) + " *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = showValidationErrors && !isGerminationTimeDaysValid
                    )
                    OutlinedTextField(
                        value = sowingDepthMm,
                        onValueChange = { sowingDepthMm = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.sowing_depth_mm) + " *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = showValidationErrors && !isSowingDepthMmValid
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = heightCm,
                        onValueChange = { heightCm = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.height_cm) + " *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = showValidationErrors && !isHeightCmValid
                    )
                    OutlinedTextField(
                        value = bloomTime,
                        onValueChange = { bloomTime = it },
                        label = { Text(stringResource(R.string.bloom_time) + " *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = showValidationErrors && !isBloomTimeValid
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = germinationRate,
                    onValueChange = { germinationRate = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.germination_rate_percent) + " *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = showValidationErrors && !isGerminationRateValid
                )
            }

            // Growing position
            item {
                Text(
                    stringResource(R.string.growing_position) + " *",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (showValidationErrors && !isPositionsValid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    growingPositions.forEachIndexed { i, pos ->
                        FilterChip(
                            selected = pos in selectedPositions,
                            onClick = { selectedPositions = if (pos in selectedPositions) selectedPositions - pos else selectedPositions + pos },
                            label = { Text(stringResource(growingPositionLabelRes[i])) }
                        )
                    }
                }
            }

            // Soil type
            item {
                Text(
                    stringResource(R.string.soil_type) + " *",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (showValidationErrors && !isSoilsValid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    soilTypes.forEachIndexed { i, soil ->
                        FilterChip(
                            selected = soil in selectedSoils,
                            onClick = { selectedSoils = if (soil in selectedSoils) selectedSoils - soil else selectedSoils + soil },
                            label = { Text(stringResource(soilTypeLabelRes[i])) }
                        )
                    }
                }
            }

            // Group picker
            item { Text(stringResource(R.string.group), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            item {
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
            }

            // Tag picker
            item { Text(stringResource(R.string.tags), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            item {
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
            }

            // Providers (read-only, shown in edit mode)
            if (isEdit && uiState.existingSpecies != null) {
                val providers = uiState.existingSpecies!!.providers
                item {
                    Text(
                        stringResource(R.string.providers),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                if (providers.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.no_providers),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    providers.forEach { provider ->
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        provider.providerName,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        provider.providerIdentifier,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    // Front/back images
                                    if (provider.imageFrontUrl != null || provider.imageBackUrl != null) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            provider.imageFrontUrl?.let { url ->
                                                coil.compose.AsyncImage(
                                                    model = url,
                                                    contentDescription = stringResource(R.string.front_photo),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(max = 120.dp),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                                )
                                            }
                                            provider.imageBackUrl?.let { url ->
                                                coil.compose.AsyncImage(
                                                    model = url,
                                                    contentDescription = stringResource(R.string.back_photo),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(max = 120.dp),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                                )
                                            }
                                        }
                                    }
                                    // Product URL
                                    provider.productUrl?.let { url ->
                                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            stringResource(R.string.product_page),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable { uriHandler.openUri(url) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                Button(
                    onClick = {
                        if (isEdit && !hasChanges) { onBack(); return@Button }
                        showValidationErrors = true
                        if (!isFormValid) return@Button
                        if (isEdit) {
                            viewModel.updateSpecies(
                                UpdateSpeciesRequest(
                                    commonName = commonName,
                                    commonNameSv = if (currentLocale == "sv") commonName else null,
                                    scientificName = scientificName,
                                    imageFrontBase64 = imageFrontBase64,
                                    imageBackBase64 = imageBackBase64,
                                    daysToSprout = daysToSprout.toIntOrNull(),
                                    daysToHarvest = daysToHarvest.toIntOrNull(),
                                    germinationTimeDays = germinationTimeDays.toIntOrNull(),
                                    sowingDepthMm = sowingDepthMm.toIntOrNull(),
                                    growingPositions = selectedPositions.toList(),
                                    soils = selectedSoils.toList(),
                                    heightCm = heightCm.toIntOrNull(),
                                    bloomTime = bloomTime.ifBlank { null },
                                    germinationRate = germinationRate.toIntOrNull(),
                                    groupId = selectedGroupId,
                                    tagIds = selectedTagIds.toList(),
                                )
                            )
                        } else {
                            viewModel.createSpecies(
                                CreateSpeciesRequest(
                                    commonName = commonName,
                                    commonNameSv = if (currentLocale == "sv") commonName else null,
                                    scientificName = scientificName,
                                    imageFrontBase64 = imageFrontBase64,
                                    imageBackBase64 = imageBackBase64,
                                    daysToSprout = daysToSprout.toIntOrNull(),
                                    daysToHarvest = daysToHarvest.toIntOrNull(),
                                    germinationTimeDays = germinationTimeDays.toIntOrNull(),
                                    sowingDepthMm = sowingDepthMm.toIntOrNull(),
                                    growingPositions = selectedPositions.toList(),
                                    soils = selectedSoils.toList(),
                                    heightCm = heightCm.toIntOrNull(),
                                    bloomTime = bloomTime.ifBlank { null },
                                    germinationRate = germinationRate.toIntOrNull(),
                                    groupId = selectedGroupId,
                                    tagIds = selectedTagIds.toList(),
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(R.string.save_species))
                    }
                }
            }

            item {
                uiState.error?.let { app.verdant.android.ui.common.InlineErrorBanner(it) }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
