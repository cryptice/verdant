package app.verdant.android.ui.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.verdant.android.ui.inventory.SeedInventoryScreen
import app.verdant.android.ui.inventory.SeedPlanScreen
import app.verdant.android.ui.location.TrayLocationDetailScreen
import app.verdant.android.ui.location.TrayLocationsScreen
import app.verdant.android.ui.navigation.Screen
import app.verdant.android.ui.season.SeasonSelectorScreen
import app.verdant.android.ui.supplies.SupplyInventoryScreen

fun NavGraphBuilder.inventoryGraph(navController: NavController) {
    composable(Screen.SeedInventory.route) {
        SeedInventoryScreen(
            onBack = { navController.popBackStack() },
            onAddSeeds = { navController.navigate(Screen.AddSeeds.route) },
            onPlanBatch = { batchId -> navController.navigate(Screen.SeedPlan.create(batchId)) },
        )
    }
    composable(
        Screen.SeedPlan.route,
        arguments = listOf(navArgument("batchId") { type = NavType.LongType }),
    ) {
        SeedPlanScreen(
            onBack = { navController.popBackStack() },
            onShowTasks = {
                navController.navigate(Screen.TaskList.route) {
                    popUpTo(Screen.SeedInventory.route)
                }
            },
        )
    }
    composable(Screen.Supplies.route) {
        SupplyInventoryScreen(onBack = { navController.popBackStack() })
    }
    composable(Screen.Seasons.route) {
        SeasonSelectorScreen(onBack = { navController.popBackStack() })
    }
    composable(Screen.TrayLocations.route) {
        TrayLocationsScreen(
            onBack = { navController.popBackStack() },
            onLocationClick = { id -> navController.navigate(Screen.TrayLocationDetail.create(id)) },
        )
    }
    composable(
        Screen.TrayLocationDetail.route,
        arguments = listOf(navArgument("locationId") { type = NavType.LongType }),
    ) {
        TrayLocationDetailScreen(
            onBack = { navController.popBackStack() },
            onSpeciesClick = { speciesId ->
                navController.navigate(Screen.PlantedSpeciesDetail.create(speciesId))
            },
            onFertilize = { locationId ->
                navController.navigate(Screen.ApplySupply.create(trayLocationId = locationId))
            },
            onDeleted = { navController.popBackStack() },
        )
    }
}
