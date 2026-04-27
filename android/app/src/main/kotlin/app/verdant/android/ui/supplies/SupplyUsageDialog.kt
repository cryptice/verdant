package app.verdant.android.ui.supplies

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.R
import app.verdant.android.data.model.SupplyInventoryResponse
import app.verdant.android.data.repository.GardenRepository
import kotlinx.coroutines.launch

private const val TAG = "SupplyUsageDialog"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplyUsageBottomSheet(
    repo: GardenRepository,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var supplies by remember { mutableStateOf<List<SupplyInventoryResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedSupply by remember { mutableStateOf<SupplyInventoryResponse?>(null) }
    var amount by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var supplyExpanded by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            supplies = repo.getSupplyInventory().filter { it.quantity > 0 }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load supplies", e)
        }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.record_supply_usage),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )

            if (isLoading) {
                CircularProgressIndicator(Modifier.padding(16.dp))
            } else if (supplies.isEmpty()) {
                Text(
                    stringResource(R.string.no_supplies_available),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            } else if (submitted) {
                Text(
                    stringResource(R.string.supply_usage_recorded),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                // Allow recording another
                TextButton(onClick = {
                    submitted = false
                    selectedSupply = null
                    amount = ""
                }) {
                    Text(stringResource(R.string.record_another))
                }
            } else {
                // Supply picker
                ExposedDropdownMenuBox(
                    expanded = supplyExpanded,
                    onExpandedChange = { supplyExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedSupply?.let {
                            "${it.supplyTypeName} (${formatQuantity(it.quantity, it.unit)})"
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text(stringResource(R.string.select_supply)) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable, true),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(supplyExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = supplyExpanded,
                        onDismissRequest = { supplyExpanded = false },
                    ) {
                        supplies.forEach { supply ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(supply.supplyTypeName, fontSize = 14.sp)
                                        Text(
                                            formatQuantity(supply.quantity, supply.unit),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        )
                                    }
                                },
                                onClick = {
                                    selectedSupply = supply
                                    amount = formatQuantity(supply.quantity, "").trim()
                                    supplyExpanded = false
                                },
                            )
                        }
                    }
                }

                // Amount field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.amount_to_use)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    suffix = selectedSupply?.let { { Text(it.unit) } },
                )

                val qty = amount.toDoubleOrNull()
                Button(
                    onClick = {
                        if (selectedSupply != null && qty != null && qty > 0) {
                            isSubmitting = true
                            scope.launch {
                                try {
                                    repo.decrementSupply(selectedSupply!!.id, qty)
                                    submitted = true
                                    // Refresh supplies for potential next recording
                                    supplies = repo.getSupplyInventory().filter { it.quantity > 0 }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to decrement supply", e)
                                }
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedSupply != null && qty != null && qty > 0
                            && qty <= (selectedSupply?.quantity ?: 0.0) && !isSubmitting,
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(R.string.record_usage))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
