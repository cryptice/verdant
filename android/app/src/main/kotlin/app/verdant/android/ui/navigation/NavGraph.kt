package app.verdant.android.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import app.verdant.android.R
import app.verdant.android.data.SessionManager
import app.verdant.android.ui.account.AccountScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import app.verdant.android.ui.activity.*
import app.verdant.android.ui.auth.AuthScreen
import app.verdant.android.ui.orgrequired.OrgRequiredScreen
import app.verdant.android.ui.theme.FaltetAccent
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20
import app.verdant.android.ui.theme.verdantTopAppBarColors
import app.verdant.android.ui.plants.PlantedSpeciesListScreen
import app.verdant.android.ui.plants.PlantedSpeciesDetailScreen
import app.verdant.android.ui.task.TaskListScreen
import app.verdant.android.ui.task.TaskFormScreen
import app.verdant.android.ui.bed.BedDetailScreen
import app.verdant.android.ui.bed.CreateBedScreen
import app.verdant.android.ui.garden.CreateGardenScreen
import app.verdant.android.ui.garden.GardenDetailScreen
import app.verdant.android.ui.inventory.SeedInventoryScreen
import app.verdant.android.ui.supplies.SupplyInventoryScreen
import app.verdant.android.ui.plant.AddPlantEventScreen
import app.verdant.android.ui.plant.CreatePlantScreen
import app.verdant.android.ui.plant.PlantDetailScreen
import app.verdant.android.ui.season.SeasonSelectorScreen
import app.verdant.android.ui.splash.SplashScreen
import app.verdant.android.ui.workflow.WorkflowProgressScreen
import app.verdant.android.ui.world.MyVerdantWorldScreen
import app.verdant.android.ui.pest.PestDiseaseLogScreen
import app.verdant.android.ui.customer.CustomerListScreen
import app.verdant.android.ui.succession.SuccessionSchedulesScreen
import app.verdant.android.ui.targets.ProductionTargetsScreen
import app.verdant.android.ui.trials.VarietyTrialsScreen
import app.verdant.android.ui.bouquet.BouquetRecipesScreen
import app.verdant.android.ui.bouquet.BouquetsScreen
import app.verdant.android.ui.analytics.AnalyticsScreen
import app.verdant.android.ui.dashboard.DashboardScreen

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object OrgRequired : Screen("org-required")
    data object Dashboard : Screen("dashboard")
    data object MyWorld : Screen("my-world")
    data object CreateGarden : Screen("garden/create")
    data object GardenDetail : Screen("garden/{gardenId}") {
        fun create(gardenId: Long) = "garden/$gardenId"
    }
    data object CreateBed : Screen("garden/{gardenId}/bed/create") {
        fun create(gardenId: Long) = "garden/$gardenId/bed/create"
    }
    data object BedDetail : Screen("bed/{bedId}?edit={edit}") {
        fun create(bedId: Long, edit: Boolean = false) = "bed/$bedId?edit=$edit"
    }
    data object CreatePlant : Screen("bed/{bedId}/plant/create") {
        fun create(bedId: Long) = "bed/$bedId/plant/create"
    }
    data object BatchPlantOut : Screen("bed/{bedId}/plant-from-tray") {
        fun create(bedId: Long) = "bed/$bedId/plant-from-tray"
    }
    data object PlantDetail : Screen("plant/{plantId}") {
        fun create(plantId: Long) = "plant/$plantId"
    }
    data object AddPlantEvent : Screen("plant/{plantId}/event/add") {
        fun create(plantId: Long) = "plant/$plantId/event/add"
    }
    data object Account : Screen("account")
    data object SeedInventory : Screen("seed-inventory")
    data object Supplies : Screen("supplies")
    data object SpeciesList : Screen("species")
    data object PlantedSpeciesList : Screen("planted-species")
    data object PlantedSpeciesDetail : Screen("planted-species/{speciesId}") {
        fun create(speciesId: Long) = "planted-species/$speciesId"
    }

    // Scheduled Tasks
    data object TaskList : Screen("tasks")
    data object CreateTask : Screen("tasks/create")
    data object EditTask : Screen("tasks/{taskId}/edit") {
        fun create(taskId: Long) = "tasks/$taskId/edit"
    }

    // Workflows
    data object WorkflowProgress : Screen("workflow-progress/{speciesId}") {
        fun create(speciesId: Long) = "workflow-progress/$speciesId"
    }

    // Seasons
    data object Seasons : Screen("seasons")

    // Parity screens
    data object PestDiseaseLog : Screen("pest-disease")
    data object Customers : Screen("customers")
    data object Successions : Screen("successions")
    data object Targets : Screen("targets")
    data object Trials : Screen("trials")
    data object Bouquets : Screen("bouquets")
    data object BouquetRecipes : Screen("bouquet-recipes")
    data object Analytics : Screen("analytics")

    // Activity screens
    data object AddSpecies : Screen("activity/add-species")
    data object RegisterPlants : Screen("activity/register-plants")
    data object EditSpecies : Screen("activity/edit-species/{speciesId}") {
        fun create(speciesId: Long) = "activity/edit-species/$speciesId"
    }
    data object AddSeeds : Screen("activity/add-seeds")
    data object Sow : Screen("activity/sow?taskId={taskId}&speciesId={speciesId}&bedId={bedId}") {
        fun create(taskId: Long? = null, speciesId: Long? = null, bedId: Long? = null): String {
            val base = "activity/sow"
            val params = mutableListOf<String>()
            if (taskId != null) params.add("taskId=$taskId")
            if (speciesId != null) params.add("speciesId=$speciesId")
            if (bedId != null) params.add("bedId=$bedId")
            return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
        }
    }
    data object PlantPicker : Screen("activity/plant-picker/{statuses}/{nextRoute}?taskId={taskId}&speciesId={speciesId}") {
        fun create(statuses: String, nextRoute: String) = "activity/plant-picker/$statuses/$nextRoute"
    }
    data object PotUp : Screen("activity/pot-up/{plantId}?taskId={taskId}") {
        fun create(plantId: Long) = "activity/pot-up/$plantId"
    }
    data object BatchPotUp : Screen("activity/batch-pot-up?taskId={taskId}&speciesId={speciesId}") {
        fun create(taskId: Long? = null, speciesId: Long? = null): String {
            val params = mutableListOf<String>()
            if (taskId != null) params.add("taskId=$taskId")
            if (speciesId != null) params.add("speciesId=$speciesId")
            return "activity/batch-pot-up" + if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        }
    }
    data object PlantOut : Screen("activity/plant-out/{plantId}?taskId={taskId}") {
        fun create(plantId: Long) = "activity/plant-out/$plantId"
    }
    data object Harvest : Screen("activity/harvest/{plantId}?taskId={taskId}") {
        fun create(plantId: Long) = "activity/harvest/$plantId"
    }
    data object Recover : Screen("activity/recover/{plantId}?taskId={taskId}") {
        fun create(plantId: Long) = "activity/recover/$plantId"
    }
    data object Discard : Screen("activity/discard/{plantId}?taskId={taskId}") {
        fun create(plantId: Long) = "activity/discard/$plantId"
    }
    data object ApplySupply : Screen("activity/apply-supply/{bedId}?plantIds={plantIds}&stepId={stepId}&supplyTypeId={supplyTypeId}&quantity={quantity}") {
        fun create(
            bedId: Long,
            plantIds: List<Long> = emptyList(),
            stepId: Long? = null,
            supplyTypeId: Long? = null,
            quantity: Double? = null,
        ): String {
            val base = "activity/apply-supply/$bedId"
            val params = buildList {
                if (plantIds.isNotEmpty()) add("plantIds=${plantIds.joinToString(",")}")
                if (stepId != null) add("stepId=$stepId")
                if (supplyTypeId != null) add("supplyTypeId=$supplyTypeId")
                if (quantity != null) add("quantity=$quantity")
            }
            return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
        }
    }
}

@HiltViewModel
class NavViewModel @Inject constructor(
    val sessionManager: SessionManager,
    private val gardenRepository: app.verdant.android.data.repository.GardenRepository,
) : ViewModel() {
    private val _gardens = kotlinx.coroutines.flow.MutableStateFlow<List<app.verdant.android.data.model.GardenResponse>>(emptyList())
    val gardens = _gardens.asStateFlow()

    init { refreshGardens() }

    fun refreshGardens() {
        viewModelScope.launch {
            runCatching { gardenRepository.getGardens() }.onSuccess { _gardens.value = it }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerdantNavHost(viewModel: NavViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // When the user has exactly one garden, the "My world" entry collapses into
    // a direct shortcut to that garden's detail screen — same UX as the web
    // sidebar.
    val gardens by viewModel.gardens.collectAsState()
    val soleGarden = gardens.singleOrNull()
    var showQuickActions by remember { mutableStateOf(false) }
    val myWorldLabel = soleGarden?.name ?: stringResource(R.string.my_world)
    val myWorldRoute = soleGarden?.let { Screen.GardenDetail.create(it.id) } ?: Screen.MyWorld.route
    val myWorldSelected = currentRoute == Screen.MyWorld.route ||
        (soleGarden != null && currentRoute == Screen.GardenDetail.route)

    val hideChrome = currentRoute in listOf(Screen.Splash.route, Screen.Auth.route, Screen.OrgRequired.route)
    val showTopBar = currentRoute == Screen.MyWorld.route
    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route, Screen.MyWorld.route, Screen.PlantedSpeciesList.route, Screen.TaskList.route, Screen.GardenDetail.route,
    )

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Redirect to login when session expires (JWT valid but user deleted)
    LaunchedEffect(Unit) {
        viewModel.sessionManager.expired.collect {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showTopBar,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = FaltetCream,
                drawerContentColor = FaltetInk,
            ) {
                Column(modifier = Modifier.fillMaxHeight().padding(top = 24.dp)) {
                    // Wordmark header
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = FaltetInk)) { append("Verdant") }
                                withStyle(SpanStyle(color = FaltetAccent)) { append(".") }
                            },
                            fontFamily = FaltetDisplay,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.W300,
                            fontSize = 26.sp,
                        )
                    }
                    Text(
                        text = "Est. 2026".uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.8.sp,
                        color = FaltetForest,
                        modifier = Modifier.padding(start = 18.dp, top = 4.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(FaltetInk))

                    // Scrollable nav body
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        // Section 1 — § ODLING
                        DrawerSection("§ Odling")
                        DrawerItem("Översikt", Screen.Dashboard.route, currentRoute, navController, scope, drawerState)
                        DrawerItem(myWorldLabel, myWorldRoute, currentRoute, navController, scope, drawerState)
                        DrawerItem("Växter", Screen.PlantedSpeciesList.route, currentRoute, navController, scope, drawerState)
                        DrawerItem("Arter", Screen.SpeciesList.route, currentRoute, navController, scope, drawerState)

                        // Section 2 — § UPPGIFTER
                        DrawerSection("§ Uppgifter")
                        DrawerItem("Uppgifter", Screen.TaskList.route, currentRoute, navController, scope, drawerState)
                        DrawerItem("Utsäde", Screen.SeedInventory.route, currentRoute, navController, scope, drawerState)
                        DrawerItem("Utrustning & förbrukning", Screen.Supplies.route, currentRoute, navController, scope, drawerState)
                        DrawerItem("Successioner", Screen.Successions.route, currentRoute, navController, scope, drawerState)
                        DrawerItem("Mål", Screen.Targets.route, currentRoute, navController, scope, drawerState)

                        // Section 3 — § SKÖRD & FÖRSÄLJNING
                        DrawerSection("§ Skörd & Försäljning")
                        DrawerItem("Kunder", Screen.Customers.route, currentRoute, navController, scope, drawerState)
                        DrawerItem("Buketter", Screen.Bouquets.route, currentRoute, navController, scope, drawerState)
                        DrawerItem("Bukettrecept", Screen.BouquetRecipes.route, currentRoute, navController, scope, drawerState)

                        // Section 4 — § ANALYS
                        DrawerSection("§ Analys")
                        DrawerItem("Försök", Screen.Trials.route, currentRoute, navController, scope, drawerState)
                        DrawerItem("Skadedjur & sjukdomar", Screen.PestDiseaseLog.route, currentRoute, navController, scope, drawerState)
                        DrawerItem("Analys", Screen.Analytics.route, currentRoute, navController, scope, drawerState)

                        // Section 5 — § PLAN
                        DrawerSection("§ Plan")
                        DrawerItem("Säsonger", Screen.Seasons.route, currentRoute, navController, scope, drawerState)

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    ) {
        // Hoist the drawer-open callback so every Masthead can render the
        // burger automatically without each screen wiring it through.
        val openDrawer: (() -> Unit)? = if (hideChrome) null else { { scope.launch { drawerState.open() } } }
        val openAccount: (() -> Unit)? = if (hideChrome) null else { { navController.navigate(Screen.Account.route) } }
        androidx.compose.runtime.CompositionLocalProvider(
            app.verdant.android.ui.faltet.LocalDrawerOpen provides openDrawer,
            app.verdant.android.ui.faltet.LocalAccountOpen provides openAccount,
        ) {
        Scaffold(
            topBar = {},
            bottomBar = {
                if (!hideChrome) {
                    NavigationBar(
                        containerColor = FaltetCream,
                        contentColor = FaltetInk,
                        modifier = Modifier
                            .drawBehind {
                                drawLine(
                                    color = FaltetInk,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == Screen.Dashboard.route,
                            onClick = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                }
                            },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Översikt") },
                            label = {
                                Text(
                                    text = "ÖVERSIKT",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.4.sp,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FaltetAccent,
                                selectedTextColor = FaltetAccent,
                                unselectedIconColor = FaltetForest,
                                unselectedTextColor = FaltetForest,
                                indicatorColor = Color.Transparent,
                            )
                        )

                        NavigationBarItem(
                            selected = currentRoute == Screen.PlantedSpeciesList.route,
                            onClick = {
                                navController.navigate(Screen.PlantedSpeciesList.route) {
                                    popUpTo(Screen.Dashboard.route)
                                }
                            },
                            icon = { Icon(Icons.Default.Yard, contentDescription = stringResource(R.string.plants)) },
                            label = {
                                Text(
                                    text = stringResource(R.string.plants).uppercase(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.4.sp,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FaltetAccent,
                                selectedTextColor = FaltetAccent,
                                unselectedIconColor = FaltetForest,
                                unselectedTextColor = FaltetForest,
                                indicatorColor = Color.Transparent,
                            )
                        )

                        NavigationBarItem(
                            selected = currentRoute == Screen.TaskList.route,
                            onClick = {
                                navController.navigate(Screen.TaskList.route) {
                                    popUpTo(Screen.Dashboard.route)
                                }
                            },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.scheduled_tasks)) },
                            label = {
                                Text(
                                    text = stringResource(R.string.scheduled_tasks).uppercase(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.4.sp,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FaltetAccent,
                                selectedTextColor = FaltetAccent,
                                unselectedIconColor = FaltetForest,
                                unselectedTextColor = FaltetForest,
                                indicatorColor = Color.Transparent,
                            )
                        )

                        NavigationBarItem(
                            selected = false,
                            onClick = { showQuickActions = true },
                            icon = { Icon(Icons.Default.Bolt, contentDescription = "Aktivitet") },
                            label = {
                                Text(
                                    text = "AKTIVITET",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.4.sp,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FaltetAccent,
                                selectedTextColor = FaltetAccent,
                                unselectedIconColor = FaltetAccent,
                                unselectedTextColor = FaltetAccent,
                                indicatorColor = Color.Transparent,
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(
                top = if (showTopBar) paddingValues.calculateTopPadding() else 0.dp,
                bottom = paddingValues.calculateBottomPadding()
            )
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToAuth = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } },
                    onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) { inclusive = true } } },
                    onNavigateToOrgRequired = { navController.navigate(Screen.OrgRequired.route) { popUpTo(0) { inclusive = true } } },
                )
            }
            composable(Screen.Auth.route) {
                AuthScreen(
                    onAuthSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) { inclusive = true } } },
                    onNeedsOrg = { navController.navigate(Screen.OrgRequired.route) { popUpTo(0) { inclusive = true } } },
                )
            }
            composable(Screen.OrgRequired.route) {
                OrgRequiredScreen(
                    onOrgReady = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) { inclusive = true } } },
                    onSignedOut = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } },
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onTaskClick = { task ->
                        val speciesParam = task.speciesId?.let { "&speciesId=$it" } ?: ""
                        when (task.activityType) {
                            "SOW" -> navController.navigate("activity/sow?taskId=${task.id}$speciesParam")
                            "POT_UP" -> navController.navigate(Screen.BatchPotUp.create(taskId = task.id, speciesId = task.speciesId))
                            "PLANT" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP/plant-out?taskId=${task.id}$speciesParam")
                            "HARVEST" -> navController.navigate("activity/plant-picker/GROWING/harvest?taskId=${task.id}$speciesParam")
                            "RECOVER" -> navController.navigate("activity/plant-picker/GROWING/recover?taskId=${task.id}$speciesParam")
                            "DISCARD" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP,PLANTED_OUT,GROWING,HARVESTED,RECOVERED/discard?taskId=${task.id}$speciesParam")
                            else -> navController.navigate(Screen.TaskList.route)
                        }
                    },
                    onOpenTasks = { navController.navigate(Screen.TaskList.route) },
                    onTrayAction = { action, speciesId ->
                        when (action) {
                            "POT_UP" -> navController.navigate(Screen.BatchPotUp.create(speciesId = speciesId))
                            "PLANT" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP/plant-out?speciesId=$speciesId")
                            "DISCARD" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP,PLANTED_OUT,GROWING,HARVESTED,RECOVERED/discard?speciesId=$speciesId")
                        }
                    },
                )
            }
            composable(Screen.MyWorld.route) {
                MyVerdantWorldScreen(
                    onGardenClick = { gardenId -> navController.navigate(Screen.GardenDetail.create(gardenId)) },
                    onCreateGarden = { navController.navigate(Screen.CreateGarden.route) },
                    onSow = { navController.navigate(Screen.Sow.create()) },
                    onTrayAction = { action, speciesId ->
                        when (action) {
                            "POT_UP" -> navController.navigate(Screen.BatchPotUp.create(speciesId = speciesId))
                            "PLANT" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP/plant-out?speciesId=$speciesId")
                            "DISCARD" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP,PLANTED_OUT,GROWING,HARVESTED,RECOVERED/discard?speciesId=$speciesId")
                        }
                    },
                )
            }
            composable(Screen.CreateGarden.route) {
                CreateGardenScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.navigate(Screen.MyWorld.route) { popUpTo(Screen.MyWorld.route) { inclusive = true } } }
                )
            }
            composable(
                Screen.GardenDetail.route,
                arguments = listOf(navArgument("gardenId") { type = NavType.LongType })
            ) {
                GardenDetailScreen(
                    onBack = { navController.popBackStack() },
                    onBedClick = { bedId -> navController.navigate(Screen.BedDetail.create(bedId)) },
                    onCreateBed = { gardenId -> navController.navigate(Screen.CreateBed.create(gardenId)) },
                    onTrayAction = { action, speciesId ->
                        when (action) {
                            "POT_UP" -> navController.navigate(Screen.BatchPotUp.create(speciesId = speciesId))
                            "PLANT" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP/plant-out?speciesId=$speciesId")
                            "DISCARD" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP,PLANTED_OUT,GROWING,HARVESTED,RECOVERED/discard?speciesId=$speciesId")
                        }
                    },
                )
            }
            composable(
                Screen.CreateBed.route,
                arguments = listOf(navArgument("gardenId") { type = NavType.LongType })
            ) {
                CreateBedScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { _ -> navController.popBackStack() }
                )
            }
            composable(
                Screen.BedDetail.route,
                arguments = listOf(
                    navArgument("bedId") { type = NavType.LongType },
                    navArgument("edit") { type = NavType.BoolType; defaultValue = false },
                )
            ) { backStackEntry ->
                val openEditOnStart = backStackEntry.arguments?.getBoolean("edit") ?: false
                BedDetailScreen(
                    onBack = { navController.popBackStack() },
                    onPlantClick = { plantId -> navController.navigate(Screen.PlantDetail.create(plantId)) },
                    onSowInBed = { bedId -> navController.navigate(Screen.Sow.create(bedId = bedId)) },
                    onPlantFromTray = { bedId -> navController.navigate(Screen.BatchPlantOut.create(bedId)) },
                    onFertilize = { bedId -> navController.navigate(Screen.ApplySupply.create(bedId)) },
                    onGardenClick = { gardenId -> navController.navigate(Screen.GardenDetail.create(gardenId)) },
                    onBedCopied = { newBedId ->
                        navController.navigate(Screen.BedDetail.create(newBedId, edit = true)) {
                            popUpTo(Screen.BedDetail.route) { inclusive = true }
                        }
                    },
                    openEditOnStart = openEditOnStart,
                )
            }
            composable(
                Screen.CreatePlant.route,
                arguments = listOf(navArgument("bedId") { type = NavType.LongType })
            ) {
                CreatePlantScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() }
                )
            }
            composable(
                Screen.BatchPlantOut.route,
                arguments = listOf(navArgument("bedId") { type = NavType.LongType })
            ) {
                BatchPlantOutScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.PlantDetail.route,
                arguments = listOf(navArgument("plantId") { type = NavType.LongType })
            ) { backStackEntry ->
                val refreshKey = backStackEntry.savedStateHandle.get<Boolean>("refresh")
                PlantDetailScreen(
                    onBack = { navController.popBackStack() },
                    onAddEvent = { plantId -> navController.navigate(Screen.AddPlantEvent.create(plantId)) },
                    onWorkflowProgress = { speciesId -> navController.navigate(Screen.WorkflowProgress.create(speciesId)) },
                    refreshKey = refreshKey
                )
            }
            composable(
                Screen.AddPlantEvent.route,
                arguments = listOf(navArgument("plantId") { type = NavType.LongType })
            ) {
                AddPlantEventScreen(
                    onBack = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Account.route) {
                AccountScreen(
                    onSignedOut = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } }
                )
            }
            composable(Screen.SeedInventory.route) {
                SeedInventoryScreen(
                    onBack = { navController.popBackStack() },
                    onAddSeeds = { navController.navigate(Screen.AddSeeds.route) }
                )
            }

            // ── Supplies ──

            composable(Screen.Supplies.route) {
                SupplyInventoryScreen(onBack = { navController.popBackStack() })
            }

            // ── Seasons ──

            composable(Screen.Seasons.route) {
                SeasonSelectorScreen(onBack = { navController.popBackStack() })
            }

            // ── Parity screens ──

            composable(Screen.PestDiseaseLog.route) {
                PestDiseaseLogScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Customers.route) {
                CustomerListScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Successions.route) {
                SuccessionSchedulesScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Targets.route) {
                ProductionTargetsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Trials.route) {
                VarietyTrialsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Bouquets.route) {
                BouquetsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRecipes = { navController.navigate(Screen.BouquetRecipes.route) },
                )
            }
            composable(Screen.BouquetRecipes.route) {
                BouquetRecipesScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(onBack = { navController.popBackStack() })
            }

            // ── Workflows ──

            composable(
                Screen.WorkflowProgress.route,
                arguments = listOf(navArgument("speciesId") { type = NavType.LongType })
            ) {
                WorkflowProgressScreen(
                    onBack = { navController.popBackStack() },
                    onApplySupplyStep = { bedId, stepId, plantIds, supplyTypeId, quantity ->
                        navController.navigate(
                            Screen.ApplySupply.create(
                                bedId = bedId,
                                plantIds = plantIds,
                                stepId = stepId,
                                supplyTypeId = supplyTypeId,
                                quantity = quantity,
                            )
                        )
                    },
                )
            }

            // ── Planted Species ──

            composable(Screen.PlantedSpeciesList.route) {
                PlantedSpeciesListScreen(
                    onBack = { navController.popBackStack() },
                    onSpeciesClick = { speciesId -> navController.navigate(Screen.PlantedSpeciesDetail.create(speciesId)) }
                )
            }
            composable(
                Screen.PlantedSpeciesDetail.route,
                arguments = listOf(navArgument("speciesId") { type = NavType.LongType })
            ) {
                PlantedSpeciesDetailScreen(onBack = { navController.popBackStack() })
            }

            // ── Scheduled Tasks ──

            composable(Screen.TaskList.route) {
                TaskListScreen(
                    onBack = { navController.popBackStack() },
                    onCreateTask = { navController.navigate(Screen.CreateTask.route) },
                    onEditTask = { taskId -> navController.navigate(Screen.EditTask.create(taskId)) },
                    onPerformTask = { task ->
                        val speciesParam = task.speciesId?.let { "&speciesId=$it" } ?: ""
                        when (task.activityType) {
                            "SOW" -> navController.navigate("activity/sow?taskId=${task.id}$speciesParam")
                            "POT_UP" -> navController.navigate(Screen.BatchPotUp.create(taskId = task.id, speciesId = task.speciesId))
                            "PLANT" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP/plant-out?taskId=${task.id}$speciesParam")
                            "HARVEST" -> navController.navigate("activity/plant-picker/GROWING/harvest?taskId=${task.id}$speciesParam")
                            "RECOVER" -> navController.navigate("activity/plant-picker/GROWING/recover?taskId=${task.id}$speciesParam")
                            "DISCARD" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP,PLANTED_OUT,GROWING,HARVESTED,RECOVERED/discard?taskId=${task.id}$speciesParam")
                        }
                    }
                )
            }
            composable(Screen.CreateTask.route) {
                TaskFormScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.EditTask.route,
                arguments = listOf(navArgument("taskId") { type = NavType.LongType })
            ) {
                TaskFormScreen(onBack = { navController.popBackStack() })
            }

            // ── Activity screens ──

            composable(Screen.SpeciesList.route) {
                SpeciesListScreen(
                    onBack = { navController.popBackStack() },
                    onAddSpecies = { navController.navigate(Screen.AddSpecies.route) },
                    onEditSpecies = { speciesId -> navController.navigate(Screen.EditSpecies.create(speciesId)) },
                    onSowSpecies = { speciesId -> navController.navigate(Screen.Sow.create(speciesId = speciesId)) }
                )
            }
            composable(Screen.AddSpecies.route) {
                AddSpeciesScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.RegisterPlants.route) {
                RegisterPlantsScreen(
                    onBack = { navController.popBackStack() },
                    onComplete = { navController.popBackStack() },
                )
            }
            composable(
                Screen.EditSpecies.route,
                arguments = listOf(navArgument("speciesId") { type = NavType.LongType })
            ) {
                AddSpeciesScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.AddSeeds.route) {
                AddSeedsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.Sow.route,
                arguments = listOf(
                    navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("speciesId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("bedId") { type = NavType.LongType; defaultValue = -1L },
                )
            ) {
                SowActivityScreen(
                    onBack = { navController.popBackStack() },
                    onSowComplete = { gardenId ->
                        val target = gardenId?.let { Screen.GardenDetail.create(it) } ?: Screen.MyWorld.route
                        navController.navigate(target) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
            }
            composable(
                Screen.PlantPicker.route,
                arguments = listOf(
                    navArgument("statuses") { type = NavType.StringType },
                    navArgument("nextRoute") { type = NavType.StringType },
                    navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("speciesId") { type = NavType.LongType; defaultValue = -1L },
                )
            ) { backStackEntry ->
                val nextRoute = backStackEntry.arguments?.getString("nextRoute") ?: ""
                val taskId = backStackEntry.arguments?.getLong("taskId")?.takeIf { it > 0 }
                PlantPickerScreen(
                    onBack = { navController.popBackStack() },
                    onPlantSelected = { plantId ->
                        val target = if (taskId != null) {
                            "activity/$nextRoute/$plantId?taskId=$taskId"
                        } else {
                            "activity/$nextRoute/$plantId"
                        }
                        navController.navigate(target) {
                            popUpTo(Screen.MyWorld.route)
                        }
                    }
                )
            }
            composable(
                Screen.PotUp.route,
                arguments = listOf(
                    navArgument("plantId") { type = NavType.LongType },
                    navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
                )
            ) {
                PotUpActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.BatchPotUp.route,
                arguments = listOf(
                    navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("speciesId") { type = NavType.LongType; defaultValue = -1L },
                )
            ) {
                BatchPotUpScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.PlantOut.route,
                arguments = listOf(
                    navArgument("plantId") { type = NavType.LongType },
                    navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
                )
            ) {
                PlantActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.Harvest.route,
                arguments = listOf(
                    navArgument("plantId") { type = NavType.LongType },
                    navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
                )
            ) {
                HarvestActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.Recover.route,
                arguments = listOf(
                    navArgument("plantId") { type = NavType.LongType },
                    navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
                )
            ) {
                RecoverActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.Discard.route,
                arguments = listOf(
                    navArgument("plantId") { type = NavType.LongType },
                    navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
                )
            ) {
                DiscardActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.ApplySupply.route,
                arguments = listOf(
                    navArgument("bedId") { type = NavType.LongType },
                    navArgument("plantIds") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("stepId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("supplyTypeId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("quantity") { type = NavType.StringType; nullable = true; defaultValue = null },
                )
            ) { backStackEntry ->
                val bedId = backStackEntry.arguments?.getLong("bedId") ?: 0L
                val plantIds = backStackEntry.arguments?.getString("plantIds")
                    ?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
                val stepId = backStackEntry.arguments?.getString("stepId")?.toLongOrNull()
                val supplyTypeId = backStackEntry.arguments?.getString("supplyTypeId")?.toLongOrNull()
                val quantity = backStackEntry.arguments?.getString("quantity")?.toDoubleOrNull()
                ApplySupplyScreen(
                    bedId = bedId,
                    initialPlantIds = plantIds,
                    suggestedSupplyTypeId = supplyTypeId,
                    suggestedQuantity = quantity,
                    workflowStepId = stepId,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (showQuickActions) {
            QuickActionsDialog(
                onDismiss = { showQuickActions = false },
                onSow = {
                    showQuickActions = false
                    navController.navigate(Screen.Sow.create())
                },
                onRegister = {
                    showQuickActions = false
                    navController.navigate(Screen.RegisterPlants.route)
                },
            )
        }
        }
        }
    }
}

@Composable
private fun QuickActionsDialog(
    onDismiss: () -> Unit,
    onSow: () -> Unit,
    onRegister: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FaltetCream, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .border(1.dp, FaltetInk, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .padding(24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "AKTIVITET",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.8.sp,
                color = FaltetForest,
            )
            Text(
                text = "Vad vill du göra?",
                fontFamily = FaltetDisplay,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 26.sp,
                color = FaltetInk,
            )
            QuickActionCard(
                icon = Icons.Default.Spa,
                title = "Så",
                subtitle = "Sätt nya frön i brätte eller bädd",
                onClick = onSow,
            )
            QuickActionCard(
                icon = Icons.Default.Yard,
                title = "Registrera plantor",
                subtitle = "Lägg till plantor du redan har",
                onClick = onRegister,
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.CenterEnd,
            ) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Avbryt", color = FaltetForest)
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FaltetInk, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(44.dp)
                .background(FaltetAccent.copy(alpha = 0.12f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = FaltetAccent, modifier = Modifier.size(22.dp))
        }
        androidx.compose.foundation.layout.Spacer(Modifier.width(14.dp))
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = FaltetDisplay,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 20.sp,
                color = FaltetInk,
            )
            Text(
                text = subtitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = FaltetForest,
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = FaltetAccent,
        )
    }
}

@Composable
private fun DrawerSection(title: String) {
    Text(
        text = title.uppercase(),
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        letterSpacing = 1.4.sp,
        color = FaltetForest.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun DrawerItem(
    label: String,
    route: String,
    currentRoute: String?,
    navController: NavController,
    scope: CoroutineScope,
    drawerState: DrawerState,
) {
    val active = currentRoute == route
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable {
                scope.launch { drawerState.close() }
                navController.navigate(route) {
                    popUpTo(Screen.MyWorld.route)
                }
            }
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 18.sp,
            color = if (active) FaltetAccent else FaltetInk,
            modifier = Modifier.weight(1f),
        )
        if (active) {
            Text(
                text = "●",
                fontFamily = FaltetDisplay,
                fontSize = 16.sp,
                color = FaltetAccent,
            )
        }
    }
}
