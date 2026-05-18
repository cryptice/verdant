package app.verdant.android.ui.plant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.data.model.OutletResponse
import app.verdant.android.data.model.PlantEventResponse
import app.verdant.android.data.model.UnitKind
import app.verdant.android.ui.sales.OutletPicker
import app.verdant.android.ui.theme.FaltetForest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ListHarvestForSaleDialog(
    event: PlantEventResponse,
    available: Int,
    outlets: List<OutletResponse>,
    onDismiss: () -> Unit,
    onCreate: (unitKind: String, stemsPerUnit: Int?, quantity: Int, priceCents: Int, outletId: Long) -> Unit,
    onCreateOutlet: (name: String, channel: String, onCreated: (Long) -> Unit) -> Unit,
) {
    var unitKind by remember { mutableStateOf(UnitKind.STEM) }
    var quantity by remember { mutableStateOf("") }
    var stemsPerBunch by remember { mutableStateOf("10") }
    var priceText by remember { mutableStateOf("") }
    var outletId by remember { mutableStateOf<Long?>(null) }

    val isBunch = unitKind == UnitKind.BUNCH
    val qtyInt = quantity.toIntOrNull()
    val stemsPerUnit = if (isBunch) stemsPerBunch.toIntOrNull() else null
    val priceCents = priceText.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toInt() }
    val multiplier = if (isBunch) (stemsPerUnit ?: 0) else 1
    val totalStems = (qtyInt ?: 0) * multiplier
    val valid = qtyInt != null && qtyInt > 0 &&
        (!isBunch || (stemsPerUnit != null && stemsPerUnit > 0)) &&
        totalStems in 1..available &&
        priceCents != null && priceCents > 0 &&
        outletId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lägg ut skörd") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Skörd ${event.eventDate.take(10)} — tillgängliga stjälkar: $available",
                    fontSize = 13.sp,
                    color = FaltetForest,
                )

                Text("Enhet", fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(UnitKind.STEM, UnitKind.BUNCH).forEach { u ->
                        FilterChip(
                            selected = u == unitKind,
                            onClick = { unitKind = u },
                            label = { Text(unitLabelSv(u), fontSize = 12.sp) },
                        )
                    }
                }

                if (isBunch) {
                    OutlinedTextField(
                        value = stemsPerBunch,
                        onValueChange = { stemsPerBunch = it.filter { c -> c.isDigit() } },
                        label = { Text("Stjälkar per bunt") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                    label = { Text(if (isBunch) "Antal buntar" else "Antal stjälkar") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = quantity.isNotBlank() && (qtyInt == null || qtyInt < 1 || totalStems > available),
                    supportingText = {
                        if (isBunch && qtyInt != null && stemsPerUnit != null) {
                            Text("$totalStems stjälkar totalt", fontSize = 11.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutletPicker(
                    outlets = outlets,
                    selectedId = outletId,
                    onSelected = { outletId = it },
                    onCreateOutlet = { name, channel ->
                        onCreateOutlet(name, channel) { newId -> outletId = newId }
                    },
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text(if (isBunch) "Begärt pris per bunt (SEK)" else "Begärt pris per stjälk (SEK)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onCreate(unitKind, stemsPerUnit, qtyInt!!, priceCents!!, outletId!!) },
            ) { Text("Lägg ut") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}
