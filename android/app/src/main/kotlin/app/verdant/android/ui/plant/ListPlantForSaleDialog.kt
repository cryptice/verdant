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
import app.verdant.android.data.model.PlantResponse
import app.verdant.android.data.model.UnitKind
import app.verdant.android.ui.sales.OutletPicker
import app.verdant.android.ui.theme.FaltetForest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ListPlantForSaleDialog(
    plant: PlantResponse,
    available: Int,
    outlets: List<OutletResponse>,
    onDismiss: () -> Unit,
    onCreate: (unitKind: String, quantity: Int, priceCents: Int, outletId: Long) -> Unit,
    onCreateOutlet: (name: String, channel: String, onCreated: (Long) -> Unit) -> Unit,
) {
    var quantity by remember { mutableStateOf("") }
    var unitKind by remember { mutableStateOf(UnitKind.PLUG) }
    var priceText by remember { mutableStateOf("") }
    var outletId by remember { mutableStateOf<Long?>(null) }

    val plantUnitKinds = listOf(UnitKind.PLUG, UnitKind.BULB, UnitKind.TUBER, UnitKind.PLANT)

    val qtyInt = quantity.toIntOrNull()
    val priceCents = priceText.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toInt() }
    val valid = qtyInt != null && qtyInt in 1..available &&
        priceCents != null && priceCents > 0 &&
        outletId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lägg ut till försäljning") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${plant.name} — tillgängligt: $available",
                    fontSize = 13.sp,
                    color = FaltetForest,
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                    label = { Text("Antal") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = quantity.isNotBlank() && (qtyInt == null || qtyInt < 1 || qtyInt > available),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Enhet", fontSize = 13.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    plantUnitKinds.forEach { u ->
                        FilterChip(
                            selected = u == unitKind,
                            onClick = { unitKind = u },
                            label = { Text(unitLabelSv(u), fontSize = 12.sp) },
                        )
                    }
                }

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
                    label = { Text("Begärt pris per enhet (SEK)") },
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
                onClick = { onCreate(unitKind, qtyInt!!, priceCents!!, outletId!!) },
            ) { Text("Lägg ut") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

internal fun unitLabelSv(unit: String): String = when (unit) {
    UnitKind.STEM -> "Stjälk"
    UnitKind.BUNCH -> "Bunt"
    UnitKind.PLUG -> "Plugg"
    UnitKind.BULB -> "Lök"
    UnitKind.TUBER -> "Knöl"
    UnitKind.PLANT -> "Planta"
    UnitKind.BOUQUET -> "Bukett"
    else -> unit
}
