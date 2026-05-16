package app.verdant.android.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import app.verdant.android.data.model.EditSaleRequest
import app.verdant.android.data.model.SaleLedgerEntry
import app.verdant.android.ui.faltet.FaltetDatePicker
import app.verdant.android.ui.faltet.FaltetDropdown
import java.time.LocalDate

@Composable
fun EditSaleDialog(
    entry: SaleLedgerEntry,
    customers: List<CustomerResponse>,
    onDismiss: () -> Unit,
    onConfirm: (EditSaleRequest) -> Unit,
) {
    var qtyText by remember(entry.id) { mutableStateOf(entry.quantity.toString()) }
    var priceText by remember(entry.id) { mutableStateOf("%.2f".format(entry.pricePerUnitCents / 100.0)) }
    var notes by remember(entry.id) { mutableStateOf("") }
    var soldAt by remember(entry.id) {
        mutableStateOf(runCatching { LocalDate.parse(entry.soldAt.take(10)) }.getOrNull() ?: LocalDate.now())
    }

    // Best-effort pre-select by display name (ledger DTO carries customerName but not customerId).
    var selectedCustomer by remember(entry.id, customers) {
        mutableStateOf<CustomerResponse?>(customers.firstOrNull { it.name == entry.customerName })
    }

    val qty = qtyText.toIntOrNull()
    val priceCents = priceText.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toInt() }
    val valid = qty != null && qty >= 1 && priceCents != null && priceCents >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redigera försäljning") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                FaltetDatePicker(
                    label = "Säljdatum",
                    value = soldAt,
                    onValueChange = { soldAt = it ?: soldAt },
                    required = true,
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
                        EditSaleRequest(
                            quantity = qty,
                            pricePerUnitCents = priceCents,
                            customerId = selectedCustomer?.id,
                            soldAt = soldAt.toString(),
                            notes = notes.ifBlank { null },
                        )
                    )
                },
            ) { Text("Spara") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}
