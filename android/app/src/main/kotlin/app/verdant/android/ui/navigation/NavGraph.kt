package app.verdant.android.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import app.verdant.android.ui.account.AccountScreen
import app.verdant.android.ui.auth.AuthScreen
import app.verdant.android.ui.bed.BedDetailScreen
import app.verdant.android.ui.bed.CreateBedScreen
import app.verdant.android.ui.dashboard.DashboardScreen
import app.verdant.android.ui.garden.CreateGardenScreen
import app.verdant.android.ui.garden.GardenDetailScreen
import app.verdant.android.ui.plant.AddPlantEventScreen
import app.verdant.android.ui.plant.CreatePlantScreen
import app.verdant.android.ui.plant.PlantDetailScreen
import app.verdant.android.ui.splash.SplashScreen
import app.verdant.android.ui.stats.HarvestStatsScreen

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object Dashboard : Screen("dashboard")
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
    data object HarvestStats : Screen("stats/harvests")
    data object Account : Screen("account")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerdantNavHost() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val hideBottomBar = currentRoute in listOf(Screen.Splash.route, Screen.Auth.route)

    Scaffold(
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Dashboard.route,
                        onClick = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.HarvestStats.route,
                        onClick = { navController.navigate(Screen.HarvestStats.route) { popUpTo(Screen.Dashboard.route) } },
                        icon = { Icon(Icons.Default.Grass, contentDescription = "Plants") },
                        label = { Text("Plants") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Account.route,
                        onClick = { navController.navigate(Screen.Account.route) { popUpTo(Screen.Dashboard.route) } },
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
                    onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) { inclusive = true } } }
                )
            }
            composable(Screen.Auth.route) {
                AuthScreen(
                    onAuthSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) { inclusive = true } } }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onGardenClick = { gardenId -> navController.navigate(Screen.GardenDetail.create(gardenId)) },
                    onCreateGarden = { navController.navigate(Screen.CreateGarden.route) }
                )
            }
            composable(Screen.CreateGarden.route) {
                CreateGardenScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { gardenId -> navController.navigate(Screen.GardenDetail.create(gardenId)) { popUpTo(Screen.Dashboard.route) } }
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
                    onCreated = { bedId -> navController.navigate(Screen.BedDetail.create(bedId)) { popUpTo(Screen.Dashboard.route) } }
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
                // Refresh when returning from add event screen
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
            composable(Screen.HarvestStats.route) {
                HarvestStatsScreen()
            }
            composable(Screen.Account.route) {
                AccountScreen(
                    onSignedOut = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } }
                )
            }
        }
    }
}
