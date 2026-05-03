package app.verdant.android.ui.bed
import app.verdant.android.ui.faltet.BotanicalPlate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetMetadataRow
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedDetailScreen(
    onBack: () -> Unit,
    onPlantClick: (Long) -> Unit,
    onSowInBed: (Long) -> Unit = {},
    onPlantFromTray: (Long) -> Unit = {},
    onFertilize: (Long) -> Unit = {},
    onGardenClick: (Long) -> Unit = {},
    onBedCopied: (Long) -> Unit = {},
    openEditOnStart: Boolean = false,
    viewModel: BedDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loaded = uiState as? BedDetailUiState.Loaded
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(loaded?.toastMessage) {
        loaded?.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeToast()
        }
    }
    var editing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }

    // Edit conditions state
    var editSoilType by remember { mutableStateOf<String?>(null) }
    var editSoilPhText by remember { mutableStateOf("") }
    var editSunExposure by remember { mutableStateOf<String?>(null) }
    var editSunDirections by remember { mutableStateOf<Set<String>>(emptySet()) }
    var editDrainage by remember { mutableStateOf<String?>(null) }
    var editIrrigationType by remember { mutableStateOf<String?>(null) }
    var editProtection by remember { mutableStateOf<String?>(null) }
    var editRaisedBed by remember { mutableStateOf<Boolean?>(null) }
    var editConditionsExpanded by remember { mutableStateOf(false) }

    val editSoilPhError = editSoilPhText.isNotBlank() &&
        editSoilPhText.toDoubleOrNull().let { it == null || it < 3.0 || it > 9.0 }

    LaunchedEffect(loaded?.deleted) {
        if (loaded?.deleted == true) onBack()
    }

    var didAutoOpenEdit by remember { mutableStateOf(false) }
    LaunchedEffect(loaded?.bed) {
        val bed = loaded?.bed
        if (openEditOnStart && !didAutoOpenEdit && bed != null) {
            editName = bed.name
            editDescription = bed.description ?: ""
            editSoilType = bed.soilType
            editSoilPhText = bed.soilPh?.toString() ?: ""
            editSunExposure = bed.sunExposure
            editSunDirections = bed.sunDirections?.toSet() ?: emptySet()
            editDrainage = bed.drainage
            editIrrigationType = bed.irrigationType
            editProtection = bed.protection
            editRaisedBed = bed.raisedBed
            editConditionsExpanded = listOf(
                bed.soilType, bed.soilPh, bed.sunExposure, bed.drainage,
                bed.sunDirections, bed.irrigationType, bed.protection, bed.raisedBed,
            ).any { it != null }
            editing = true
            didAutoOpenEdit = true
        }
    }

    val source = loaded?.bed
    val editDirty = editing && source != null && (
        editName != source.name ||
            editDescription != (source.description ?: "") ||
            editSoilType != source.soilType ||
            editSoilPhText != (source.soilPh?.toString() ?: "") ||
            editSunExposure != source.sunExposure ||
            editSunDirections != (source.sunDirections?.toSet() ?: emptySet<String>()) ||
            editDrainage != source.drainage ||
            editIrrigationType != source.irrigationType ||
            editProtection != source.protection ||
            editRaisedBed != source.raisedBed
    )
    val editGuard = app.verdant.android.ui.faltet.rememberUnsavedChangesGuard(editDirty)
    editGuard.RenderConfirmDialog()

    if (editing) {
        AlertDialog(
            onDismissRequest = editGuard.requestDismiss { editing = false },
            title = { Text("Redigera bädd") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Field(
                        label = "Namn",
                        value = editName,
                        onValueChange = { editName = it },
                        required = true,
                    )
                    Field(
                        label = "Beskrivning (valfri)",
                        value = editDescription,
                        onValueChange = { editDescription = it },
                    )

                    // Conditions section in edit dialog
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Förhållanden",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { editConditionsExpanded = !editConditionsExpanded }) {
                                    Icon(
                                        if (editConditionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null
                                    )
                                }
                            }
                            AnimatedVisibility(visible = editConditionsExpanded) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    BedConditionsFields(
                                        soilType = editSoilType,
                                        onSoilTypeChange = { editSoilType = it },
                                        soilPhText = editSoilPhText,
                                        onSoilPhTextChange = { editSoilPhText = it },
                                        soilPhError = editSoilPhError,
                                        sunExposure = editSunExposure,
                                        onSunExposureChange = { editSunExposure = it },
                                        sunDirections = editSunDirections,
                                        onSunDirectionsChange = { editSunDirections = it },
                                        drainage = editDrainage,
                                        onDrainageChange = { editDrainage = it },
                                        irrigationType = editIrrigationType,
                                        onIrrigationTypeChange = { editIrrigationType = it },
                                        protection = editProtection,
                                        onProtectionChange = { editProtection = it },
                                        raisedBed = editRaisedBed,
                                        onRaisedBedChange = { editRaisedBed = it }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.update(
                            name = editName,
                            description = editDescription,
                            soilType = editSoilType,
                            soilPh = editSoilPhText.toDoubleOrNull(),
                            sunExposure = editSunExposure,
                            drainage = editDrainage,
                            sunDirections = editSunDirections,
                            irrigationType = editIrrigationType,
                            protection = editProtection,
                            raisedBed = editRaisedBed
                        )
                        editing = false
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text("Spara")
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text("Avbryt") }
            }
        )
    }

    if (showAddDialog) {
        val bedId = loaded?.bed?.id ?: 0L
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Åtgärd") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Button(
                        onClick = { showAddDialog = false; onSowInBed(bedId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Så frön i bädd") }
                    androidx.compose.material3.Button(
                        onClick = { showAddDialog = false; onPlantFromTray(bedId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Plantera från bricka") }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showAddDialog = false; onFertilize(bedId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Gödsla") }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showAddDialog = false; viewModel.water() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Vattna") }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showAddDialog = false; viewModel.weed() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Rensa ogräs") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Avbryt") }
            }
        )
    }

    if (showDeleteDialog && loaded?.bed != null) {
        val bed = loaded.bed
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Ta bort bädd") },
            text = { Text("Vill du ta bort bädden \"${bed.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete()
                    showDeleteDialog = false
                    onBack()
                }) { Text("Ta bort", color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Avbryt") }
            }
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = loaded?.gardenName ?: "",
        onMastheadLeftClick = loaded?.bed?.gardenId?.let { gid -> { onGardenClick(gid) } },
        mastheadCenter = "",
        mastheadRight = {
            if (loaded?.bed != null) {
                val bed = loaded.bed
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            editName = bed.name
                            editDescription = bed.description ?: ""
                            editSoilType = bed.soilType
                            editSoilPhText = bed.soilPh?.toString() ?: ""
                            editSunExposure = bed.sunExposure
                            editSunDirections = bed.sunDirections?.toSet() ?: emptySet()
                            editDrainage = bed.drainage
                            editIrrigationType = bed.irrigationType
                            editProtection = bed.protection
                            editRaisedBed = bed.raisedBed
                            editConditionsExpanded = listOf(
                                bed.soilType, bed.soilPh?.toString(), bed.sunExposure,
                                bed.drainage, bed.irrigationType, bed.protection,
                                bed.raisedBed?.toString()
                            ).any { it != null } || !bed.sunDirections.isNullOrEmpty()
                            editing = true
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Default.Edit, "Redigera", tint = FaltetAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { viewModel.copy(onBedCopied) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Default.ContentCopy, "Kopiera", tint = FaltetAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Default.DeleteOutline, "Ta bort", tint = FaltetClay, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        fab = {
            loaded?.bed?.let {
                FaltetFab(
                    onClick = { showAddDialog = true },
                    contentDescription = "Åtgärd",
                    icon = Icons.Default.Bolt,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        watermark = BotanicalPlate.EmptyGarden,
) { padding ->
        when (val state = uiState) {
            is BedDetailUiState.Loading -> FaltetLoadingState(Modifier.padding(padding))
            is BedDetailUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            is BedDetailUiState.Loaded -> {
                val bed = state.bed
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = bed.name,
                                fontFamily = FaltetDisplay,
                                fontStyle = FontStyle.Italic,
                                fontSize = 24.sp,
                                color = FaltetInk,
                            )
                            if (!bed.description.isNullOrBlank()) {
                                Text(
                                    text = bed.description!!,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.4.sp,
                                    color = FaltetForest,
                                )
                            }
                        }
                    }

                    // Skötsel section — bed-level maintenance log (water, weed, …).
                    // Placed above the slower-changing sections so the most
                    // recently mutated information is visible first.
                    item { FaltetSectionHeader(label = "Skötsel") }
                    if (state.bedEvents.isEmpty()) {
                        item { InlineEmpty("Inga skötselhändelser ännu.") }
                    } else {
                        items(state.bedEvents, key = { "be_${it.id}" }) { ev ->
                            val secondary = buildList {
                                ev.plantsAffected?.takeIf { it > 0 }?.let { add("$it plantor") }
                                ev.notes?.takeIf { it.isNotBlank() }?.let { add("“$it”") }
                            }.joinToString(" · ").takeIf { it.isNotBlank() }
                            FaltetListRow(
                                title = bedEventLabelSv(ev.eventType),
                                meta = secondary,
                                metaMaxLines = 2,
                                stat = {
                                    Text(
                                        formattedDate(ev.eventDate.take(10)).uppercase(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.2.sp,
                                        color = FaltetForest,
                                    )
                                },
                            )
                        }
                    }

                    // Förhållanden section
                    val hasAnyCondition = listOf(
                        bed.soilType, bed.soilPh?.toString(), bed.drainage,
                        bed.raisedBed?.toString(),
                        bed.sunExposure,
                        bed.irrigationType,
                        bed.protection,
                    ).any { it != null } || !bed.sunDirections.isNullOrEmpty()
                    if (hasAnyCondition) {
                        item { FaltetSectionHeader(label = "Förhållanden") }
                        item {
                            ConditionGroup(
                                title = "Jord",
                                summary = listOfNotNull(
                                    bed.soilType?.let { bedSoilTypeLabel(it) },
                                    bed.soilPh?.let { "pH $it" },
                                ).joinToString(" · ").takeIf { it.isNotBlank() },
                                fields = listOf(
                                    "Jordtyp" to bed.soilType?.let { bedSoilTypeLabel(it) },
                                    "pH" to bed.soilPh?.toString(),
                                    "Dränering" to bed.drainage?.let { bedDrainageLabel(it) },
                                    "Upphöjd bädd" to bed.raisedBed?.let { if (it) "Ja" else "Nej" },
                                ),
                            )
                        }
                        item {
                            val sunDirSummary = bed.sunDirections?.joinToString(" · ")?.ifBlank { null }
                            ConditionGroup(
                                title = "Exponering",
                                summary = listOfNotNull(
                                    bed.sunExposure?.let { bedSunExposureLabel(it) },
                                    sunDirSummary,
                                ).joinToString(" · ").takeIf { it.isNotBlank() },
                                fields = listOf(
                                    "Sol" to bed.sunExposure?.let { bedSunExposureLabel(it) },
                                    "Sol från" to sunDirSummary,
                                ),
                            )
                        }
                        item {
                            ConditionGroup(
                                title = "Bevattning",
                                summary = bed.irrigationType?.let { bedIrrigationTypeLabel(it) },
                                fields = listOf(
                                    "Bevattning" to bed.irrigationType?.let { bedIrrigationTypeLabel(it) },
                                ),
                            )
                        }
                        item {
                            ConditionGroup(
                                title = "Skydd",
                                summary = bed.protection?.let { bedProtectionLabel(it) },
                                fields = listOf(
                                    "Skydd" to bed.protection?.let { bedProtectionLabel(it) },
                                ),
                            )
                        }
                    }

                    // Plantor section
                    item { FaltetSectionHeader(label = "Plantor") }
                    if (state.plants.isEmpty()) {
                        item { InlineEmpty("Inga plantor ännu.") }
                    } else {
                        items(state.plants, key = { it.id }) { plant ->
                            val count = plant.survivingCount ?: plant.seedCount
                            FaltetListRow(
                                title = plant.name,
                                meta = "${statusLabelSv(plant.status)} · ${formattedDate(plant.plantedDate)}",
                                stat = if (count != null && count > 1) {
                                    {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text("$count", fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = FaltetInk)
                                            Text(" STK", fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.2.sp, color = FaltetForest)
                                        }
                                    }
                                } else null,
                                onClick = { onPlantClick(plant.id) },
                            )
                        }
                    }

                    // Gödsling & vatten section
                    item { FaltetSectionHeader(label = "Gödsling & vatten") }
                    if (state.applications.isEmpty()) {
                        item { InlineEmpty("Inga gödslingar ännu.") }
                    } else {
                        items(state.applications, key = { it.id }) { application ->
                            FaltetListRow(
                                title = application.supplyTypeName,
                                meta = formattedDate(application.appliedAt.take(10)),
                                stat = {
                                    Text(
                                        "${application.quantity} ${application.supplyUnit.lowercase()}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        color = FaltetInk,
                                    )
                                },
                            )
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ConditionGroup(
    title: String,
    summary: String?,
    fields: List<Pair<String, String?>>,
) {
    if (fields.all { it.second == null }) return
    var expanded by remember { mutableStateOf(false) }
    Column {
        FaltetListRow(
            title = title,
            meta = summary,
            actions = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Dölj" else "Visa",
                    tint = FaltetForest,
                    modifier = Modifier.size(18.dp),
                )
            },
            onClick = { expanded = !expanded },
        )
        AnimatedVisibility(visible = expanded) {
            Column {
                fields.forEach { (label, value) ->
                    if (value != null) FaltetMetadataRow(label = label, value = value)
                }
            }
        }
    }
}

@Composable
private fun InlineEmpty(text: String) {
    Text(
        text = text,
        fontFamily = FaltetDisplay,
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        color = FaltetForest,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun ConditionGroupPreview() {
    Column {
        ConditionGroup(
            title = "Jord",
            summary = "Lerhaltig · pH 6.5",
            fields = listOf(
                "Jordtyp" to "Lerhaltig",
                "pH" to "6.5",
                "Dränering" to "God",
                "Upphöjd bädd" to "Ja",
            ),
        )
        ConditionGroup(
            title = "Exponering",
            summary = "Fullt sol · Söder",
            fields = listOf(
                "Sol" to "Fullt sol",
                "Väderstreck" to "Söder",
            ),
        )
    }
}
