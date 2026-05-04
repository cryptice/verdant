package app.verdant.android.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateOutletRequest
import app.verdant.android.data.model.OutletResponse
import app.verdant.android.data.model.RecordSaleRequest
import app.verdant.android.data.model.SaleLotDetailResponse
import app.verdant.android.data.model.SaleLotEventType
import app.verdant.android.data.model.SaleLotStatus
import app.verdant.android.data.repository.OutletRepository
import app.verdant.android.data.repository.SaleLotRepository
import app.verdant.android.data.repository.SaleRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.BotanicalPlate
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.plant.unitLabelSv
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaleLotDetailState(
    val isLoading: Boolean = true,
    val detail: SaleLotDetailResponse? = null,
    val outlets: List<OutletResponse> = emptyList(),
    val error: String? = null,
    val toastMessage: String? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class SaleLotDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val saleLotRepository: SaleLotRepository,
    private val saleRepository: SaleRepository,
    private val outletRepository: OutletRepository,
) : ViewModel() {
    private val lotId: Long = savedStateHandle.get<Long>("lotId")!!
    private val _uiState = MutableStateFlow(SaleLotDetailState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val detail = saleLotRepository.detail(lotId)
                val outlets = runCatching { outletRepository.list() }.getOrDefault(emptyList())
                _uiState.value = SaleLotDetailState(isLoading = false, detail = detail, outlets = outlets)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun recordSale(quantity: Int, pricePerUnitCents: Int, customerId: Long?, notes: String?) {
        viewModelScope.launch {
            try {
                saleRepository.record(
                    lotId,
                    RecordSaleRequest(
                        quantity = quantity,
                        pricePerUnitCents = pricePerUnitCents,
                        customerId = customerId,
                        notes = notes,
                    ),
                )
                toast("Sålde $quantity")
                refresh()
            } catch (e: Exception) { toast(e.message ?: "Kunde inte registrera försäljning") }
        }
    }

    fun changePrice(newPriceCents: Int) {
        viewModelScope.launch {
            try {
                saleLotRepository.changePrice(lotId, newPriceCents)
                toast("Pris sänkt")
                refresh()
            } catch (e: Exception) { toast(e.message ?: "Kunde inte sänka pris") }
        }
    }

    fun changeOutlet(newOutletId: Long) {
        viewModelScope.launch {
            try {
                saleLotRepository.changeOutlet(lotId, newOutletId)
                toast("Utlopp ändrat")
                refresh()
            } catch (e: Exception) { toast(e.message ?: "Kunde inte ändra utlopp") }
        }
    }

    fun markReturned(fromOutletId: Long) {
        viewModelScope.launch {
            try {
                saleLotRepository.markReturnedFromOutlet(lotId, fromOutletId)
                toast("Markerade som returnerad")
                refresh()
            } catch (e: Exception) { toast(e.message ?: "Kunde inte markera returnerad") }
        }
    }

    fun markNotSold() {
        viewModelScope.launch {
            try {
                saleLotRepository.markNotSold(lotId)
                toast("Markerade som ej såld")
                refresh()
            } catch (e: Exception) { toast(e.message ?: "Kunde inte markera ej såld") }
        }
    }

    fun deleteLot() {
        viewModelScope.launch {
            try {
                saleLotRepository.delete(lotId)
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: Exception) { toast(e.message ?: "Kunde inte ta bort") }
        }
    }

    fun createOutlet(name: String, channel: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val created = outletRepository.create(CreateOutletRequest(name = name, channel = channel))
                _uiState.value = _uiState.value.copy(outlets = _uiState.value.outlets + created)
                onCreated(created.id)
            } catch (e: Exception) { toast(e.message ?: "Kunde inte skapa utlopp") }
        }
    }

    fun consumeToast() { _uiState.value = _uiState.value.copy(toastMessage = null) }

    private fun toast(msg: String) {
        _uiState.value = _uiState.value.copy(toastMessage = msg)
    }
}

@Composable
fun SaleLotDetailScreen(
    onBack: () -> Unit,
    viewModel: SaleLotDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var sellDialog by remember { mutableStateOf(false) }
    var priceDialog by remember { mutableStateOf(false) }
    var outletDialog by remember { mutableStateOf(false) }
    var returnDialog by remember { mutableStateOf(false) }
    var notSoldConfirm by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeToast()
        }
    }
    LaunchedEffect(uiState.deleted) { if (uiState.deleted) onBack() }

    val detail = uiState.detail
    val lot = detail?.lot
    val isOffered = lot?.status == SaleLotStatus.OFFERED
    val noSales = (detail?.sales ?: emptyList()).isEmpty()

    if (sellDialog && lot != null) {
        RecordSaleDialog(
            remaining = lot.quantityRemaining,
            currentPriceCents = lot.currentRequestedPriceCents,
            onDismiss = { sellDialog = false },
            onConfirm = { qty, priceCents, notes ->
                viewModel.recordSale(qty, priceCents, customerId = null, notes = notes)
                sellDialog = false
            },
        )
    }
    if (priceDialog && lot != null) {
        ChangePriceDialog(
            currentCents = lot.currentRequestedPriceCents,
            onDismiss = { priceDialog = false },
            onConfirm = { newCents ->
                viewModel.changePrice(newCents)
                priceDialog = false
            },
        )
    }
    if (outletDialog && lot != null) {
        ChangeOutletDialog(
            currentOutletId = lot.currentOutletId,
            outlets = uiState.outlets,
            onDismiss = { outletDialog = false },
            onConfirm = { newOutletId ->
                viewModel.changeOutlet(newOutletId)
                outletDialog = false
            },
            onCreateOutlet = { name, channel, onCreated ->
                viewModel.createOutlet(name, channel, onCreated)
            },
        )
    }
    if (returnDialog && lot != null) {
        AlertDialog(
            onDismissRequest = { returnDialog = false },
            title = { Text("Markera returnerad") },
            text = { Text("Markera ${lot.quantityRemaining} kvar som returnerade från ${lot.currentOutletName}? Detta är endast en logghändelse — antalet ändras inte.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markReturned(lot.currentOutletId)
                    returnDialog = false
                }) { Text("Markera") }
            },
            dismissButton = { TextButton(onClick = { returnDialog = false }) { Text("Avbryt") } },
        )
    }
    if (notSoldConfirm) {
        AlertDialog(
            onDismissRequest = { notSoldConfirm = false },
            title = { Text("Markera ej såld") },
            text = { Text("Detta avslutar partiet — det går inte att registrera fler försäljningar. Bekräfta?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markNotSold()
                    notSoldConfirm = false
                }) { Text("Markera") }
            },
            dismissButton = { TextButton(onClick = { notSoldConfirm = false }) { Text("Avbryt") } },
        )
    }
    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text("Ta bort parti") },
            text = { Text("Endast aktiva partier utan registrerade försäljningar kan tas bort. Bekräfta?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteLot(); deleteConfirm = false }) { Text("Ta bort") }
            },
            dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("Avbryt") } },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "",
        mastheadCenter = "Försäljning",
        snackbarHost = { SnackbarHost(snackbarHostState) },
        watermark = BotanicalPlate.Harvest,
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null && detail == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { ConnectionErrorState(onRetry = { viewModel.refresh() }) }
            detail == null -> FaltetEmptyState(
                headline = "Partiet hittades inte",
                subtitle = "Det kan ha tagits bort.",
                modifier = Modifier.padding(padding),
            )
            else -> {
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    // Header
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = lot!!.sourceSummary ?: sourceKindLabelSv(lot.sourceKind),
                                fontFamily = FaltetDisplay,
                                fontStyle = FontStyle.Italic,
                                fontSize = 22.sp,
                                color = FaltetInk,
                            )
                            Text(
                                text = "${statusLabelSv(lot.status)} · ${lot.quantityRemaining}/${lot.quantityTotal} ${unitLabelSv(lot.unitKind).lowercase()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp,
                                color = FaltetForest,
                            )
                            Text(
                                text = "${lot.currentOutletName} · ${"%.2f".format(lot.currentRequestedPriceCents / 100.0)} kr/${unitLabelSv(lot.unitKind).lowercase()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp,
                                color = FaltetForest,
                            )
                            if (lot.currentRequestedPriceCents != lot.initialRequestedPriceCents) {
                                Text(
                                    text = "Ursprungligt pris: ${"%.2f".format(lot.initialRequestedPriceCents / 100.0)} kr",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = FaltetForest,
                                )
                            }
                        }
                    }
                    // Actions
                    if (isOffered) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(onClick = { sellDialog = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Sälj N st") }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = { priceDialog = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Sänk pris") }
                                    OutlinedButton(onClick = { outletDialog = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Byt utlopp") }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = { returnDialog = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Returnerad") }
                                    OutlinedButton(onClick = { notSoldConfirm = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Ej såld") }
                                }
                                if (noSales) {
                                    TextButton(onClick = { deleteConfirm = true }, modifier = Modifier.fillMaxWidth()) { Text("Ta bort parti") }
                                }
                            }
                        }
                    }

                    // Sales section
                    item { Spacer(Modifier.height(8.dp)) }
                    item { FaltetSectionHeader(label = "Försäljningar") }
                    val sales = detail.sales
                    if (sales.isEmpty()) {
                        item {
                            Text(
                                text = "Inga försäljningar registrerade.",
                                fontFamily = FaltetDisplay,
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.sp,
                                color = FaltetForest,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            )
                        }
                    } else {
                        items(sales, key = { "sale_${it.id}" }) { sale ->
                            FaltetListRow(
                                title = "${sale.quantity} ${unitLabelSv(lot!!.unitKind).lowercase()} @ ${"%.2f".format(sale.pricePerUnitCents / 100.0)} kr",
                                meta = buildString {
                                    append(sale.outletName)
                                    sale.customerName?.let { append(" · $it") }
                                    sale.notes?.takeIf { it.isNotBlank() }?.let { append(" · “$it”") }
                                },
                                stat = {
                                    Text(
                                        text = sale.soldAt.take(10),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.2.sp,
                                        color = FaltetForest,
                                    )
                                },
                                metaMaxLines = 2,
                            )
                        }
                    }

                    // Audit log
                    item { FaltetSectionHeader(label = "Historik") }
                    val events = detail.events
                    if (events.isEmpty()) {
                        item {
                            Text(
                                text = "Ingen historik.",
                                fontFamily = FaltetDisplay,
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.sp,
                                color = FaltetForest,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            )
                        }
                    } else {
                        items(events, key = { "ev_${it.id}" }) { ev ->
                            FaltetListRow(
                                title = eventTypeLabelSv(ev.eventType),
                                meta = ev.createdAt.take(10),
                            )
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

private fun eventTypeLabelSv(type: String): String = when (type) {
    SaleLotEventType.CREATED -> "Skapat"
    SaleLotEventType.PRICE_CHANGED -> "Pris ändrat"
    SaleLotEventType.OUTLET_CHANGED -> "Utlopp ändrat"
    SaleLotEventType.RETURNED_FROM_OUTLET -> "Returnerad"
    SaleLotEventType.SALE_RECORDED -> "Försäljning registrerad"
    SaleLotEventType.SALE_EDITED -> "Försäljning redigerad"
    SaleLotEventType.MARKED_NOT_SOLD -> "Markerad ej såld"
    SaleLotEventType.AUTO_SOLD_OUT -> "Slutsåld"
    else -> type
}

@Composable
private fun RecordSaleDialog(
    remaining: Int,
    currentPriceCents: Int,
    onDismiss: () -> Unit,
    onConfirm: (qty: Int, priceCents: Int, notes: String?) -> Unit,
) {
    var qtyText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("%.2f".format(currentPriceCents / 100.0)) }
    var notes by remember { mutableStateOf("") }
    val qty = qtyText.toIntOrNull()
    val priceCents = priceText.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toInt() }
    val valid = qty != null && qty in 1..remaining && priceCents != null && priceCents >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrera försäljning") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Kvar att sälja: $remaining", fontSize = 12.sp, color = FaltetForest)
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                    label = { Text("Antal") },
                    singleLine = true,
                    isError = qtyText.isNotBlank() && (qty == null || qty < 1 || qty > remaining),
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
            Button(enabled = valid, onClick = { onConfirm(qty!!, priceCents!!, notes.ifBlank { null }) }) { Text("Registrera") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

@Composable
private fun ChangePriceDialog(
    currentCents: Int,
    onDismiss: () -> Unit,
    onConfirm: (newCents: Int) -> Unit,
) {
    var priceText by remember { mutableStateOf("%.2f".format(currentCents / 100.0)) }
    val newCents = priceText.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toInt() }
    val valid = newCents != null && newCents >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sänk pris") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Nuvarande: ${"%.2f".format(currentCents / 100.0)} kr", fontSize = 12.sp, color = FaltetForest)
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Nytt pris (SEK)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onConfirm(newCents!!) }) { Text("Sänk") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

@Composable
private fun ChangeOutletDialog(
    currentOutletId: Long,
    outlets: List<OutletResponse>,
    onDismiss: () -> Unit,
    onConfirm: (newOutletId: Long) -> Unit,
    onCreateOutlet: (name: String, channel: String, onCreated: (Long) -> Unit) -> Unit,
) {
    var selected by remember { mutableStateOf(currentOutletId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Byt utlopp") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutletPicker(
                    outlets = outlets,
                    selectedId = selected,
                    onSelected = { selected = it },
                    onCreateOutlet = { name, channel ->
                        onCreateOutlet(name, channel) { id -> selected = id }
                    },
                )
            }
        },
        confirmButton = {
            Button(enabled = selected != currentOutletId, onClick = { onConfirm(selected) }) { Text("Byt") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}
