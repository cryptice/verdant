package app.verdant.android.ui.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.verdant.android.ui.navigation.Screen
import app.verdant.android.ui.plant.AddPlantEventScreen
import app.verdant.android.ui.plant.PlantDetailScreen
import app.verdant.android.ui.plants.PlantedSpeciesDetailScreen
import app.verdant.android.ui.plants.PlantedSpeciesListScreen

fun NavGraphBuilder.plantGraph(navController: NavController) {
    composable(
        Screen.PlantDetail.route,
        arguments = listOf(navArgument("plantId") { type = NavType.LongType }),
    ) { backStackEntry ->
        val refreshKey = backStackEntry.savedStateHandle.get<Boolean>("refresh")
        PlantDetailScreen(
            onBack = { navController.popBackStack() },
            onAddEvent = { plantId -> navController.navigate(Screen.AddPlantEvent.create(plantId)) },
            onWorkflowProgress = { speciesId -> navController.navigate(Screen.WorkflowProgress.create(speciesId)) },
            refreshKey = refreshKey,
        )
    }
    composable(
        Screen.AddPlantEvent.route,
        arguments = listOf(navArgument("plantId") { type = NavType.LongType }),
    ) {
        AddPlantEventScreen(
            onBack = {
                navController.previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                navController.popBackStack()
            },
        )
    }
    composable(Screen.PlantedSpeciesList.route) {
        PlantedSpeciesListScreen(
            onBack = { navController.popBackStack() },
            onSpeciesClick = { speciesId -> navController.navigate(Screen.PlantedSpeciesDetail.create(speciesId)) },
        )
    }
    composable(
        Screen.PlantedSpeciesDetail.route,
        arguments = listOf(navArgument("speciesId") { type = NavType.LongType }),
    ) {
        PlantedSpeciesDetailScreen(
            onBack = { navController.popBackStack() },
            onPlantedOut = {
                navController.navigate(Screen.PlantedSpeciesList.route) {
                    popUpTo(Screen.PlantedSpeciesList.route) { inclusive = false }
                    launchSingleTop = true
                }
            },
        )
    }
}
