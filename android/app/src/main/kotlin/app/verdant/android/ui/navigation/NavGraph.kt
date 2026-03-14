package app.verdant.android.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import app.verdant.android.data.SessionManager
import app.verdant.android.ui.account.AccountScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import app.verdant.android.ui.activity.*
import app.verdant.android.ui.auth.AuthScreen
import app.verdant.android.ui.bed.BedDetailScreen
import app.verdant.android.ui.bed.CreateBedScreen
import app.verdant.android.ui.garden.CreateGardenScreen
import app.verdant.android.ui.garden.GardenDetailScreen
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

    // Activity screens
    data object AddSpecies : Screen("activity/add-species")
    data object Sow : Screen("activity/sow")
    data object PlantPicker : Screen("activity/plant-picker/{statuses}/{nextRoute}") {
        fun create(statuses: String, nextRoute: String) = "activity/plant-picker/$statuses/$nextRoute"
    }
    data object PotUp : Screen("activity/pot-up/{plantId}") {
        fun create(plantId: Long) = "activity/pot-up/$plantId"
    }
    data object PlantOut : Screen("activity/plant-out/{plantId}") {
        fun create(plantId: Long) = "activity/plant-out/$plantId"
    }
    data object Harvest : Screen("activity/harvest/{plantId}") {
        fun create(plantId: Long) = "activity/harvest/$plantId"
    }
    data object Recover : Screen("activity/recover/{plantId}") {
        fun create(plantId: Long) = "activity/recover/$plantId"
    }
    data object Discard : Screen("activity/discard/{plantId}") {
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

    val hideBottomBar = currentRoute in listOf(Screen.Splash.route, Screen.Auth.route)

    // Redirect to login when session expires (JWT valid but user deleted)
    LaunchedEffect(Unit) {
        viewModel.sessionManager.expired.collect {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    var showActivitySheet by remember { mutableStateOf(false) }

    if (showActivitySheet) {
        ActivitySheet(
            onDismiss = { showActivitySheet = false },
            onActivitySelected = { activity ->
                when (activity) {
                    Activity.ADD_SPECIES -> navController.navigate(Screen.AddSpecies.route)
                    Activity.SOW -> navController.navigate(Screen.Sow.route)
                    Activity.POT_UP -> navController.navigate(Screen.PlantPicker.create("SEEDED", "pot-up"))
                    Activity.PLANT -> navController.navigate(Screen.PlantPicker.create("SEEDED,POTTED_UP", "plant-out"))
                    Activity.HARVEST -> navController.navigate(Screen.PlantPicker.create("GROWING", "harvest"))
                    Activity.RECOVER -> navController.navigate(Screen.PlantPicker.create("GROWING", "recover"))
                    Activity.DISCARD -> navController.navigate(Screen.PlantPicker.create("SEEDED,POTTED_UP,PLANTED_OUT,GROWING,HARVESTED,RECOVERED", "discard"))
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (!hideBottomBar) {
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
                        icon = { Icon(Icons.Default.Eco, contentDescription = "My World") },
                        label = { Text("My World") }
                    )

                    // Center FAB-style Activities button
                    NavigationBarItem(
                        selected = false,
                        onClick = { showActivitySheet = true },
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp).offset(y = (-4).dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Activities",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        },
                        label = { Text("Activities") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Account.route,
                        onClick = {
                            navController.navigate(Screen.Account.route) {
                                popUpTo(Screen.MyWorld.route)
                            }
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                        label = { Text("Account") }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(paddingValues)
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

            // ── Activity screens ──

            composable(Screen.AddSpecies.route) {
                AddSpeciesScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Sow.route) {
                SowActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.PlantPicker.route,
                arguments = listOf(
                    navArgument("statuses") { type = NavType.StringType },
                    navArgument("nextRoute") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                val nextRoute = backStackEntry.arguments?.getString("nextRoute") ?: ""
                PlantPickerScreen(
                    onBack = { navController.popBackStack() },
                    onPlantSelected = { plantId ->
                        val target = "activity/$nextRoute/$plantId"
                        navController.navigate(target) {
                            popUpTo(Screen.MyWorld.route)
                        }
                    }
                )
            }
            composable(
                Screen.PotUp.route,
                arguments = listOf(navArgument("plantId") { type = NavType.LongType })
            ) {
                PotUpActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.PlantOut.route,
                arguments = listOf(navArgument("plantId") { type = NavType.LongType })
            ) {
                PlantActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.Harvest.route,
                arguments = listOf(navArgument("plantId") { type = NavType.LongType })
            ) {
                HarvestActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.Recover.route,
                arguments = listOf(navArgument("plantId") { type = NavType.LongType })
            ) {
                RecoverActivityScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.Discard.route,
                arguments = listOf(navArgument("plantId") { type = NavType.LongType })
            ) {
                DiscardActivityScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
