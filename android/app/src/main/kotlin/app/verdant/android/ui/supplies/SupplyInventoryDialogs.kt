package app.verdant.android.ui.supplies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.data.model.SupplyTypeResponse
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetForest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddSupplyDialog(
    types: List<SupplyTypeResponse>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (typeId: Long, quantity: Double, costCents: Int?, notes: String?) -> Unit,
    onAddType: (initialCategory: String) -> Unit,
) {
    // Two-stage selection: pick the category first, then the type within
    // it. Categories that have no types yet show a hint instead of an
    // empty type dropdown.
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<SupplyTypeResponse?>(null) }
    var quantity by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val typesInCategory = remember(types, selectedCategory) {
        selectedCategory?.let { cat -> types.filter { it.category == cat } } ?: emptyList()
    }

    val qty = quantity.toDoubleOrNull()
    val canSubmit = selectedType != null && qty != null && qty > 0 && !saving

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lägg till material") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FaltetDropdown(
                    label = "Kategori",
                    options = SUPPLY_CATEGORIES,
                    selected = selectedCategory,
                    onSelectedChange = {
                        selectedCategory = it
                        // Picking a different category invalidates the
                        // type, so reset it.
                        selectedType = null
                    },
                    labelFor = { categoryLabelSv(it) },
                    searchable = false,
                    required = true,
                )

                if (selectedCategory != null && typesInCategory.isEmpty()) {
                    // Hint: this category has no types yet — nudge the
                    // user toward creating one in the right category.
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Inga typer i ${categoryLabelSv(selectedCategory!!)} ännu.",
                            fontSize = 12.sp,
                            color = FaltetForest,
                        )
                        TextButton(onClick = { onAddType(selectedCategory!!) }) {
                            Text("+ Skapa ${categoryLabelSv(selectedCategory!!).lowercase()}-typ", color = FaltetAccent, fontSize = 12.sp)
                        }
                    }
                } else {
                    FaltetDropdown(
                        label = "Typ",
                        options = typesInCategory,
                        selected = selectedType,
                        onSelectedChange = { selectedType = it },
                        labelFor = { it.name },
                        searchable = true,
                        required = true,
                    )
                    TextButton(onClick = { onAddType(selectedCategory ?: "FERTILIZER") }) {
                        Text("+ Ny typ", color = FaltetAccent, fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Mängd${selectedType?.unit?.let { " (${unitLabelSv(it)})" } ?: ""}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Kostnad (kr, valfri)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Anteckning (valfri)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                onClick = {
                    val type = selectedType!!
                    val q = quantity.toDouble()
                    val costCents = cost.toDoubleOrNull()?.let { (it * 100).toInt() }
                    onSubmit(type.id, q, costCents, notes.trim().ifBlank { null })
                },
            ) { Text(if (saving) "Sparar…" else "Spara", color = FaltetAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt", color = FaltetForest) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddSupplyTypeDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, category: String, unit: String, inexhaustible: Boolean) -> Unit,
    initialCategory: String = "FERTILIZER",
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(initialCategory) }
    var unit by remember { mutableStateOf<String?>("LITERS") }
    var inexhaustible by remember { mutableStateOf(false) }

    val canSubmit = name.trim().isNotBlank() && category != null && unit != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ny materialtyp") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Namn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FaltetDropdown(
                    label = "Kategori",
                    options = SUPPLY_CATEGORIES,
                    selected = category,
                    onSelectedChange = { category = it },
                    labelFor = { categoryLabelSv(it) },
                    searchable = false,
                    required = true,
                )
                FaltetDropdown(
                    label = "Enhet",
                    options = SUPPLY_UNITS,
                    selected = unit,
                    onSelectedChange = { unit = it },
                    labelFor = { unitLabelSv(it) },
                    searchable = false,
                    required = true,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Switch(
                        checked = inexhaustible,
                        onCheckedChange = { inexhaustible = it },
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Obegränsad", fontSize = 14.sp)
                        Text(
                            "Behöver inte spåras (t.ex. egen hästgödsel).",
                            fontSize = 11.sp,
                            color = FaltetForest,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                onClick = { onSubmit(name.trim(), category!!, unit!!, inexhaustible) },
            ) { Text("Spara", color = FaltetAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt", color = FaltetForest) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditSupplyTypeDialog(
    type: SupplyTypeResponse,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, unit: String, inexhaustible: Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(type.id) { mutableStateOf(type.name) }
    var category by remember(type.id) { mutableStateOf<String?>(type.category) }
    var unit by remember(type.id) { mutableStateOf<String?>(type.unit) }
    var inexhaustible by remember(type.id) { mutableStateOf(type.inexhaustible) }
    var confirmDelete by remember { mutableStateOf(false) }

    val canSave = name.trim().isNotBlank() && category != null && unit != null && (
        name.trim() != type.name || category != type.category ||
            unit != type.unit || inexhaustible != type.inexhaustible
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redigera typ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Namn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FaltetDropdown(
                    label = "Kategori",
                    options = SUPPLY_CATEGORIES,
                    selected = category,
                    onSelectedChange = { category = it },
                    labelFor = { categoryLabelSv(it) },
                    searchable = false,
                    required = true,
                )
                FaltetDropdown(
                    label = "Enhet",
                    options = SUPPLY_UNITS,
                    selected = unit,
                    onSelectedChange = { unit = it },
                    labelFor = { unitLabelSv(it) },
                    searchable = false,
                    required = true,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Switch(
                        checked = inexhaustible,
                        onCheckedChange = { inexhaustible = it },
                    )
                    Spacer(Modifier.size(12.dp))
                    Text("Obegränsad", fontSize = 14.sp, modifier = Modifier.weight(1f))
                }
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Ta bort typ", color = FaltetAccent)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onSave(name.trim(), category!!, unit!!, inexhaustible) },
            ) { Text("Spara", color = FaltetAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt", color = FaltetForest) }
        },
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Ta bort typ?") },
            text = { Text("${type.name} tas bort. Befintliga registreringar finns kvar.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Ta bort", color = FaltetAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Avbryt", color = FaltetForest) }
            },
        )
    }
}
