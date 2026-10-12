package app.verdant.android.ui.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.data.model.ScheduledTaskResponse

@Composable
fun TaskCompletionSummary(task: ScheduledTaskResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Uppgift att slutföra", fontSize = 12.sp)
        Text(task.notes?.takeIf { it.isNotBlank() } ?: task.speciesName ?: task.originGroupName ?: "Uppgift", fontSize = 16.sp)
        (task.speciesName ?: task.originGroupName)?.let { Text("Art: $it") }
        Text("Planerat antal: ${task.targetCount} · Återstår: ${task.remainingCount}")
        task.earliestDate?.let { Text("Tidigast: ${formatTaskDeadline(it)}") }
        task.deadline?.let { Text("Senast: ${formatTaskDeadline(it)}") }
        taskPlaceName(task)?.let { Text("Plats: $it") }
    }
}
