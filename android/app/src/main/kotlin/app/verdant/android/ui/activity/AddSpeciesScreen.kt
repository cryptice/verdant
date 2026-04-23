package app.verdant.android.ui.activity

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateSpeciesGroupRequest
import app.verdant.android.data.model.CreateSpeciesRequest
import app.verdant.android.data.model.CreateSpeciesTagRequest
import app.verdant.android.data.model.ExtractSpeciesInfoRequest
import app.verdant.android.data.model.ExtractedSpeciesInfo
import app.verdant.android.data.model.IdentifyPlantRequest
import app.verdant.android.data.model.PlantSuggestion
import app.verdant.android.data.model.SpeciesGroupResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.SpeciesTagResponse
import app.verdant.android.data.model.UpdateSpeciesRequest
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.faltet.FaltetChipMultiSelector
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetImagePicker
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AddSpeciesScreen"

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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load species data", e)
            }
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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create species group", e)
            }
        }
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            try {
                repo.createSpeciesTag(CreateSpeciesTagRequest(name))
                loadData()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create species tag", e)
            }
        }
    }

    fun identifyPlant(imageBase64: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(identifying = true, suggestions = emptyList(), error = null)
            try {
                val suggestions = repo.identifyPlant(IdentifyPlantRequest(imageBase64))
                _uiState.value = _uiState.value.copy(identifying = false, suggestions = suggestions)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(identifying = false, error = "Kunde inte identifiera bilden")
            }
        }
    }

    fun extractSpeciesInfo(imageBase64: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(extracting = true, extractedInfo = null, error = null)
            try {
                val info = repo.extractSpeciesInfo(ExtractSpeciesInfoRequest(imageBase64))
                _uiState.value = _uiState.value.copy(extracting = false, extractedInfo = info)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(extracting = false, error = "Kunde inte extrahera information")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpeciesScreen(
    onBack: () -> Unit,
    viewModel: AddSpeciesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEdit = viewModel.speciesId != null

    var commonName by remember { mutableStateOf("") }
    var variantName by remember { mutableStateOf("") }
    var variantNameSv by remember { mutableStateOf("") }
    var scientificName by remember { mutableStateOf("") }
    var imageFrontBase64 by remember { mutableStateOf<String?>(null) }
    var imageBackBase64 by remember { mutableStateOf<String?>(null) }
    var imageFrontUrl by remember { mutableStateOf<String?>(null) }
    var imageBackUrl by remember { mutableStateOf<String?>(null) }
    var frontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var backBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var germinationTimeDaysMin by remember { mutableStateOf("") }
    var germinationTimeDaysMax by remember { mutableStateOf("") }
    var daysToHarvestMin by remember { mutableStateOf("") }
    var daysToHarvestMax by remember { mutableStateOf("") }
    var sowingDepthMm by remember { mutableStateOf("") }
    var heightCmMin by remember { mutableStateOf("") }
    var heightCmMax by remember { mutableStateOf("") }
    var selectedBloomMonths by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedSowingMonths by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var germinationRate by remember { mutableStateOf("") }
    var selectedTagIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedPositions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSoils by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }
    var prefilled by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.existingSpecies) {
        val s = uiState.existingSpecies
        if (s != null && !prefilled) {
            commonName = s.commonName
            variantName = s.variantName ?: ""
            variantNameSv = s.variantNameSv ?: ""
            scientificName = s.scientificName ?: ""
            imageFrontUrl = s.imageFrontUrl
            imageBackUrl = s.imageBackUrl
            germinationTimeDaysMin = s.germinationTimeDaysMin?.toString() ?: ""
            germinationTimeDaysMax = s.germinationTimeDaysMax?.toString() ?: ""
            daysToHarvestMin = s.daysToHarvestMin?.toString() ?: ""
            daysToHarvestMax = s.daysToHarvestMax?.toString() ?: ""
            sowingDepthMm = s.sowingDepthMm?.toString() ?: ""
            heightCmMin = s.heightCmMin?.toString() ?: ""
            heightCmMax = s.heightCmMax?.toString() ?: ""
            selectedBloomMonths = s.bloomMonths.toSet()
            selectedSowingMonths = s.sowingMonths.toSet()
            germinationRate = s.germinationRate?.toString() ?: ""
            selectedTagIds = s.tags.map { it.id }.toSet()
            selectedPositions = s.growingPositions.toSet()
            selectedSoils = s.soils.toSet()
            selectedGroupId = s.groups.firstOrNull()?.id
            prefilled = true
        }
    }

    // AI auto-populate from suggestions (front photo identification)
    LaunchedEffect(uiState.suggestions) {
        val top = uiState.suggestions.firstOrNull() ?: return@LaunchedEffect
        if (commonName.isBlank()) commonName = top.commonName
        if (scientificName.isBlank()) scientificName = top.species
        val box = top.cropBox
        val src = frontBitmap
        if (box != null && src != null) {
            val cropped = src.cropToBox(box)
            frontBitmap = cropped
            imageFrontBase64 = cropped.toCompressedBase64()
        }
    }

    // AI auto-populate from extraction (back photo info extraction)
    LaunchedEffect(uiState.extractedInfo) {
        val info = uiState.extractedInfo ?: return@LaunchedEffect
        if (commonName.isBlank()) info.commonName?.let { commonName = it }
        if (variantName.isBlank()) info.variantName?.let { variantName = it }
        if (variantNameSv.isBlank()) info.variantNameSv?.let { variantNameSv = it }
        if (scientificName.isBlank()) info.scientificName?.let { scientificName = it }
        if (germinationTimeDaysMin.isBlank()) info.germinationTimeDaysMin?.let { germinationTimeDaysMin = it.toString() }
        if (germinationTimeDaysMax.isBlank()) info.germinationTimeDaysMax?.let { germinationTimeDaysMax = it.toString() }
        if (daysToHarvestMin.isBlank()) info.daysToHarvestMin?.let { daysToHarvestMin = it.toString() }
        if (daysToHarvestMax.isBlank()) info.daysToHarvestMax?.let { daysToHarvestMax = it.toString() }
        if (sowingDepthMm.isBlank()) info.sowingDepthMm?.let { sowingDepthMm = it.toString() }
        if (heightCmMin.isBlank()) info.heightCmMin?.let { heightCmMin = it.toString() }
        if (heightCmMax.isBlank()) info.heightCmMax?.let { heightCmMax = it.toString() }
        if (germinationRate.isBlank()) info.germinationRate?.let { germinationRate = it.toString() }
        if (selectedBloomMonths.isEmpty()) info.bloomMonths?.let { selectedBloomMonths = it.toSet() }
        if (selectedSowingMonths.isEmpty()) info.sowingMonths?.let { selectedSowingMonths = it.toSet() }
        if (selectedPositions.isEmpty()) info.growingPositions?.let { selectedPositions = it.toSet() }
        if (selectedSoils.isEmpty()) info.soils?.let { selectedSoils = it.toSet() }
        val box = info.cropBox
        val src = backBitmap
        if (box != null && src != null) {
            val cropped = src.cropToBox(box)
            backBitmap = cropped
            imageBackBase64 = cropped.toCompressedBase64()
        }
    }

    val hasData = commonName.isNotBlank() || variantName.isNotBlank() || variantNameSv.isNotBlank() ||
        scientificName.isNotBlank() || imageFrontBase64 != null || imageBackBase64 != null ||
        germinationTimeDaysMin.isNotBlank() || germinationTimeDaysMax.isNotBlank() ||
        daysToHarvestMin.isNotBlank() || daysToHarvestMax.isNotBlank() ||
        sowingDepthMm.isNotBlank() || heightCmMin.isNotBlank() || heightCmMax.isNotBlank() ||
        selectedBloomMonths.isNotEmpty() || selectedSowingMonths.isNotEmpty() ||
        germinationRate.isNotBlank() || selectedTagIds.isNotEmpty() ||
        selectedPositions.isNotEmpty() || selectedSoils.isNotEmpty() || selectedGroupId != null

    val hasChanges = if (!isEdit) hasData else {
        val s = uiState.existingSpecies
        s == null || commonName != s.commonName ||
            variantName != (s.variantName ?: "") ||
            variantNameSv != (s.variantNameSv ?: "") ||
            scientificName != (s.scientificName ?: "") ||
            imageFrontBase64 != null || imageBackBase64 != null ||
            germinationTimeDaysMin != (s.germinationTimeDaysMin?.toString() ?: "") ||
            germinationTimeDaysMax != (s.germinationTimeDaysMax?.toString() ?: "") ||
            daysToHarvestMin != (s.daysToHarvestMin?.toString() ?: "") ||
            daysToHarvestMax != (s.daysToHarvestMax?.toString() ?: "") ||
            sowingDepthMm != (s.sowingDepthMm?.toString() ?: "") ||
            heightCmMin != (s.heightCmMin?.toString() ?: "") ||
            heightCmMax != (s.heightCmMax?.toString() ?: "") ||
            selectedBloomMonths != s.bloomMonths.toSet() ||
            selectedSowingMonths != s.sowingMonths.toSet() ||
            germinationRate != (s.germinationRate?.toString() ?: "") ||
            selectedTagIds != s.tags.map { it.id }.toSet() ||
            selectedPositions != s.growingPositions.toSet() ||
            selectedSoils != s.soils.toSet() ||
            selectedGroupId != s.groups.firstOrNull()?.id
    }

    val tryBack: () -> Unit = {
        if (hasData && (!isEdit || hasChanges)) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = hasData) { tryBack() }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(uiState.created) {
        if (uiState.created) onBack()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Avbryt ändringar?") },
            text = { Text("Dina ändringar kommer att gå förlorade.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) { Text("Avbryt ändringar", color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Fortsätt redigera") }
            },
        )
    }

    if (showNewTagDialog) {
        AlertDialog(
            onDismissRequest = { showNewTagDialog = false; newTagName = "" },
            title = { Text("Ny tagg") },
            text = {
                Field(
                    label = "Namn",
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    required = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            viewModel.createTag(newTagName.trim())
                            showNewTagDialog = false
                            newTagName = ""
                        }
                    },
                    enabled = newTagName.isNotBlank(),
                ) { Text("Skapa", color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { showNewTagDialog = false; newTagName = "" }) { Text("Avbryt") }
            },
        )
    }

    if (showNewGroupDialog) {
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false; newGroupName = "" },
            title = { Text("Ny grupp") },
            text = {
                Field(
                    label = "Namn",
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    required = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            viewModel.createGroup(newGroupName.trim())
                            showNewGroupDialog = false
                            newGroupName = ""
                        }
                    },
                    enabled = newGroupName.isNotBlank(),
                ) { Text("Skapa", color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false; newGroupName = "" }) { Text("Avbryt") }
            },
        )
    }

    val submitAction: () -> Unit = {
        showValidationErrors = true
        val germMin = germinationTimeDaysMin.toIntOrNull()
        val harvestMin = daysToHarvestMin.toIntOrNull()
        val depth = sowingDepthMm.toIntOrNull()
        val heightMin = heightCmMin.toIntOrNull()
        val germRate = germinationRate.toIntOrNull()
        val valid = commonName.isNotBlank() &&
            scientificName.isNotBlank() &&
            germMin != null && harvestMin != null &&
            depth != null && heightMin != null && germRate != null &&
            selectedPositions.isNotEmpty() &&
            selectedSoils.isNotEmpty() &&
            (imageFrontBase64 != null || imageFrontUrl != null)

        if (valid) {
            if (isEdit) {
                viewModel.updateSpecies(
                    UpdateSpeciesRequest(
                        commonName = commonName,
                        variantName = variantName.ifBlank { null },
                        variantNameSv = variantNameSv.ifBlank { null },
                        scientificName = scientificName.ifBlank { null },
                        imageFrontBase64 = imageFrontBase64,
                        imageBackBase64 = imageBackBase64,
                        germinationTimeDaysMin = germMin,
                        germinationTimeDaysMax = germinationTimeDaysMax.toIntOrNull(),
                        daysToHarvestMin = harvestMin,
                        daysToHarvestMax = daysToHarvestMax.toIntOrNull(),
                        sowingDepthMm = depth,
                        heightCmMin = heightMin,
                        heightCmMax = heightCmMax.toIntOrNull(),
                        bloomMonths = selectedBloomMonths.toList().sorted(),
                        sowingMonths = selectedSowingMonths.toList().sorted(),
                        germinationRate = germRate,
                        tagIds = selectedTagIds.toList(),
                        growingPositions = selectedPositions.toList(),
                        soils = selectedSoils.toList(),
                        groupId = selectedGroupId,
                    )
                )
            } else {
                viewModel.createSpecies(
                    CreateSpeciesRequest(
                        commonName = commonName,
                        variantName = variantName.ifBlank { null },
                        variantNameSv = variantNameSv.ifBlank { null },
                        scientificName = scientificName.ifBlank { null },
                        imageFrontBase64 = imageFrontBase64,
                        imageBackBase64 = imageBackBase64,
                        germinationTimeDaysMin = germMin,
                        germinationTimeDaysMax = germinationTimeDaysMax.toIntOrNull(),
                        daysToHarvestMin = harvestMin,
                        daysToHarvestMax = daysToHarvestMax.toIntOrNull(),
                        sowingDepthMm = depth,
                        heightCmMin = heightMin,
                        heightCmMax = heightCmMax.toIntOrNull(),
                        bloomMonths = selectedBloomMonths.toList().sorted(),
                        sowingMonths = selectedSowingMonths.toList().sorted(),
                        germinationRate = germRate,
                        tagIds = selectedTagIds.toList(),
                        growingPositions = selectedPositions.toList(),
                        soils = selectedSoils.toList(),
                        groupId = selectedGroupId,
                    )
                )
            }
        }
    }

    val fillFromSuggestion: (PlantSuggestion) -> Unit = { suggestion ->
        commonName = suggestion.commonName
        scientificName = suggestion.species
        val box = suggestion.cropBox
        val src = frontBitmap
        if (box != null && src != null) {
            val cropped = src.cropToBox(box)
            frontBitmap = cropped
            imageFrontBase64 = cropped.toCompressedBase64()
        }
    }

    FaltetScreenScaffold(
        mastheadLeft = "§ Art",
        mastheadCenter = if (isEdit) uiState.existingSpecies?.commonName ?: "Redigera art" else "Ny art",
        mastheadRight = {
            IconButton(onClick = { tryBack() }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Tillbaka",
                    tint = FaltetClay,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        bottomBar = {
            FaltetFormSubmitBar(
                label = if (isEdit) "Spara" else "Skapa",
                onClick = submitAction,
                enabled = !uiState.isLoading,
                submitting = uiState.isLoading,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Photo row: front + back side-by-side
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FaltetImagePicker(
                            label = "Framsida *",
                            value = frontBitmap,
                            onValueChange = { bitmap ->
                                frontBitmap = bitmap
                                if (bitmap != null) {
                                    val b64 = bitmap.toCompressedBase64()
                                    imageFrontBase64 = b64
                                    viewModel.identifyPlant(b64)
                                } else {
                                    imageFrontBase64 = null
                                }
                            },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        FaltetImagePicker(
                            label = "Baksida (valfri)",
                            value = backBitmap,
                            onValueChange = { bitmap ->
                                backBitmap = bitmap
                                if (bitmap != null) {
                                    val b64 = bitmap.toCompressedBase64()
                                    imageBackBase64 = b64
                                    viewModel.extractSpeciesInfo(b64)
                                } else {
                                    imageBackBase64 = null
                                }
                            },
                        )
                    }
                }
            }

            // AI spinner row
            if (uiState.identifying || uiState.extracting) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        CircularProgressIndicator(
                            color = FaltetClay,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (uiState.identifying) "Identifierar…" else "Extraherar information…",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            color = FaltetForest,
                        )
                    }
                }
            }

            // AI suggestion cards
            if (uiState.suggestions.isNotEmpty()) {
                item { FaltetSectionHeader(label = "Förslag") }
                items(uiState.suggestions, key = { it.species }) { suggestion ->
                    SuggestionRow(suggestion = suggestion, onTap = { fillFromSuggestion(suggestion) })
                }
            }

            // Common name
            item {
                Field(
                    label = "Artnamn",
                    value = commonName,
                    onValueChange = { commonName = it },
                    required = true,
                    error = if (showValidationErrors && commonName.isBlank()) "Artnamn krävs" else null,
                )
            }

            // Scientific name
            item {
                Field(
                    label = "Vetenskapligt namn",
                    value = scientificName,
                    onValueChange = { scientificName = it },
                    required = true,
                    error = if (showValidationErrors && scientificName.isBlank()) "Vetenskapligt namn krävs" else null,
                )
            }

            // Variant EN
            item {
                Field(
                    label = "Variant (engelska, valfri)",
                    value = variantName,
                    onValueChange = { variantName = it },
                )
            }

            // Variant SV
            item {
                Field(
                    label = "Variant (svenska, valfri)",
                    value = variantNameSv,
                    onValueChange = { variantNameSv = it },
                )
            }

            // Germination time min
            item {
                Field(
                    label = "Grobarhet dagar min",
                    value = germinationTimeDaysMin,
                    onValueChange = { germinationTimeDaysMin = it.filter { c -> c.isDigit() } },
                    keyboardType = KeyboardType.Number,
                    required = true,
                    error = if (showValidationErrors && germinationTimeDaysMin.toIntOrNull() == null) "Heltal krävs" else null,
                )
            }

            // Germination time max
            item {
                Field(
                    label = "Grobarhet dagar max (valfri)",
                    value = germinationTimeDaysMax,
                    onValueChange = { germinationTimeDaysMax = it.filter { c -> c.isDigit() } },
                    keyboardType = KeyboardType.Number,
                )
            }

            // Days to harvest min
            item {
                Field(
                    label = "Dagar till skörd min",
                    value = daysToHarvestMin,
                    onValueChange = { daysToHarvestMin = it.filter { c -> c.isDigit() } },
                    keyboardType = KeyboardType.Number,
                    required = true,
                    error = if (showValidationErrors && daysToHarvestMin.toIntOrNull() == null) "Heltal krävs" else null,
                )
            }

            // Days to harvest max
            item {
                Field(
                    label = "Dagar till skörd max (valfri)",
                    value = daysToHarvestMax,
                    onValueChange = { daysToHarvestMax = it.filter { c -> c.isDigit() } },
                    keyboardType = KeyboardType.Number,
                )
            }

            // Sowing depth
            item {
                Field(
                    label = "Sådjup mm",
                    value = sowingDepthMm,
                    onValueChange = { sowingDepthMm = it.filter { c -> c.isDigit() } },
                    keyboardType = KeyboardType.Number,
                    required = true,
                    error = if (showValidationErrors && sowingDepthMm.toIntOrNull() == null) "Heltal krävs" else null,
                )
            }

            // Height min
            item {
                Field(
                    label = "Höjd cm min",
                    value = heightCmMin,
                    onValueChange = { heightCmMin = it.filter { c -> c.isDigit() } },
                    keyboardType = KeyboardType.Number,
                    required = true,
                    error = if (showValidationErrors && heightCmMin.toIntOrNull() == null) "Heltal krävs" else null,
                )
            }

            // Height max
            item {
                Field(
                    label = "Höjd cm max (valfri)",
                    value = heightCmMax,
                    onValueChange = { heightCmMax = it.filter { c -> c.isDigit() } },
                    keyboardType = KeyboardType.Number,
                )
            }

            // Sowing months
            item {
                FaltetChipMultiSelector(
                    label = "Såmånader (valfri)",
                    options = (1..12).toList(),
                    selected = selectedSowingMonths,
                    onSelectedChange = { selectedSowingMonths = it },
                    labelFor = { monthShortSv(it) },
                )
            }

            // Bloom months
            item {
                FaltetChipMultiSelector(
                    label = "Blomningsmånader (valfri)",
                    options = (1..12).toList(),
                    selected = selectedBloomMonths,
                    onSelectedChange = { selectedBloomMonths = it },
                    labelFor = { monthShortSv(it) },
                )
            }

            // Germination rate
            item {
                Field(
                    label = "Grobarhetsprocent",
                    value = germinationRate,
                    onValueChange = { germinationRate = it.filter { c -> c.isDigit() } },
                    keyboardType = KeyboardType.Number,
                    required = true,
                    error = if (showValidationErrors && germinationRate.toIntOrNull() == null) "Heltal krävs" else null,
                )
            }

            // Growing positions
            item {
                FaltetChipMultiSelector(
                    label = "Växtplats",
                    options = listOf("SUNNY", "PARTIALLY_SUNNY", "SHADOWY"),
                    selected = selectedPositions,
                    onSelectedChange = { selectedPositions = it },
                    labelFor = { positionLabelSv(it) },
                    required = true,
                )
            }

            // Soil types
            item {
                FaltetChipMultiSelector(
                    label = "Jordtyp",
                    options = listOf("CLAY", "SANDY", "LOAMY", "CHALKY", "PEATY", "SILTY"),
                    selected = selectedSoils,
                    onSelectedChange = { selectedSoils = it },
                    labelFor = { soilLabelSv(it) },
                    required = true,
                )
            }

            // Group dropdown
            item {
                val selectedGroup = uiState.groups.find { it.id == selectedGroupId }
                FaltetDropdown(
                    label = "Grupp (valfri)",
                    options = uiState.groups,
                    selected = selectedGroup,
                    onSelectedChange = { group -> selectedGroupId = group.id },
                    labelFor = { it.name },
                    searchable = false,
                )
            }

            // + NY GRUPP affordance
            item {
                Text(
                    text = "+ NY GRUPP",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    color = FaltetClay,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNewGroupDialog = true }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }

            // Tags — state is Set<Long> of IDs; convert at render boundary
            item {
                FaltetChipMultiSelector(
                    label = "Taggar (valfri)",
                    options = uiState.tags,
                    selected = uiState.tags.filter { it.id in selectedTagIds }.toSet(),
                    onSelectedChange = { newSet -> selectedTagIds = newSet.map { it.id }.toSet() },
                    labelFor = { it.name },
                )
            }

            // + NY TAGG affordance
            item {
                Text(
                    text = "+ NY TAGG",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    color = FaltetClay,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNewTagDialog = true }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: PlantSuggestion,
    onTap: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.commonName,
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = FaltetInk,
            )
            Text(
                text = suggestion.species.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = FaltetForest,
            )
        }
        Text(
            text = "${(suggestion.confidence * 100).toInt()}%",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = FaltetClay,
        )
    }
}

private fun monthShortSv(month: Int): String = arrayOf(
    "jan", "feb", "mar", "apr", "maj", "jun",
    "jul", "aug", "sep", "okt", "nov", "dec",
)[month - 1]

private fun positionLabelSv(code: String): String = when (code) {
    "SUNNY" -> "Sol"
    "PARTIALLY_SUNNY" -> "Halvskugga"
    "SHADOWY" -> "Skugga"
    else -> code
}

private fun soilLabelSv(code: String): String = when (code) {
    "CLAY" -> "Lera"
    "SANDY" -> "Sand"
    "LOAMY" -> "Mylla"
    "CHALKY" -> "Kalk"
    "PEATY" -> "Torv"
    "SILTY" -> "Silt"
    else -> code
}
