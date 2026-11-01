package app.verdant.android.ui.activity

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.verdant.android.data.model.PlantSuggestion
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.VarietyNames
import app.verdant.android.ui.faltet.FaltetChipMultiSelector
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetImagePicker
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20
import coil.compose.AsyncImage

private val PLANT_TYPES = listOf("ANNUAL", "BIENNIAL", "PERENNIAL", "BULB", "TUBER")
private val UNIT_TYPES = listOf("SEED", "PLUG", "BULB", "TUBER", "PLANT")

/**
 * Adding and editing a variety, laid out like the web form: supplier (new), photos (edit), names, growth,
 * months, conditions, suppliers (edit), tags, commercial figures and workflow. A catalogue variety is
 * shown read-only. Seed packet photos can fill in the form through identification and packet reading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpeciesScreen(
    onBack: () -> Unit,
    viewModel: AddSpeciesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEdit = viewModel.speciesId != null
    val readOnly = uiState.readOnly

    var form by remember { mutableStateOf(SpeciesFormValues()) }
    var initial by remember { mutableStateOf(SpeciesFormValues()) }
    var prefilled by remember { mutableStateOf(false) }
    var providerId by remember { mutableStateOf<Long?>(null) }
    var productUrl by remember { mutableStateOf("") }
    var imageFrontBase64 by remember { mutableStateOf<String?>(null) }
    var imageBackBase64 by remember { mutableStateOf<String?>(null) }
    var frontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var backBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.existingSpecies) {
        val s = uiState.existingSpecies
        if (s != null && !prefilled) {
            form = SpeciesFormValues.from(s)
            initial = form
            prefilled = true
        }
    }

    // Identification of a front photo fills in the names.
    LaunchedEffect(uiState.suggestions) {
        val top = uiState.suggestions.firstOrNull() ?: return@LaunchedEffect
        if (form.commonNameSv.isBlank()) form = form.copy(commonNameSv = top.commonName)
        if (form.scientificName.isBlank()) form = form.copy(scientificName = top.species)
        val box = top.cropBox
        val src = frontBitmap
        if (box != null && src != null) {
            val cropped = src.cropToBox(box)
            frontBitmap = cropped
            imageFrontBase64 = cropped.toCompressedBase64()
        }
    }

    // Reading the back of a seed packet fills in what it says.
    LaunchedEffect(uiState.extractedInfo) {
        val info = uiState.extractedInfo ?: return@LaunchedEffect
        var f = form
        if (f.commonNameSv.isBlank()) info.commonName?.let { f = f.copy(commonNameSv = it) }
        if (f.variantName.isBlank()) info.variantName?.let { f = f.copy(variantName = it) }
        if (f.variantNameSv.isBlank()) info.variantNameSv?.let { f = f.copy(variantNameSv = it) }
        if (f.scientificName.isBlank()) info.scientificName?.let { f = f.copy(scientificName = it) }
        if (f.germinationTimeDaysMin.isBlank()) info.germinationTimeDaysMin?.let { f = f.copy(germinationTimeDaysMin = it.toString()) }
        if (f.germinationTimeDaysMax.isBlank()) info.germinationTimeDaysMax?.let { f = f.copy(germinationTimeDaysMax = it.toString()) }
        if (f.daysToHarvestMin.isBlank()) info.daysToHarvestMin?.let { f = f.copy(daysToHarvestMin = it.toString()) }
        if (f.daysToHarvestMax.isBlank()) info.daysToHarvestMax?.let { f = f.copy(daysToHarvestMax = it.toString()) }
        if (f.sowingDepthMm.isBlank()) info.sowingDepthMm?.let { f = f.copy(sowingDepthMm = it.toString()) }
        if (f.heightCmMin.isBlank()) info.heightCmMin?.let { f = f.copy(heightCmMin = it.toString()) }
        if (f.heightCmMax.isBlank()) info.heightCmMax?.let { f = f.copy(heightCmMax = it.toString()) }
        if (f.germinationRate.isBlank()) info.germinationRate?.let { f = f.copy(germinationRate = it.toString()) }
        if (f.bloomMonths.isEmpty()) info.bloomMonths?.let { f = f.copy(bloomMonths = it.toSet()) }
        if (f.sowingMonths.isEmpty()) info.sowingMonths?.let { f = f.copy(sowingMonths = it.toSet()) }
        if (f.positions.isEmpty()) info.growingPositions?.let { f = f.copy(positions = it.toSet()) }
        if (f.soils.isEmpty()) info.soils?.let { f = f.copy(soils = it.toSet()) }
        form = f
        val box = info.cropBox
        val src = backBitmap
        if (box != null && src != null) {
            val cropped = src.cropToBox(box)
            backBitmap = cropped
            imageBackBase64 = cropped.toCompressedBase64()
        }
    }

    val hasChanges = !readOnly && (form != initial || imageFrontBase64 != null || imageBackBase64 != null ||
        (!isEdit && (providerId != null || productUrl.isNotBlank())))
    val tryBack: () -> Unit = { if (hasChanges) showDiscardDialog = true else onBack() }
    BackHandler(enabled = hasChanges) { tryBack() }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(form.commonNameSv, form.commonName, form.variantName, form.variantNameSv) {
        viewModel.checkDuplicates(VarietyNames(form.commonNameSv.ifBlank { form.commonName }, form.variantName, form.variantNameSv))
    }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(uiState.created) { if (uiState.created) onBack() }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Avbryt ändringar?") },
            text = { Text("Dina ändringar kommer att gå förlorade.") },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onBack() }) { Text("Avbryt ändringar", color = FaltetClay) }
            },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Fortsätt redigera") } },
        )
    }

    if (showNewTagDialog) {
        AlertDialog(
            onDismissRequest = { showNewTagDialog = false; newTagName = "" },
            title = { Text("Ny tagg") },
            text = { Field(label = "Namn", value = newTagName, onValueChange = { newTagName = it }, required = true) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createTag(newTagName.trim())
                        showNewTagDialog = false
                        newTagName = ""
                    },
                    enabled = newTagName.isNotBlank(),
                ) { Text("Skapa", color = FaltetAccent) }
            },
            dismissButton = { TextButton(onClick = { showNewTagDialog = false; newTagName = "" }) { Text("Avbryt") } },
        )
    }

    val submit: () -> Unit = {
        showValidationErrors = true
        if (form.hasName) {
            if (isEdit) {
                viewModel.updateSpecies(form.toUpdateRequest(imageFrontBase64, imageBackBase64), form.workflowTemplateId)
            } else {
                viewModel.createSpecies(
                    form.toCreateRequest(imageFrontBase64, imageBackBase64),
                    NewSpeciesExtras(providerId, productUrl.trim().ifBlank { null }, form.workflowTemplateId),
                )
            }
        }
    }

    val fillFromSuggestion: (PlantSuggestion) -> Unit = { suggestion ->
        form = form.copy(commonNameSv = suggestion.commonName, scientificName = suggestion.species)
        val box = suggestion.cropBox
        val src = frontBitmap
        if (box != null && src != null) {
            val cropped = src.cropToBox(box)
            frontBitmap = cropped
            imageFrontBase64 = cropped.toCompressedBase64()
        }
    }

    // A catalogue variety shows its details without letting them change.
    val edit: ((String) -> Unit) -> ((String) -> Unit)? = { change -> if (readOnly) null else change }
    val digits: (String) -> String = { it.filter { c -> c.isDigit() } }
    val existing = uiState.existingSpecies
    val title = when {
        !isEdit -> "Ny sort"
        existing == null -> "Sort"
        else -> listOfNotNull(existing.commonNameSv ?: existing.commonName, existing.variantNameSv ?: existing.variantName).joinToString(" – ")
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = title,
        mastheadRight = {
            IconButton(onClick = { tryBack() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka", tint = FaltetAccent, modifier = Modifier.size(18.dp))
            }
        },
        bottomBar = {
            if (!readOnly) {
                FaltetFormSubmitBar(
                    label = if (isEdit) "Spara ändringar" else "Skapa sort",
                    onClick = submit,
                    enabled = !uiState.isLoading && form.hasName,
                    submitting = uiState.isLoading,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (readOnly) {
                item {
                    Text(
                        text = "Det här är en sort från katalogen. Kopiera den i webbappen för att ändra uppgifterna.",
                        fontFamily = FaltetDisplay,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        color = FaltetForest,
                    )
                }
            }

            if (!readOnly) {
                // Seed packet photos: identification fills in the names, reading the back fills in the rest.
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            FaltetImagePicker(
                                label = "Fröpåse framsida (valfri)",
                                value = frontBitmap,
                                onValueChange = { bitmap ->
                                    frontBitmap = bitmap
                                    imageFrontBase64 = bitmap?.toCompressedBase64()?.also(viewModel::identifyPlant)
                                },
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            FaltetImagePicker(
                                label = "Fröpåse baksida (valfri)",
                                value = backBitmap,
                                onValueChange = { bitmap ->
                                    backBitmap = bitmap
                                    imageBackBase64 = bitmap?.toCompressedBase64()?.also(viewModel::extractSpeciesInfo)
                                },
                            )
                        }
                    }
                }
            }

            if (uiState.identifying || uiState.extracting) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = FaltetAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (uiState.identifying) "Identifierar…" else "Läser fröpåsen…",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            color = FaltetForest,
                        )
                    }
                }
            }

            if (uiState.suggestions.isNotEmpty()) {
                item { FaltetSectionHeader(label = "Förslag") }
                items(uiState.suggestions, key = { it.species }) { suggestion ->
                    SuggestionRow(suggestion = suggestion, onTap = { fillFromSuggestion(suggestion) })
                }
            }

            if (!isEdit) {
                item { FaltetSectionHeader(label = "Fröleverantör") }
                item {
                    FaltetDropdown(
                        label = "Leverantör",
                        options = uiState.providers,
                        selected = uiState.providers.find { it.id == providerId },
                        onSelectedChange = { providerId = it.id },
                        labelFor = { it.name },
                        searchable = uiState.providers.size > 8,
                        clearLabel = "Välj leverantör…",
                        onClear = { providerId = null; productUrl = "" },
                    )
                }
                if (providerId != null) {
                    item { Field(label = "Produkt-URL", value = productUrl, onValueChange = { productUrl = it }, keyboardType = KeyboardType.Uri) }
                }
            }

            if (isEdit && !readOnly && existing != null) {
                item { FaltetSectionHeader(label = "Fler foton") }
                item { PhotosRow(existing, uiState.updatingDetails, onAdd = { viewModel.uploadPhoto(it.toCompressedBase64(1280)) }, onDelete = viewModel::deletePhoto) }
            }

            item {
                FaltetSectionHeader(
                    label = "Grunduppgifter",
                    trailing = if (readOnly) null else {
                        {
                            Text(
                                text = "KOPIERA TILL ENGELSKA",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                letterSpacing = 1.4.sp,
                                color = FaltetAccent,
                                modifier = Modifier.clickable { form = form.copyToEnglish() },
                            )
                        }
                    },
                )
            }
            item {
                Field(
                    label = "Namn (svenska)",
                    value = form.commonNameSv,
                    onValueChange = edit { form = form.copy(commonNameSv = it) },
                    required = true,
                    error = if (showValidationErrors && !form.hasName) "Ange ett namn" else null,
                )
            }
            item { Field(label = "Namn (engelska)", value = form.commonName, onValueChange = edit { form = form.copy(commonName = it) }) }
            item { Field(label = "Sortnamn (svenska)", value = form.variantNameSv, onValueChange = edit { form = form.copy(variantNameSv = it) }) }
            item { Field(label = "Sortnamn (engelska)", value = form.variantName, onValueChange = edit { form = form.copy(variantName = it) }) }
            item { Field(label = "Vetenskapligt namn", value = form.scientificName, onValueChange = edit { form = form.copy(scientificName = it) }) }
            if (!isEdit && uiState.duplicates.isNotEmpty()) {
                item { DuplicateWarning(uiState.duplicates) }
            }

            item { FaltetSectionHeader(label = "Tillväxt") }
            item {
                RangeFields("Groningstid (dagar)", form.germinationTimeDaysMin, form.germinationTimeDaysMax,
                    edit { form = form.copy(germinationTimeDaysMin = digits(it)) }, edit { form = form.copy(germinationTimeDaysMax = digits(it)) })
            }
            item {
                RangeFields("Dagar till skörd", form.daysToHarvestMin, form.daysToHarvestMax,
                    edit { form = form.copy(daysToHarvestMin = digits(it)) }, edit { form = form.copy(daysToHarvestMax = digits(it)) })
            }
            item {
                RangeFields("Höjd (cm)", form.heightCmMin, form.heightCmMax,
                    edit { form = form.copy(heightCmMin = digits(it)) }, edit { form = form.copy(heightCmMax = digits(it)) })
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Field(label = "Sådjup (mm)", value = form.sowingDepthMm, onValueChange = edit { form = form.copy(sowingDepthMm = digits(it)) },
                        keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    Field(label = "Groningsgrad (%)", value = form.germinationRate, onValueChange = edit { form = form.copy(germinationRate = digits(it)) },
                        keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                }
            }

            item { FaltetSectionHeader(label = "Sådd- och blommånader") }
            item {
                FaltetChipMultiSelector(label = "Såmånader", options = (1..12).toList(), selected = form.sowingMonths,
                    onSelectedChange = { if (!readOnly) form = form.copy(sowingMonths = it) }, labelFor = ::monthShortSv)
            }
            item {
                FaltetChipMultiSelector(label = "Blommånader", options = (1..12).toList(), selected = form.bloomMonths,
                    onSelectedChange = { if (!readOnly) form = form.copy(bloomMonths = it) }, labelFor = ::monthShortSv)
            }

            item { FaltetSectionHeader(label = "Odlingsförhållanden") }
            item {
                FaltetChipMultiSelector(label = "Läge", options = listOf("SUNNY", "PARTIALLY_SUNNY", "SHADOWY"), selected = form.positions,
                    onSelectedChange = { if (!readOnly) form = form.copy(positions = it) }, labelFor = ::positionLabelSv)
            }
            item {
                FaltetChipMultiSelector(label = "Jordtyp", options = listOf("CLAY", "SANDY", "LOAMY", "CHALKY", "PEATY", "SILTY"), selected = form.soils,
                    onSelectedChange = { if (!readOnly) form = form.copy(soils = it) }, labelFor = ::soilLabelSv)
            }

            if (isEdit && existing != null) {
                item { FaltetSectionHeader(label = "Fröleverantörer") }
                item {
                    ProvidersSection(
                        species = existing,
                        providers = uiState.providers,
                        readOnly = readOnly,
                        busy = uiState.updatingDetails,
                        onAdd = viewModel::addProvider,
                        onRemove = viewModel::removeProvider,
                    )
                }
            }

            item {
                FaltetSectionHeader(
                    label = "Taggar",
                    trailing = if (readOnly) null else {
                        {
                            Text("+ NY TAGG", fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.4.sp, color = FaltetAccent,
                                modifier = Modifier.clickable { showNewTagDialog = true })
                        }
                    },
                )
            }
            item {
                FaltetChipMultiSelector(
                    label = "Taggar",
                    options = uiState.tags,
                    selected = uiState.tags.filter { it.id in form.tagIds }.toSet(),
                    onSelectedChange = { set -> if (!readOnly) form = form.copy(tagIds = set.map { it.id }.toSet()) },
                    labelFor = { it.name },
                )
            }

            item { FaltetSectionHeader(label = "Kommersiellt och klassificering") }
            item {
                // Price per seed and per package fill each other in through the package size.
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Field(label = "Frön per paket", value = form.seedsPerPackage, onValueChange = edit { form = form.withSeedsPerPackage(it) },
                        keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    Field(label = "Pris per paket (kr)", value = form.costPerPackage, onValueChange = edit { form = form.withCostPerPackage(it) },
                        keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                }
            }
            item {
                Field(label = "Kostnad per frö (kr)", value = form.costPerSeed, onValueChange = edit { form = form.withCostPerSeed(it) },
                    keyboardType = KeyboardType.Decimal)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Field(label = "Stjälkar per planta", value = form.expectedStemsPerPlant,
                        onValueChange = edit { form = form.copy(expectedStemsPerPlant = digits(it)) }, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    Field(label = "Vashållbarhet (dagar)", value = form.expectedVaseLifeDays,
                        onValueChange = edit { form = form.copy(expectedVaseLifeDays = digits(it)) }, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                }
            }
            item {
                FaltetDropdown(
                    label = "Växttyp",
                    options = PLANT_TYPES,
                    selected = form.plantType,
                    onSelectedChange = { if (!readOnly) form = form.copy(plantType = it) },
                    labelFor = ::plantTypeLabelSv,
                    searchable = false,
                    clearLabel = "—",
                    onClear = { if (!readOnly) form = form.copy(plantType = null) },
                )
            }
            item {
                FaltetDropdown(
                    label = "Standardenhet",
                    options = UNIT_TYPES,
                    selected = form.defaultUnitType,
                    onSelectedChange = { if (!readOnly) form = form.copy(defaultUnitType = it) },
                    labelFor = ::unitTypeLabelSv,
                    searchable = false,
                    clearLabel = "—",
                    onClear = { if (!readOnly) form = form.copy(defaultUnitType = null) },
                )
            }

            if (!readOnly) {
                item { FaltetSectionHeader(label = "Arbetsflöde") }
                item {
                    FaltetDropdown(
                        label = "Arbetsflödesmall",
                        options = uiState.templates,
                        selected = uiState.templates.find { it.id == form.workflowTemplateId },
                        onSelectedChange = { form = form.copy(workflowTemplateId = it.id) },
                        labelFor = { it.name },
                        searchable = uiState.templates.size > 8,
                        clearLabel = "— Inget —",
                        onClear = if (isEdit && initial.workflowTemplateId != null) null else ({ form = form.copy(workflowTemplateId = null) }),
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun RangeFields(label: String, min: String, max: String, onMin: ((String) -> Unit)?, onMax: ((String) -> Unit)?) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Field(label = "$label, min", value = min, onValueChange = onMin, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
        Field(label = "$label, max", value = max, onValueChange = onMax, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
    }
}

/** The variety's own photos; adding uploads at once and deleting removes at once, like on the web. */
@Composable
private fun PhotosRow(species: SpeciesResponse, busy: Boolean, onAdd: (Bitmap) -> Unit, onDelete: (Long) -> Unit) {
    var confirmDelete by remember { mutableStateOf<Long?>(null) }
    confirmDelete?.let { photoId ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Ta bort fotot?") },
            confirmButton = { TextButton(onClick = { onDelete(photoId); confirmDelete = null }) { Text("Ta bort", color = FaltetClay) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Avbryt") } },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (species.photos.isEmpty()) {
            Text("Ännu inga fler foton", fontFamily = FaltetDisplay, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = FaltetForest)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(species.photos.sortedBy { it.sortOrder }, key = { it.id }) { photo ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = photo.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).border(1.dp, FaltetInkLine20),
                        )
                        Text(
                            text = "TA BORT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            letterSpacing = 1.4.sp,
                            color = FaltetClay,
                            modifier = Modifier.clickable(enabled = !busy) { confirmDelete = photo.id }.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        FaltetImagePicker(label = if (busy) "Laddar upp…" else "Lägg till foto", value = null, onValueChange = { bitmap -> bitmap?.let(onAdd) })
    }
}

/** Linked seed suppliers with their product links; packet images on suppliers stay an admin task. */
@Composable
private fun ProvidersSection(
    species: SpeciesResponse,
    providers: List<app.verdant.android.data.model.ProviderResponse>,
    readOnly: Boolean,
    busy: Boolean,
    onAdd: (Long, String?) -> Unit,
    onRemove: (Long) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var providerId by remember { mutableStateOf<Long?>(null) }
    var productUrl by remember { mutableStateOf("") }
    val linked = species.providers.map { it.providerId }.toSet()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (species.providers.isEmpty()) {
            Text("Inga leverantörer kopplade än", fontFamily = FaltetDisplay, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = FaltetForest)
        }
        species.providers.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.providerName, fontFamily = FaltetDisplay, fontSize = 16.sp, color = FaltetInk)
                    p.productUrl?.let { Text(it, fontSize = 11.sp, color = FaltetForest, maxLines = 1) }
                }
                if (!readOnly) {
                    Text(
                        text = "TA BORT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                        color = FaltetClay,
                        modifier = Modifier.clickable(enabled = !busy) { onRemove(p.id) }.padding(start = 10.dp),
                    )
                }
            }
        }
        if (!readOnly) {
            if (!adding) {
                Text(
                    text = "+ LÄGG TILL LEVERANTÖR",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    color = FaltetAccent,
                    modifier = Modifier.clickable { adding = true },
                )
            } else {
                val available = providers.filter { it.id !in linked }
                FaltetDropdown(
                    label = "Leverantör",
                    options = available,
                    selected = available.find { it.id == providerId },
                    onSelectedChange = { providerId = it.id },
                    labelFor = { it.name },
                    searchable = available.size > 8,
                )
                Field(label = "Produkt-URL", value = productUrl, onValueChange = { productUrl = it }, keyboardType = KeyboardType.Uri)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            providerId?.let { onAdd(it, productUrl) }
                            adding = false; providerId = null; productUrl = ""
                        },
                        enabled = providerId != null && !busy,
                    ) { Text("Lägg till", color = FaltetAccent) }
                    TextButton(onClick = { adding = false; providerId = null; productUrl = "" }) { Text("Avbryt", color = FaltetForest) }
                }
            }
        }
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

private fun plantTypeLabelSv(code: String): String = when (code) {
    "ANNUAL" -> "Ettårig"
    "BIENNIAL" -> "Tvåårig"
    "PERENNIAL" -> "Flerårig"
    "BULB" -> "Lök"
    "TUBER" -> "Knöl"
    else -> code
}

private fun unitTypeLabelSv(code: String): String = when (code) {
    "SEED" -> "Frö"
    "PLUG" -> "Plugg"
    "BULB" -> "Lök"
    "TUBER" -> "Knöl"
    "PLANT" -> "Planta"
    else -> code
}

@Composable
private fun DuplicateWarning(duplicates: List<SpeciesResponse>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FaltetAccent)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = "Den här sorten finns redan. Använd den befintliga sorten i stället för att lägga till en ny.",
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 14.sp,
            color = FaltetForest,
        )
        duplicates.forEach { d ->
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    text = listOfNotNull(d.commonNameSv ?: d.commonName, d.variantNameSv ?: d.variantName).joinToString(" – "),
                    fontFamily = FaltetDisplay,
                    fontSize = 16.sp,
                    color = FaltetInk,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (d.isSystem) "KATALOGEN" else "DINA SORTER",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    color = FaltetForest,
                )
            }
        }
    }
}
