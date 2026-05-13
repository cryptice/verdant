package app.verdant.android.ui.task

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetBerry
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetMustard
import app.verdant.android.ui.theme.FaltetSage
import app.verdant.android.ui.theme.FaltetSky
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Time the checked-but-not-yet-sent state is shown before the API call fires
 *  and the row animates out. Tuned so the user gets a beat of visual feedback
 *  but doesn't have to wait long. */
const val COMPLETE_TASK_DELAY_MS = 1500L

private val svDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("sv"))

fun formatTaskDeadline(deadline: String): String = runCatching {
    LocalDate.parse(deadline).format(svDateFormatter)
}.getOrElse { deadline }

fun taskActivityLabelSv(activityType: String): String = when (activityType) {
    "SOW" -> "Så"
    "POT_UP" -> "Skola om"
    "PLANT" -> "Plantera"
    "HARVEST" -> "Skörda"
    "RECOVER" -> "Återhämta"
    "DISCARD" -> "Kassera"
    "WATER" -> "Vattna"
    "WEED" -> "Rensa ogräs"
    "FERTILIZE" -> "Gödsla"
    else -> activityType
}

fun taskDotColor(activityType: String): Color = when (activityType) {
    "SOW" -> FaltetMustard
    "PLANT" -> FaltetSage
    "HARVEST" -> FaltetAccent
    "FERTILIZE" -> FaltetBerry
    else -> FaltetSky
}

/** Human-readable description of what the task targets — bed name or species. */
fun taskTargetLabel(task: ScheduledTaskResponse): String? = when {
    task.bedId != null -> task.bedName
    else -> task.originGroupName ?: task.speciesName
}

@Composable
fun TaskRow(
    task: ScheduledTaskResponse,
    isCompleting: Boolean,
    onClick: () -> Unit,
    onCompleteToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBedTask = task.bedId != null
    val dotColor = taskDotColor(task.activityType)
    val title = when {
        isBedTask -> taskActivityLabelSv(task.activityType)
        else -> task.originGroupName ?: task.speciesName ?: "Uppgift"
    }
    val meta = buildString {
        append(formatTaskDeadline(task.deadline))
        if (isBedTask) {
            task.bedName?.takeIf { it.isNotBlank() }?.let {
                append(" · ")
                append(it)
            }
        } else {
            val species = task.speciesName
            if (species != null && species != title) {
                append(" · ")
                append(species)
            }
        }
    }
    FaltetListRow(
        modifier = modifier,
        title = title,
        meta = meta,
        leading = {
            if (isBedTask) {
                Checkbox(
                    checked = isCompleting,
                    onCheckedChange = { if (!isCompleting) onCompleteToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = FaltetAccent,
                        uncheckedColor = FaltetForest,
                        checkmarkColor = FaltetInk,
                    ),
                )
            } else {
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .size(10.dp)
                        .drawBehind { drawCircle(dotColor) }
                )
            }
        },
        stat = if (!isBedTask) {
            {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = task.remainingCount.toString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        color = FaltetInk,
                    )
                    Text(
                        text = " ST",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                        color = FaltetForest,
                    )
                }
            }
        } else null,
        actions = {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Ta bort",
                    tint = FaltetAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
fun TaskDeleteDialog(
    task: ScheduledTaskResponse,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when {
        task.bedId != null -> taskActivityLabelSv(task.activityType)
        else -> task.originGroupName ?: task.speciesName ?: "Uppgift"
    }
    val target = taskTargetLabel(task)
    val label = if (target != null && target != title) "$title · $target" else title
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ta bort uppgift") },
        text = { Text("Vill du ta bort \"$label\"?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Ta bort", color = FaltetClay) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        },
    )
}
