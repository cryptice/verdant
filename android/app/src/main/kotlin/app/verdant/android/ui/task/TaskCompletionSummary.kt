package app.verdant.android.ui.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

@Composable
fun TaskCompletionSummary(task: ScheduledTaskResponse) {
    var descriptionExpanded by remember(task.id, task.notes) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        task.notes?.takeIf { it.isNotBlank() }?.let { description ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .semantics { stateDescription = if (descriptionExpanded) "Utökad" else "Hopfälld" }
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (descriptionExpanded) "Visa mindre" else "Visa hela beskrivningen",
                        onClick = { descriptionExpanded = !descriptionExpanded },
                    )
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    text = description,
                    fontSize = 16.sp,
                    color = FaltetInk,
                    maxLines = if (descriptionExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (descriptionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = FaltetForest,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (task.speciesName ?: task.originGroupName)?.let { TaskSummaryRow("Art", it) }
            TaskSummaryRow("Planerat antal", task.targetCount.toString())
            if (task.remainingCount != task.targetCount) TaskSummaryRow("Återstår", task.remainingCount.toString())
            TaskSummaryRow("Tidigast", task.earliestDate?.let(::formatTaskDeadline) ?: "—")
            TaskSummaryRow("Senast", task.deadline?.let(::formatTaskDeadline) ?: "—")
            taskPlaceName(task)?.let { TaskSummaryRow("Plats", it) }
        }
    }
}

@Composable
private fun TaskSummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = label, fontSize = 12.sp, color = FaltetForest, modifier = Modifier.weight(0.4f))
        Text(text = value, fontSize = 16.sp, color = FaltetInk, modifier = Modifier.weight(0.6f))
    }
}
