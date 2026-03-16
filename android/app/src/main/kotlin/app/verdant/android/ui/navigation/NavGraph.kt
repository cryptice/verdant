package app.verdant.android.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import app.verdant.android.R
import app.verdant.android.data.SessionManager
import app.verdant.android.ui.account.AccountScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import app.verdant.android.ui.activity.*
import app.verdant.android.ui.auth.AuthScreen
import app.verdant.android.ui.plants.PlantedSpeciesListScreen
import app.verdant.android.ui.plants.PlantedSpeciesDetailScreen
import app.verdant.android.ui.task.TaskListScreen
import app.verdant.android.ui.task.TaskFormScreen
import app.verdant.android.ui.bed.BedDetailScreen
import app.verdant.android.ui.bed.CreateBedScreen
import app.verdant.android.ui.garden.CreateGardenScreen
import app.verdant.android.ui.garden.GardenDetailScreen
import app.verdant.android.ui.inventory.SeedInventoryScreen
import app.verdant.android.ui.plant.AddPlantEventScreen
import app.verdant.android.ui.plant.CreatePlantScreen
import app.verdant.android.ui.plant.PlantDetailScreen
import app.verdant.android.ui.splash.SplashScreen
import app.verdant.android.ui.world.MyVerdantWorldScreen

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object MyWorld : Screen("my-world")
    data object CreateGarden : Screen("garden/create")
    data object GardenDetail : Screen("garden/{gardenId}") {
        fun create(gardenId: Long) = "garden/$gardenId"
    }
    data object CreateBed : Screen("garden/{gardenId}/bed/create") {
        fun create(gardenId: Long) = "garden/$gardenId/bed/create"
    }
    data object BedDetail : Screen("bed/{bedId}") {
        fun create(bedId: Long) = "bed/$bedId"
    }
    data object CreatePlant : Screen("bed/{bedId}/plant/create") {
        fun create(bedId: Long) = "bed/$bedId/plant/create"
    }
    data object PlantDetail : Screen("plant/{plantId}") {
        fun create(plantId: Long) = "plant/$plantId"
    }
    data object AddPlantEvent : Screen("plant/{plantId}/event/add") {
        fun create(plantId: Long) = "plant/$plantId/event/add"
    }
    data object Account : Screen("account")
    data object SeedInventory : Screen("seed-inventory")
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

    // Activity screens
    data object AddSpecies : Screen("activity/add-species")
    data object EditSpecies : Screen("activity/edit-species/{speciesId}") {
        fun create(speciesId: Long) = "activity/edit-species/$speciesId"
    }
    data object AddSeeds : Screen("activity/add-seeds")
    data object Sow : Screen("activity/sow?taskId={taskId}&speciesId={speciesId}") {
        fun create(taskId: Long? = null, speciesId: Long? = null): String {
            val base = "activity/sow"
            val params = mutableListOf<String>()
            if (taskId != null) params.add("taskId=$taskId")
            if (speciesId != null) params.add("speciesId=$speciesId")
            return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
        }
    }
    data object PlantPicker : Screen("activity/plant-picker/{statuses}/{nextRoute}?taskId={taskId}&speciesId={speciesId}") {
        fun create(statuses: String, nextRoute: String) = "activity/plant-picker/$statuses/$nextRoute"
    }
    data object PotUp : Screen("activity/pot-up/{plantId}?taskId={taskId}") {
        fun create(plantId: Long) = "activity/pot-up/$plantId"
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
}

@HiltViewModel
class NavViewModel @Inject constructor(
    val sessionManager: SessionManager,
) : ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerdantNavHost(viewModel: NavViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val hideChrome = currentRoute in listOf(Screen.Splash.route, Screen.Auth.route)
    val showTopBar = currentRoute == Screen.MyWorld.route

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
        gesturesEnabled = !hideChrome,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.app_name),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.species)) },
                    icon = { Icon(Icons.Default.Spa, contentDescription = null) },
                    selected = currentRoute == Screen.SpeciesList.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.SpeciesList.route) {
                            popUpTo(Screen.MyWorld.route)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.seed_inventory)) },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                    selected = currentRoute == Screen.SeedInventory.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.SeedInventory.route) {
                            popUpTo(Screen.MyWorld.route)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.account)) },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    selected = currentRoute == Screen.Account.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Account.route) {
                            popUpTo(Screen.MyWorld.route)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (showTopBar) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu))
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!hideChrome) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == Screen.MyWorld.route,
                            onClick = {
                                navController.navigate(Screen.MyWorld.route) {
                                    popUpTo(Screen.MyWorld.route) { inclusive = true }
                                }
                            },
                            icon = { Icon(Icons.Default.Eco, contentDescription = stringResource(R.string.my_world)) },
                            label = { Text(stringResource(R.string.my_world)) }
                        )

                        NavigationBarItem(
                            selected = currentRoute == Screen.PlantedSpeciesList.route,
                            onClick = {
                                navController.navigate(Screen.PlantedSpeciesList.route) {
                                    popUpTo(Screen.MyWorld.route)
                                }
                            },
                            icon = { Icon(Icons.Default.Yard, contentDescription = stringResource(R.string.plants)) },
                            label = { Text(stringResource(R.string.plants)) }
                        )

                        // Tasks button
                        NavigationBarItem(
                            selected = currentRoute == Screen.TaskList.route,
                            onClick = {
                                navController.navigate(Screen.TaskList.route) {
                                    popUpTo(Screen.MyWorld.route)
                                }
                            },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.scheduled_tasks)) },
                            label = { Text(stringResource(R.string.scheduled_tasks)) }
                        )
                    }
                }
            }
        ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToAuth = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } },
                    onNavigateToDashboard = { navController.navigate(Screen.MyWorld.route) { popUpTo(0) { inclusive = true } } }
                )
            }
            composable(Screen.Auth.route) {
                AuthScreen(
                    onAuthSuccess = { navController.navigate(Screen.MyWorld.route) { popUpTo(0) { inclusive = true } } }
                )
            }
            composable(Screen.MyWorld.route) {
                MyVerdantWorldScreen(
                    onGardenClick = { gardenId -> navController.navigate(Screen.GardenDetail.create(gardenId)) },
                    onCreateGarden = { navController.navigate(Screen.CreateGarden.route) }
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
                    onCreateBed = { gardenId -> navController.navigate(Screen.CreateBed.create(gardenId)) }
                )
            }
            composable(
                Screen.CreateBed.route,
                arguments = listOf(navArgument("gardenId") { type = NavType.LongType })
            ) {
                CreateBedScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { bedId -> navController.navigate(Screen.BedDetail.create(bedId)) { popUpTo(Screen.MyWorld.route) } }
                )
            }
            composable(
                Screen.BedDetail.route,
                arguments = listOf(navArgument("bedId") { type = NavType.LongType })
            ) {
                BedDetailScreen(
                    onBack = { navController.popBackStack() },
                    onPlantClick = { plantId -> navController.navigate(Screen.PlantDetail.create(plantId)) },
                    onCreatePlant = { bedId -> navController.navigate(Screen.CreatePlant.create(bedId)) }
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
                Screen.PlantDetail.route,
                arguments = listOf(navArgument("plantId") { type = NavType.LongType })
            ) { backStackEntry ->
                val refreshKey = backStackEntry.savedStateHandle.get<Boolean>("refresh")
                PlantDetailScreen(
                    onBack = { navController.popBackStack() },
                    onAddEvent = { plantId -> navController.navigate(Screen.AddPlantEvent.create(plantId)) },
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
                        when (task.activityType) {
                            "SOW" -> navController.navigate("activity/sow?taskId=${task.id}&speciesId=${task.speciesId}")
                            "POT_UP" -> navController.navigate("activity/plant-picker/SEEDED/pot-up?taskId=${task.id}&speciesId=${task.speciesId}")
                            "PLANT" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP/plant-out?taskId=${task.id}&speciesId=${task.speciesId}")
                            "HARVEST" -> navController.navigate("activity/plant-picker/GROWING/harvest?taskId=${task.id}&speciesId=${task.speciesId}")
                            "RECOVER" -> navController.navigate("activity/plant-picker/GROWING/recover?taskId=${task.id}&speciesId=${task.speciesId}")
                            "DISCARD" -> navController.navigate("activity/plant-picker/SEEDED,POTTED_UP,PLANTED_OUT,GROWING,HARVESTED,RECOVERED/discard?taskId=${task.id}&speciesId=${task.speciesId}")
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
                    onEditSpecies = { speciesId -> navController.navigate(Screen.EditSpecies.create(speciesId)) }
                )
            }
            composable(Screen.AddSpecies.route) {
                AddSpeciesScreen(onBack = { navController.popBackStack() })
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
                )
            ) {
                SowActivityScreen(onBack = { navController.popBackStack() })
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
        }
        }
    }
}
