package app.verdant.android.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.data.model.OutletChannel
import app.verdant.android.data.model.OutletResponse

/**
 * Reusable outlet picker. Caller passes the outlet list and gets selection
 * events back. Optional [onCreateOutlet] enables the inline "+ Lägg till
 * utlopp" affordance — when wired, the picker hosts a small create dialog and
 * forwards the new outlet's name + channel to the caller, which is responsible
 * for the API call (and for re-fetching outlets so the new entry shows up).
 *
 * Phase 11+ screens drop this in next to the price/qty fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletPicker(
    outlets: List<OutletResponse>,
    selectedId: Long?,
    onSelected: (Long) -> Unit,
    label: String = "Utlopp",
    onCreateOutlet: ((name: String, channel: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val selected = outlets.firstOrNull { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.name} · ${channelLabelSv(it.channel)}" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Välj utlopp") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable, true),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            outlets.forEach { o ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(o.name, fontSize = 14.sp)
                            Text(channelLabelSv(o.channel), fontSize = 11.sp)
                        }
                    },
                    onClick = {
                        onSelected(o.id)
                        expanded = false
                    },
                )
            }
            if (onCreateOutlet != null) {
                if (outlets.isNotEmpty()) HorizontalDivider()
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Lägg till utlopp") },
                    onClick = {
                        expanded = false
                        showCreateDialog = true
                    },
                )
            }
        }
    }

    if (showCreateDialog && onCreateOutlet != null) {
        OutletCreateDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, channel ->
                onCreateOutlet(name, channel)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun OutletCreateDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, channel: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf(OutletChannel.FLORIST) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nytt utlopp") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Namn") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Kanal", fontSize = 13.sp)
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutletChannel.values.forEach { c ->
                        FilterChip(
                            selected = c == channel,
                            onClick = { channel = c },
                            label = { Text(channelLabelSv(c), fontSize = 12.sp) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onCreate(name.trim(), channel) },
            ) { Text("Skapa") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

internal fun channelLabelSv(channel: String): String = when (channel) {
    OutletChannel.FLORIST -> "Blomsterhandel"
    OutletChannel.FARMERS_MARKET -> "Bondemarknad"
    OutletChannel.CSA -> "Andelsodling"
    OutletChannel.WEDDING -> "Bröllop"
    OutletChannel.WHOLESALE -> "Grossist"
    OutletChannel.DIRECT -> "Direktförsäljning"
    OutletChannel.OTHER -> "Övrigt"
    else -> channel
}
