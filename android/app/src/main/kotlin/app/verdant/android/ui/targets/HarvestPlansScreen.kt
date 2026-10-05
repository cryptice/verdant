package app.verdant.android.ui.targets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.verdant.android.data.model.*
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import java.time.LocalDate
import java.util.UUID


@Composable
private fun Choice(label: String, value: String, options: List<Pair<String, String>>, enabled: Boolean, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label)
        Box {
            OutlinedButton(onClick = { open = true }, enabled = enabled) { Text(options.find { it.first == value }?.second ?: "Välj") }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { open = false; onChange(id) }) }
            }
        }
    }
}

@Composable
fun HarvestPlansScreen(onWeekly: () -> Unit, onSeasonsRequired: () -> Unit, planId: Long? = null, viewModel: HarvestPlansViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(planId) { viewModel.load(planId) }
    LaunchedEffect(state.needsSeason) {
        if (state.needsSeason) onSeasonsRequired()
    }
    if (state.needsSeason) return
    var season by remember { mutableStateOf("") }
    LaunchedEffect(state.seasons) {
        if (season.isBlank()) season = state.seasons.latestByYear()?.id?.toString().orEmpty()
    }
    var speciesId by remember { mutableStateOf<Long?>(null) }
    var groupId by remember { mutableStateOf<Long?>(null) }
    var targetName by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("300") }
    var date by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("STEM") }
    var fixed by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var edit by remember { mutableStateOf<PlanningSpecies?>(null) }
    var cancel by remember { mutableStateOf<HarvestPlanResponse?>(null) }
    val enabled = !state.busy
    val validDate = runCatching { LocalDate.parse(date) }.isSuccess

    FaltetScreenScaffold(mastheadLeft = "", mastheadCenter = "Skördeplaner") { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onWeekly) { Text("Veckomål") }
            if (state.busy) CircularProgressIndicator()
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { viewModel.load(planId) }, enabled = enabled) { Text("Ladda om") } }
            state.selected?.let { plan ->
                Text("${plan.snapshot.request.harvestDate} · ${plan.snapshot.request.quantity} ${planningUnit(plan.snapshot.request.sellableUnit)}", style = MaterialTheme.typography.titleLarge)
                Text("Registrera framsteg här. Inköp, växthändelser och verklig skörd registreras separat; uppgifterna ändrar inte lagersaldon.")
                if (plan.status == "CANCELLED") Text("Avbruten")
                plan.snapshot.allocations.forEach { allocation ->
                    Text(allocation.speciesName, style = MaterialTheme.typography.titleMedium)
                    allocation.steps.forEach { step ->
                        val task = plan.tasks.find { it.stepKey == step.key }
                        Text("${step.date} · ${step.name} · ${step.quantity} ${planningUnit(step.unit)}")
                        if (task != null) {
                            Text("${task.remainingCount} återstår · ${when (task.status) { "COMPLETED" -> "Klar"; "CANCELLED" -> "Avbruten"; else -> "Återstår" }}")
                            if (task.status == "PENDING" && plan.status == "ACTIVE") ProgressInput(task, enabled) { viewModel.complete(plan, task, it) }
                        }
                    }
                }
                if (plan.status == "ACTIVE") TextButton(onClick = { cancel = plan }, enabled = enabled) { Text("Avbryt planen") }
            }
            if (state.selected != null) TextButton(onClick = {
                season = state.seasons.latestByYear()?.id?.toString().orEmpty()
                viewModel.newPlan()
            }, enabled = enabled) { Text("Ny skördeplan") }
            if (state.selected == null) {
                Text("Planera säljbara enheter till ett skördedatum. Verdant föreslår lika stor skörd från arter med kompatibla, kompletta scheman.")
                Choice("Säsong", season, state.seasons.map { it.id.toString() to it.name }, enabled) { season = it; viewModel.invalidatePreview() }
                OutlinedTextField(search, { search = it }, label = { Text("Sök art") }, enabled = enabled, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.search(search) }, enabled = enabled && search.isNotBlank()) { Text("Sök") }
                state.searchResults.forEach { sp -> TextButton(enabled = enabled, onClick = {
                    speciesId = sp.id; groupId = null; targetName = listOfNotNull(sp.commonName, sp.variantName).joinToString(" — "); fixed = emptyMap(); viewModel.candidates(sp.id, null)
                }) { Text(listOfNotNull(sp.commonName, sp.variantName).joinToString(" — ")) } }
                Choice("Eller välj grupp", groupId?.toString().orEmpty(), state.groups.map { it.id.toString() to it.name }, enabled) {
                    groupId = it.toLong(); speciesId = null; targetName = state.groups.first { sp -> sp.id == groupId }.name; fixed = emptyMap(); viewModel.candidates(null, groupId)
                }
                Text(targetName)
                OutlinedTextField(quantity, { quantity = it; viewModel.invalidatePreview() }, label = { Text("Målantal") }, enabled = enabled)
                Choice("Säljbar enhet", unit, listOf("STEM", "FLOWER").map { it to planningUnit(it) }, enabled) { unit = it; viewModel.invalidatePreview() }
                OutlinedTextField(date, { date = it; viewModel.invalidatePreview() }, label = { Text("Skördedatum (ÅÅÅÅ-MM-DD)") }, enabled = enabled, isError = date.isNotEmpty() && !validDate)
                Text("Lämna antal tomt för förslag, ange 0 för att utesluta eller ange ett fast antal.")
                state.candidates.forEach { sp ->
                    OutlinedTextField(fixed[sp.speciesId].orEmpty(), { fixed = fixed + (sp.speciesId to it); viewModel.invalidatePreview() }, label = { Text(sp.speciesName) }, placeholder = { Text("Föreslagen andel") }, enabled = enabled, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { edit = sp; viewModel.invalidatePreview() }, enabled = enabled) { Text("Konfigurera livscykel") }
                }
                Button(enabled = enabled && season.isNotBlank() && (speciesId != null || groupId != null) && validDate &&
                    (quantity.toIntOrNull() ?: 0) in 1..1_000_000 && fixed.values.all { it.isBlank() || (it.toIntOrNull() ?: -1) >= 0 }, onClick = {
                    viewModel.preview(HarvestPlanRequest(season.toLong(), speciesId, groupId, quantity.toInt(), unit, date,
                        fixed.filterValues { it.isNotBlank() }.mapValues { it.value.toInt() }))
                }) { Text("Förhandsgranska plan") }
                state.preview?.let { preview ->
                    preview.issues.forEach { issue -> Text("${state.candidates.find { it.speciesId == issue.speciesId }?.speciesName.orEmpty()} ${issue.message}") }
                    preview.allocations.forEach { AllocationSummary(it) }
                    Button(onClick = viewModel::save, enabled = enabled && preview.canSave) { Text("Spara plan och skapa uppgifter") }
                }
            }
            HorizontalDivider()
            Text("Sparade planer", style = MaterialTheme.typography.titleLarge)
            state.plans.forEach { plan -> TextButton(onClick = { viewModel.select(plan.id) }, enabled = enabled) {
                Text("${plan.snapshot.request.harvestDate} · ${plan.snapshot.request.quantity} ${planningUnit(plan.snapshot.request.sellableUnit)} · ${plan.snapshot.allocations.joinToString { it.speciesName }}${if (plan.status == "CANCELLED") " · Avbruten" else ""}")
            } }
            Row {
                TextButton(onClick = { viewModel.page(state.page - 1) }, enabled = enabled && state.page > 0) { Text("Föregående") }
                TextButton(onClick = { viewModel.page(state.page + 1) }, enabled = enabled && state.plans.size >= 50) { Text("Nästa") }
            }

        }
    }
    edit?.let { sp -> ProfileDialog(sp, enabled, state.error, { if (enabled) edit = null }) { viewModel.profile(sp.speciesId, it) { edit = null } } }
    cancel?.let { plan -> AlertDialog(onDismissRequest = { cancel = null }, title = { Text("Avbryt planen") }, text = { Text("Återstående uppgifter avbryts. Utfört arbete och den ursprungliga planen sparas.") }, confirmButton = { TextButton(onClick = { viewModel.cancel(plan.id); cancel = null }, enabled = enabled) { Text("Avbryt planen") } }, dismissButton = { TextButton(onClick = { cancel = null }) { Text("Tillbaka") } }) }
}

@Composable
private fun AllocationSummary(allocation: SpeciesAllocation) {
    Text("${allocation.speciesName} · ${allocation.outputQuantity} ${planningUnit(allocation.profile.sellableUnit)}", style = MaterialTheme.typography.titleMedium)
    Text("${allocation.plantsNeeded} plantor · ${allocation.startingQuantity} ${planningUnit(allocation.profile.startingUnit)}")
    allocation.steps.forEach { Text("${it.date} · ${it.name} · ${it.quantity} ${planningUnit(it.unit)}") }
}

@Composable
private fun ProgressInput(task: HarvestPlanTask, enabled: Boolean, complete: (Int) -> Unit) {
    var quantity by remember(task.id, task.remainingCount) { mutableStateOf(task.remainingCount.toString()) }
    OutlinedTextField(quantity, { quantity = it }, label = { Text("Utfört antal") }, enabled = enabled)
    Button(onClick = { complete(quantity.toInt()) }, enabled = enabled && (quantity.toIntOrNull() ?: 0) in 1..task.remainingCount) { Text("Registrera framsteg") }
}

@Composable
private fun ProfileDialog(species: PlanningSpecies, enabled: Boolean, error: String?, close: () -> Unit, save: (ProductionProfile) -> Unit) {
    var profile by remember(species.speciesId) { mutableStateOf(species.profile) }
    var yield by remember { mutableStateOf(profile.outputPerPlant?.toString().orEmpty()) }
    var success by remember { mutableStateOf(profile.establishmentPercent?.toString().orEmpty()) }
    AlertDialog(onDismissRequest = close, title = { Text(species.speciesName) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Organisationens inställningar. Ange säljbara enheter per planta på måldatumet, inte hela säsongens avkastning. Kontrollera föreslagna tider och etableringsgrad för din odling.")
            Choice("Säljbar enhet", profile.sellableUnit, listOf("STEM", "FLOWER").map { it to planningUnit(it) }, enabled) { profile = profile.copy(sellableUnit = it) }
            OutlinedTextField(yield, { yield = it }, label = { Text("Enheter per planta på måldatumet") }, enabled = enabled)
            OutlinedTextField(success, { success = it }, label = { Text("Etableringsgrad (%)") }, enabled = enabled)
            Choice("Utgångsmaterial", profile.startingUnit, listOf("SEED", "PLUG", "BULB", "TUBER", "PLANT").map { it to planningUnit(it) }, enabled) { profile = profile.copy(startingUnit = it) }
            Text("Stegen ska vara i tidsordning. Inköp först, skörd sist på dag 0.")
            profile.steps.forEachIndexed { index, step -> key(step.key) {
                fun update(value: LifecycleStep) { profile = profile.copy(steps = profile.steps.mapIndexed { i, s -> if (i == index) value else s }) }
                HorizontalDivider()
                OutlinedTextField(step.name, { update(step.copy(name = it)) }, label = { Text("Stegets namn") }, enabled = enabled)
                Choice("Aktivitet", step.activityType, listOf("PURCHASE", "SOW", "POT_UP", "PLANT", "PINCH", "SUPPORT", "WATER", "FERTILIZE", "HARVEST", "TODO").map { it to planningActivity(it) }, enabled) { update(step.copy(activityType = it)) }
                OutlinedTextField(step.daysBeforeHarvest?.toString().orEmpty(), { if (it.isEmpty() || it.toIntOrNull() != null) update(step.copy(daysBeforeHarvest = it.toIntOrNull())) }, label = { Text("Dagar före skörd") }, enabled = enabled)
                Choice("Mät antal som", step.quantityBasis, listOf("START" to "Utgångsmaterial", "PLANT" to "Plantor", "OUTPUT" to "Säljbara enheter"), enabled) { update(step.copy(quantityBasis = it)) }
                TextButton(enabled = enabled, onClick = { profile = profile.copy(steps = profile.steps.filterIndexed { i, _ -> i != index }) }) { Text("Ta bort steg") }
            } }
            TextButton(enabled = enabled && profile.steps.size < 30, onClick = { profile = profile.copy(steps = profile.steps.dropLast(1) + LifecycleStep(UUID.randomUUID().toString(), "", "TODO", null, "PLANT") + profile.steps.takeLast(1)) }) { Text("Lägg till steg") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = enabled && yield.toDoubleOrNull()?.isFinite() == true && success.toDoubleOrNull()?.isFinite() == true, onClick = { save(profile.copy(outputPerPlant = yield.toDouble(), establishmentPercent = success.toDouble())) }) { Text("Spara") } }, dismissButton = { TextButton(onClick = close, enabled = enabled) { Text("Tillbaka") } })
}
