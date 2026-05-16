package app.verdant.android.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import app.verdant.android.data.model.CustomerResponse
import app.verdant.android.data.model.OutletResponse
import app.verdant.android.data.model.QuickSaleRequest
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.ui.faltet.FaltetChipSelector
import app.verdant.android.ui.faltet.FaltetDatePicker
import app.verdant.android.ui.faltet.FaltetDropdown
// OutletPicker is in the same package — no import needed
import java.time.LocalDate

private val QUICK_UNIT_KINDS = listOf("STEM", "PLUG", "BULB", "TUBER", "PLANT")

private fun unitLabel(kind: String): String = when (kind) {
    "STEM" -> "Stjälk"
    "PLUG" -> "Plugg"
    "BULB" -> "Lök"
    "TUBER" -> "Knöl"
    "PLANT" -> "Planta"
    else -> kind
}

@Composable
fun QuickSaleDialog(
    species: List<SpeciesResponse>,
    outlets: List<OutletResponse>,
    customers: List<CustomerResponse>,
    onDismiss: () -> Unit,
    onConfirm: (QuickSaleRequest) -> Unit,
    onCreateOutlet: ((name: String, channel: String) -> Unit)? = null,
) {
    var selectedSpecies by remember { mutableStateOf<SpeciesResponse?>(null) }
    var unitKind by remember { mutableStateOf<String?>(null) }
    var qtyText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var selectedOutletId by remember { mutableStateOf<Long?>(null) }
    var selectedCustomer by remember { mutableStateOf<CustomerResponse?>(null) }
    var soldAt by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var notes by remember { mutableStateOf("") }

    val qty = qtyText.toIntOrNull()
    val priceCents = priceText.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toInt() }
    val valid = selectedSpecies != null &&
        unitKind != null &&
        qty != null && qty >= 1 &&
        priceCents != null && priceCents >= 0 &&
        selectedOutletId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Snabbförsäljning") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                FaltetDropdown(
                    label = "Art",
                    options = species,
                    selected = selectedSpecies,
                    onSelectedChange = { selectedSpecies = it },
                    labelFor = { speciesDisplayName(it) },
                    searchable = true,
                    required = true,
                )
                FaltetChipSelector(
                    label = "Enhet",
                    options = QUICK_UNIT_KINDS,
                    selected = unitKind,
                    onSelectedChange = { unitKind = it },
                    labelFor = { unitLabel(it) },
                    required = true,
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                    label = { Text("Antal") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Pris per enhet (SEK)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutletPicker(
                    outlets = outlets,
                    selectedId = selectedOutletId,
                    onSelected = { selectedOutletId = it },
                    label = "Försäljningskanal",
                    onCreateOutlet = onCreateOutlet,
                )
                FaltetDatePicker(
                    label = "Säljdatum",
                    value = soldAt,
                    onValueChange = { soldAt = it ?: soldAt },
                )
                FaltetDropdown(
                    label = "Kund (valfri)",
                    options = customers,
                    selected = selectedCustomer,
                    onSelectedChange = { selectedCustomer = it },
                    labelFor = { it.name },
                    searchable = true,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Anteckning (valfri)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onConfirm(
                        QuickSaleRequest(
                            speciesId = selectedSpecies!!.id,
                            unitKind = unitKind!!,
                            quantity = qty!!,
                            pricePerUnitCents = priceCents!!,
                            outletId = selectedOutletId!!,
                            customerId = selectedCustomer?.id,
                            soldAt = soldAt?.toString(),
                            notes = notes.ifBlank { null },
                        )
                    )
                },
            ) { Text("Spara") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

private fun speciesDisplayName(s: SpeciesResponse): String {
    val name = s.commonNameSv ?: s.commonName
    val variant = s.variantNameSv ?: s.variantName
    return if (variant.isNullOrBlank()) name else "$name – $variant"
}
