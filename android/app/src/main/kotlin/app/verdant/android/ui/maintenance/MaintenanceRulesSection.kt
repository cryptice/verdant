// android/app/src/main/kotlin/app/verdant/android/ui/maintenance/MaintenanceRulesSection.kt
package app.verdant.android.ui.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.R
import app.verdant.android.data.model.DueState
import app.verdant.android.data.model.MaintenanceRuleResponse
import app.verdant.android.data.model.MaintenanceTarget
import app.verdant.android.data.model.UpdateMaintenanceRuleRequest
import app.verdant.android.data.model.activitiesForTarget
import app.verdant.android.data.model.dueState
import app.verdant.android.data.model.hasSeasonWindow
import app.verdant.android.data.model.maintenanceRuleUpdate
import app.verdant.android.data.model.seasonWindowMonthDays
import app.verdant.android.ui.common.InlineErrorBanner
import app.verdant.android.ui.faltet.Chip
import app.verdant.android.ui.faltet.FaltetCheckbox
import app.verdant.android.ui.faltet.FaltetChipSelector
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.faltet.FaltetStepper
import app.verdant.android.ui.faltet.FaltetTone
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

/**
 * Resolves an activity/event type code (e.g. `"WEED"`, `"NOTE"`) to its
 * localized display string resource. Shared by the maintenance rule rows
 * here and by the bed/area event history lists, so an activity is never
 * rendered as a hardcoded Swedish string.
 */
fun activityLabelRes(activityType: String): Int = when (activityType) {
    "WATER" -> R.string.maintenance_activity_water
    "WEED" -> R.string.maintenance_activity_weed
    "FERTILIZE" -> R.string.maintenance_activity_fertilize
    "MOW" -> R.string.maintenance_activity_mow
    "RAKE" -> R.string.maintenance_activity_rake
    "PRUNE" -> R.string.maintenance_activity_prune
    "EDGE" -> R.string.maintenance_activity_edge
    "SWEEP" -> R.string.maintenance_activity_sweep
    "TOP_UP" -> R.string.maintenance_activity_top_up
    "CLEAN" -> R.string.maintenance_activity_clean
    "INSPECT" -> R.string.maintenance_activity_inspect
    "NOTE" -> R.string.maintenance_activity_note
    else -> R.string.maintenance_activity_note
}

/**
 * Longest a month can be, so the day picker cannot offer a date the server
 * rejects — Feb 29 is valid in a season window, which is a month-day pair
 * with no year, so a leap year is the right yardstick.
 */
internal fun daysInMonth(month: Int): Int =
    java.time.YearMonth.of(2024, month.coerceIn(1, 12)).lengthOfMonth()

/** `(month, day)` formatted locale-agnostically as `"DD.MM"`. */
private fun monthDay(month: Int, day: Int): String = "%02d.%02d".format(day, month)

/**
 * Renders the maintenance-rules list for a single target (a bed or a
 * garden area) plus its add/edit/delete/pause affordances. Stateless and
 * target-generic: the caller owns [MaintenanceRulesState] (from
 * [MaintenanceRulesController]) and every mutation is a callback, so this
 * same composable mounts on both the area detail screen and the bed detail
 * screen with only [target] differing.
 */
@Composable
fun MaintenanceRulesSection(
    state: MaintenanceRulesState,
    target: MaintenanceTarget,
    onCreate: (
        activityType: String,
        intervalDays: Int,
        seasonStartMonth: Int?,
        seasonStartDay: Int?,
        seasonEndMonth: Int?,
        seasonEndDay: Int?,
        notes: String?,
    ) -> Unit,
    onUpdate: (id: Long, request: UpdateMaintenanceRuleRequest) -> Unit,
    onDelete: (id: Long) -> Unit,
    onToggleActive: (id: Long, active: Boolean) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
    today: String = java.time.LocalDate.now().toString(),
) {
    var editingRule by remember { mutableStateOf<MaintenanceRuleResponse?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var ruleToDelete by remember { mutableStateOf<Long?>(null) }

    Column(modifier.fillMaxWidth()) {
        FaltetSectionHeader(
            label = stringResource(R.string.maintenance_section_title),
            trailing = {
                TextButton(onClick = { editingRule = null; showEditSheet = true }) {
                    Text(stringResource(R.string.maintenance_add_rule), color = FaltetAccent, fontSize = 12.sp)
                }
            },
        )
        state.error?.let { error ->
            InlineErrorBanner(
                message = error,
                onDismiss = onDismissError,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
        if (state.rules.isEmpty()) {
            // Only claim there is nothing here once we have actually looked —
            // otherwise every RESUME flashes "nothing recurs here yet" at a
            // target that has rules.
            if (!state.isLoading) EmptyRulesState()
        } else {
            state.rules.forEach { rule ->
                RuleRow(
                    rule = rule,
                    today = today,
                    onEdit = { editingRule = rule; showEditSheet = true },
                    onDelete = { ruleToDelete = rule.id },
                    onToggleActive = { active -> onToggleActive(rule.id, active) },
                )
            }
        }
    }

    if (showEditSheet) {
        MaintenanceRuleEditSheet(
            target = target,
            rule = editingRule,
            onDismiss = { showEditSheet = false },
            onCreate = { activityType, intervalDays, sm, sd, em, ed, notes ->
                onCreate(activityType, intervalDays, sm, sd, em, ed, notes)
                showEditSheet = false
            },
            onUpdate = { id, request ->
                onUpdate(id, request)
                showEditSheet = false
            },
        )
    }

    ruleToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text(stringResource(R.string.maintenance_delete_rule_title)) },
            text = { Text(stringResource(R.string.maintenance_delete_rule_confirm)) },
            confirmButton = {
                TextButton(onClick = { onDelete(id); ruleToDelete = null }) {
                    Text(stringResource(R.string.delete), color = FaltetClay)
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun RuleRow(
    rule: MaintenanceRuleResponse,
    today: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
) {
    val meta = buildList {
        // "Every 1 days" / "Var 1:e dag" are both wrong; Swedish also wants
        // "Varannan dag" rather than "Var 2:e dag" for a two-day interval.
        add(
            if (rule.intervalDays == 2) {
                stringResource(R.string.maintenance_interval_every_other_day)
            } else {
                pluralStringResource(
                    R.plurals.maintenance_interval_summary,
                    rule.intervalDays,
                    rule.intervalDays,
                )
            },
        )
        if (hasSeasonWindow(rule)) {
            val (start, end) = seasonWindowMonthDays(rule)!!
            add(
                stringResource(
                    R.string.maintenance_season_window_summary,
                    monthDay(start.first, start.second),
                    monthDay(end.first, end.second),
                ),
            )
        }
    }.joinToString(" · ")

    FaltetListRow(
        title = stringResource(activityLabelRes(rule.activityType)),
        meta = meta,
        stat = { DueBadge(dueState(rule, today)) },
        actions = {
            IconButton(onClick = { onToggleActive(!rule.active) }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (rule.active) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        if (rule.active) R.string.maintenance_pause else R.string.maintenance_resume,
                    ),
                    tint = FaltetForest,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = FaltetAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete),
                    tint = FaltetClay,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
}

@Composable
private fun DueBadge(state: DueState) {
    val (label, tone) = when (state) {
        is DueState.Overdue -> stringResource(R.string.maintenance_due_overdue, state.days) to FaltetTone.Clay
        DueState.Due -> stringResource(R.string.maintenance_due_today) to FaltetTone.Mustard
        is DueState.Upcoming -> stringResource(R.string.maintenance_due_upcoming, state.days) to FaltetTone.Sage
        DueState.Inactive -> stringResource(R.string.maintenance_due_paused) to FaltetTone.Forest
        // A date this client cannot read: show nothing rather than guess.
        DueState.Unknown -> return
    }
    Chip(text = label, tone = tone, filled = state is DueState.Overdue)
}

@Composable
private fun EmptyRulesState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.maintenance_empty_title),
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
            color = FaltetInk,
        )
        Text(
            text = stringResource(R.string.maintenance_empty_subtitle),
            fontSize = 13.sp,
            color = FaltetForest,
        )
    }
}

@Composable
private fun MaintenanceRuleEditSheet(
    target: MaintenanceTarget,
    rule: MaintenanceRuleResponse?,
    onDismiss: () -> Unit,
    onCreate: (
        activityType: String,
        intervalDays: Int,
        seasonStartMonth: Int?,
        seasonStartDay: Int?,
        seasonEndMonth: Int?,
        seasonEndDay: Int?,
        notes: String?,
    ) -> Unit,
    onUpdate: (id: Long, request: UpdateMaintenanceRuleRequest) -> Unit,
) {
    val activityOptions = activitiesForTarget(target)
    val activityLabels = activityOptions.associateWith { stringResource(activityLabelRes(it)) }

    var activityType by remember(rule) { mutableStateOf(rule?.activityType ?: activityOptions.first()) }
    var intervalDays by remember(rule) { mutableStateOf(rule?.intervalDays ?: 7) }
    var notes by remember(rule) { mutableStateOf(rule?.notes ?: "") }

    // The season window is all-or-none, and unticking the box on a rule that
    // has one is what removes it. Save sends that as clearSeasonWindow
    // alongside the rest of the edits — the server rejects the flag only
    // when season BOUNDS accompany it, so nothing else in the sheet has to
    // be dropped to remove a window.
    val existingWindow = rule?.let { seasonWindowMonthDays(it) }
    var addSeasonWindow by remember(rule) { mutableStateOf(existingWindow != null) }
    var startMonth by remember(rule) { mutableStateOf(existingWindow?.first?.first ?: 4) }
    // Clamped on the way in as well as on month change: a rule stored before
    // the day picker was month-aware can hold something like Feb 30, which
    // the server now rejects on save.
    var startDay by remember(rule) {
        mutableStateOf((existingWindow?.first?.second ?: 1).coerceAtMost(daysInMonth(startMonth)))
    }
    var endMonth by remember(rule) { mutableStateOf(existingWindow?.second?.first ?: 10) }
    var endDay by remember(rule) {
        mutableStateOf((existingWindow?.second?.second ?: 1).coerceAtMost(daysInMonth(endMonth)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (rule == null) stringResource(R.string.maintenance_create_rule_title)
                else stringResource(R.string.maintenance_edit_rule_title),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FaltetChipSelector(
                    label = stringResource(R.string.maintenance_activity_label),
                    options = activityOptions,
                    selected = activityType,
                    onSelectedChange = { it?.let { activityType = it } },
                    labelFor = { activityLabels[it] ?: it },
                    required = true,
                )
                Column {
                    Text(
                        text = stringResource(R.string.maintenance_interval_label).uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                        color = FaltetForest.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(6.dp))
                    FaltetStepper(
                        value = intervalDays,
                        onDecrement = { intervalDays = (intervalDays - 1).coerceAtLeast(1) },
                        onIncrement = { intervalDays = intervalDays + 1 },
                        min = 1,
                        max = 365,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FaltetCheckbox(checked = addSeasonWindow, onCheckedChange = { addSeasonWindow = it })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.maintenance_season_window_label),
                        fontSize = 14.sp,
                        color = FaltetInk,
                    )
                }
                if (addSeasonWindow) {
                    SeasonBoundPicker(
                        label = stringResource(R.string.maintenance_season_start_label),
                        month = startMonth,
                        day = startDay,
                        onMonthChange = { startMonth = it; startDay = startDay.coerceAtMost(daysInMonth(it)) },
                        onDayChange = { startDay = it },
                    )
                    SeasonBoundPicker(
                        label = stringResource(R.string.maintenance_season_end_label),
                        month = endMonth,
                        day = endDay,
                        onMonthChange = { endMonth = it; endDay = endDay.coerceAtMost(daysInMonth(it)) },
                        onDayChange = { endDay = it },
                    )
                } else if (existingWindow != null) {
                    Text(
                        text = stringResource(R.string.maintenance_season_window_will_clear),
                        fontSize = 12.sp,
                        color = FaltetClay,
                    )
                }

                Field(
                    label = stringResource(R.string.maintenance_activity_notes_label),
                    value = notes,
                    onValueChange = { notes = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmedNotes = notes.takeIf { it.isNotBlank() }
                val sm = if (addSeasonWindow) startMonth else null
                val sd = if (addSeasonWindow) startDay else null
                val em = if (addSeasonWindow) endMonth else null
                val ed = if (addSeasonWindow) endDay else null
                if (rule == null) {
                    onCreate(activityType, intervalDays, sm, sd, em, ed, trimmedNotes)
                } else {
                    // maintenanceRuleUpdate owns the "empty a field with a
                    // flag, never a null" contract — see its doc.
                    onUpdate(
                        rule.id,
                        maintenanceRuleUpdate(
                            rule = rule,
                            activityType = activityType,
                            intervalDays = intervalDays,
                            seasonStartMonth = sm,
                            seasonStartDay = sd,
                            seasonEndMonth = em,
                            seasonEndDay = ed,
                            notes = trimmedNotes,
                        ),
                    )
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun SeasonBoundPicker(
    label: String,
    month: Int,
    day: Int,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
) {
    Column {
        Text(
            text = label.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column {
                Text(stringResource(R.string.maintenance_season_month_label), fontSize = 10.sp, color = FaltetForest)
                FaltetStepper(
                    value = month,
                    onDecrement = { onMonthChange(month - 1) },
                    onIncrement = { onMonthChange(month + 1) },
                    min = 1,
                    max = 12,
                )
            }
            Column {
                Text(stringResource(R.string.maintenance_season_day_label), fontSize = 10.sp, color = FaltetForest)
                FaltetStepper(
                    value = day,
                    onDecrement = { onDayChange(day - 1) },
                    onIncrement = { onDayChange(day + 1) },
                    min = 1,
                    max = daysInMonth(month),
                )
            }
        }
    }
}
