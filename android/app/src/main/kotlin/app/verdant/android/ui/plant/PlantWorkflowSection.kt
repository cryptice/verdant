package app.verdant.android.ui.plant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.data.model.PlantWorkflowProgressResponse
import app.verdant.android.data.model.PlantWorkflowStepResponse
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest

/** Renders the per-plant workflow section into a LazyListScope. */
fun LazyListScope.plantWorkflowSection(
    progress: PlantWorkflowProgressResponse?,
    onAdd: () -> Unit,
    onResync: () -> Unit,
    onEdit: (PlantWorkflowStepResponse) -> Unit,
    onComplete: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    if (progress == null) return
    val completedSet = progress.completedStepIds.toSet()
    val sorted = progress.steps.sortedBy { it.sortOrder }

    item(key = "wf_header") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) { FaltetSectionHeader(label = "Arbetsflöde") }
            IconButton(onClick = onResync, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Sync, "Synka från art", tint = FaltetAccent, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onAdd, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, "Lägg till steg", tint = FaltetAccent, modifier = Modifier.size(20.dp))
            }
        }
    }

    if (sorted.isEmpty()) {
        item(key = "wf_empty") {
            Text(
                "Inget arbetsflöde ännu. Tryck + för att lägga till ett steg.",
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                color = FaltetForest,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            )
        }
        return
    }

    items(sorted, key = { "wf_${it.id}" }) { step ->
        val isComplete = step.id in completedSet
        val isCurrent = progress.currentStepId == step.id
        PlantWorkflowRow(
            step = step,
            isComplete = isComplete,
            isCurrent = isCurrent,
            onComplete = { onComplete(step.id) },
            onEdit = { onEdit(step) },
            onDelete = { onDelete(step.id) },
        )
    }
}

@Composable
private fun PlantWorkflowRow(
    step: PlantWorkflowStepResponse,
    isComplete: Boolean,
    isCurrent: Boolean,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val statColor = when {
        isComplete -> FaltetForest.copy(alpha = 0.55f)
        isCurrent -> FaltetAccent
        else -> FaltetForest
    }
    val secondary = buildList {
        step.daysAfterPrevious?.let { add("+$it dag") }
        step.eventType?.let { add(it) }
        if (step.isOptional) add("valfritt")
        if (step.speciesStepId == null) add("eget steg")
        if (isComplete) add("klar")
    }.joinToString(" · ").takeIf { it.isNotBlank() }

    FaltetListRow(
        title = step.name,
        meta = secondary,
        metaMaxLines = 2,
        stat = {
            Text(
                text = if (isComplete) "✓" else if (isCurrent) "▸" else "",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = statColor,
            )
        },
        actions = {
            Row {
                if (!isComplete) {
                    IconButton(onClick = onComplete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Check, "Slutför", tint = FaltetAccent, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Redigera", tint = FaltetForest, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, "Ta bort", tint = FaltetClay, modifier = Modifier.size(18.dp))
                }
            }
        },
        onClick = if (!isComplete) onComplete else null,
    )
}

@Composable
fun PlantWorkflowStepDialog(
    initial: PlantWorkflowStepResponse?,
    onDismiss: () -> Unit,
    onSave: (name: String, eventType: String?, daysAfter: Int?, optional: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var eventType by remember { mutableStateOf(initial?.eventType ?: "") }
    var daysAfter by remember { mutableStateOf(initial?.daysAfterPrevious?.toString() ?: "") }
    var optional by remember { mutableStateOf(initial?.isOptional ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nytt steg" else "Redigera steg") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Namn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = eventType,
                    onValueChange = { eventType = it.uppercase() },
                    label = { Text("Händelsetyp (valfritt)") },
                    placeholder = { Text("SEEDED, POTTED_UP, …") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = daysAfter,
                    onValueChange = { daysAfter = it.filter { c -> c.isDigit() } },
                    label = { Text("Dagar efter föregående") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = optional, onCheckedChange = { optional = it })
                    Spacer(Modifier.size(4.dp))
                    Text("Valfritt")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        name.trim(),
                        eventType.takeIf { it.isNotBlank() },
                        daysAfter.toIntOrNull(),
                        optional,
                    )
                },
            ) { Text("Spara") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}
