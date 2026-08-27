package app.verdant.android.ui.area

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.verdant.android.R
import app.verdant.android.data.model.AREA_CATEGORIES
import app.verdant.android.data.model.GardenAreaEventResponse
import app.verdant.android.data.model.MaintenanceTarget
import app.verdant.android.data.model.UpdateMaintenanceRuleRequest
import app.verdant.android.data.model.activitiesForTarget
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.BotanicalPlate
import app.verdant.android.ui.faltet.Chip
import app.verdant.android.ui.faltet.FaltetChipSelector
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.faltet.FaltetTone
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.faltet.rememberUnsavedChangesGuard
import app.verdant.android.ui.maintenance.MaintenanceRulesSection
import app.verdant.android.ui.maintenance.activityLabelRes
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

/** Resolves an area category code to its localized display string resource. */
fun areaCategoryLabelRes(category: String): Int = when (category) {
    "WALKWAY" -> R.string.area_category_walkway
    "LAWN" -> R.string.area_category_lawn
    "HEDGE" -> R.string.area_category_hedge
    "COMPOST" -> R.string.area_category_compost
    "GREENHOUSE" -> R.string.area_category_greenhouse
    "WATER_FEATURE" -> R.string.area_category_water_feature
    "STRUCTURE" -> R.string.area_category_structure
    "OTHER" -> R.string.area_category_other
    else -> R.string.area_category_other
}

private fun formattedEventDate(dateIso: String): String = try {
    val d = java.time.LocalDate.parse(dateIso)
    "%02d.%02d.%04d".format(d.dayOfMonth, d.monthValue, d.year)
} catch (e: Exception) {
    dateIso
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenAreaDetailScreen(
    onBack: () -> Unit,
    onGardenClick: (Long) -> Unit = {},
    viewModel: GardenAreaDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rulesState by viewModel.rulesController.state.collectAsStateWithLifecycle()
    val loaded = uiState as? GardenAreaDetailUiState.Loaded
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
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
    var editCategory by remember { mutableStateOf<String?>(null) }
    var editSizeText by remember { mutableStateOf("") }

    LaunchedEffect(loaded?.deleted) {
        if (loaded?.deleted == true) onBack()
    }

    val source = loaded?.area
    val editDirty = editing && source != null && (
        editName != source.name ||
            editDescription != (source.description ?: "") ||
            editCategory != source.category ||
            editSizeText != (source.sizeSqm?.toString() ?: "")
        )
    val editGuard = rememberUnsavedChangesGuard(editDirty)
    editGuard.RenderConfirmDialog()

    if (editing) {
        val categoryLabels = AREA_CATEGORIES.associateWith { stringResource(areaCategoryLabelRes(it)) }
        AlertDialog(
            onDismissRequest = editGuard.requestDismiss { editing = false },
            title = { Text(stringResource(R.string.area_edit_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Field(
                        label = stringResource(R.string.area_field_name),
                        value = editName,
                        onValueChange = { editName = it },
                        required = true,
                    )
                    Field(
                        label = stringResource(R.string.area_field_description),
                        value = editDescription,
                        onValueChange = { editDescription = it },
                    )
                    FaltetChipSelector(
                        label = stringResource(R.string.area_field_category),
                        options = AREA_CATEGORIES,
                        selected = editCategory,
                        onSelectedChange = { it?.let { editCategory = it } },
                        labelFor = { categoryLabels[it] ?: it },
                        required = true,
                    )
                    Field(
                        label = stringResource(R.string.area_field_size),
                        value = editSizeText,
                        onValueChange = { editSizeText = it },
                        keyboardType = KeyboardType.Decimal,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.update(
                            name = editName,
                            description = editDescription.takeIf { it.isNotBlank() },
                            category = editCategory ?: source?.category ?: AREA_CATEGORIES.first(),
                            sizeSqm = editSizeText.toDoubleOrNull(),
                        )
                        editing = false
                    },
                    enabled = editName.isNotBlank() && editCategory != null,
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showLogDialog) {
        LogMaintenanceDialog(
            onDismiss = { showLogDialog = false },
            onLog = { activityType, notes ->
                viewModel.logEvent(activityType, notes)
                showLogDialog = false
            },
        )
    }

    if (showDeleteDialog && loaded?.area != null) {
        val area = loaded.area
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.area_delete_title)) },
            text = { Text(stringResource(R.string.area_delete_confirm, area.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete()
                    showDeleteDialog = false
                }) { Text(stringResource(R.string.delete), color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = loaded?.area?.gardenName ?: "",
        onMastheadLeftClick = loaded?.area?.gardenId?.let { gid -> { onGardenClick(gid) } },
        mastheadCenter = "",
        mastheadRight = {
            if (loaded?.area != null) {
                val area = loaded.area
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            editName = area.name
                            editDescription = area.description ?: ""
                            editCategory = area.category
                            editSizeText = area.sizeSqm?.toString() ?: ""
                            editing = true
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = FaltetAccent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            tint = FaltetClay,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
        fab = {
            loaded?.area?.let {
                FaltetFab(
                    onClick = { showLogDialog = true },
                    contentDescription = stringResource(R.string.area_log_maintenance_title),
                    icon = Icons.Default.Bolt,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        watermark = BotanicalPlate.EmptyGarden,
    ) { padding ->
        when (uiState) {
            is GardenAreaDetailUiState.Loading -> FaltetLoadingState(Modifier.padding(padding))
            is GardenAreaDetailUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            is GardenAreaDetailUiState.Loaded -> {
                val state = uiState as GardenAreaDetailUiState.Loaded
                val area = state.area
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = area.name,
                                fontFamily = FaltetDisplay,
                                fontStyle = FontStyle.Italic,
                                fontSize = 24.sp,
                                color = FaltetInk,
                            )
                            Chip(text = stringResource(areaCategoryLabelRes(area.category)), tone = FaltetTone.Sky)
                            if (!area.description.isNullOrBlank()) {
                                Text(
                                    text = area.description!!,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.4.sp,
                                    color = FaltetForest,
                                )
                            }
                        }
                    }

                    item {
                        MaintenanceRulesSection(
                            state = rulesState,
                            target = MaintenanceTarget.AREA,
                            onCreate = { activityType, intervalDays, sm, sd, em, ed, notes ->
                                viewModel.rulesController.create(
                                    activityType = activityType,
                                    intervalDays = intervalDays,
                                    seasonStartMonth = sm,
                                    seasonStartDay = sd,
                                    seasonEndMonth = em,
                                    seasonEndDay = ed,
                                    notes = notes,
                                )
                            },
                            onUpdate = viewModel.rulesController::update,
                            onClearSeasonWindow = viewModel.rulesController::clearSeasonWindow,
                            onDelete = viewModel.rulesController::delete,
                            onToggleActive = { id, active ->
                                viewModel.rulesController.update(id, UpdateMaintenanceRuleRequest(active = active))
                            },
                        )
                    }

                    item { FaltetSectionHeader(label = stringResource(R.string.area_history_section_title)) }
                    if (state.events.isEmpty()) {
                        item { InlineEmptyHistory() }
                    } else {
                        items(state.events, key = { "aev_${it.id}" }) { ev ->
                            AreaEventRow(ev)
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AreaEventRow(ev: GardenAreaEventResponse) {
    val secondary = ev.notes?.takeIf { it.isNotBlank() }?.let { "“$it”" }
    FaltetListRow(
        title = stringResource(activityLabelRes(ev.eventType)),
        meta = secondary,
        metaMaxLines = 2,
        stat = {
            Text(
                formattedEventDate(ev.eventDate.take(10)),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = FaltetForest,
            )
        },
    )
}

@Composable
private fun InlineEmptyHistory() {
    Text(
        text = stringResource(R.string.area_history_empty),
        fontFamily = FaltetDisplay,
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        color = FaltetForest,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
    )
}

@Composable
private fun LogMaintenanceDialog(
    onDismiss: () -> Unit,
    onLog: (activityType: String, notes: String?) -> Unit,
) {
    val options = remember { activitiesForTarget(MaintenanceTarget.AREA) + "NOTE" }
    val labels = options.associateWith { stringResource(activityLabelRes(it)) }
    var activityType by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.area_log_maintenance_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FaltetChipSelector(
                    label = stringResource(R.string.maintenance_activity_label),
                    options = options,
                    selected = activityType,
                    onSelectedChange = { activityType = it },
                    labelFor = { labels[it] ?: it },
                    required = true,
                )
                Field(
                    label = stringResource(R.string.maintenance_activity_notes_label),
                    value = notes,
                    onValueChange = { notes = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = activityType != null,
                onClick = {
                    activityType?.let { onLog(it, notes.takeIf { n -> n.isNotBlank() }) }
                },
            ) { Text(stringResource(R.string.area_log_maintenance_submit)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
