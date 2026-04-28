package app.verdant.android.ui.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.verdant.android.ui.activity.AddSeedsScreen
import app.verdant.android.ui.activity.AddSpeciesScreen
import app.verdant.android.ui.activity.ApplySupplyScreen
import app.verdant.android.ui.activity.BatchPotUpScreen
import app.verdant.android.ui.activity.DiscardActivityScreen
import app.verdant.android.ui.activity.HarvestActivityScreen
import app.verdant.android.ui.activity.PlantActivityScreen
import app.verdant.android.ui.activity.PlantPickerScreen
import app.verdant.android.ui.activity.PotUpActivityScreen
import app.verdant.android.ui.activity.RecoverActivityScreen
import app.verdant.android.ui.activity.RegisterPlantsScreen
import app.verdant.android.ui.activity.SowActivityScreen
import app.verdant.android.ui.activity.SpeciesListScreen
import app.verdant.android.ui.navigation.Screen

fun NavGraphBuilder.activityGraph(navController: NavController) {
    composable(Screen.SpeciesList.route) {
        SpeciesListScreen(
            onBack = { navController.popBackStack() },
            onAddSpecies = { navController.navigate(Screen.AddSpecies.route) },
            onEditSpecies = { speciesId -> navController.navigate(Screen.EditSpecies.create(speciesId)) },
            onSowSpecies = { speciesId -> navController.navigate(Screen.Sow.create(speciesId = speciesId)) },
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
        arguments = listOf(navArgument("speciesId") { type = NavType.LongType }),
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
        ),
    ) {
        SowActivityScreen(
            onBack = { navController.popBackStack() },
            onSowComplete = { gardenId ->
                val target = gardenId?.let { Screen.GardenDetail.create(it) } ?: Screen.MyWorld.route
                navController.navigate(target) {
                    popUpTo(Screen.Dashboard.route)
                }
            },
        )
    }
    composable(
        Screen.PlantPicker.route,
        arguments = listOf(
            navArgument("statuses") { type = NavType.StringType },
            navArgument("nextRoute") { type = NavType.StringType },
            navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
            navArgument("speciesId") { type = NavType.LongType; defaultValue = -1L },
        ),
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
            },
        )
    }
    composable(
        Screen.PotUp.route,
        arguments = listOf(
            navArgument("plantId") { type = NavType.LongType },
            navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
        ),
    ) {
        PotUpActivityScreen(onBack = { navController.popBackStack() })
    }
    composable(
        Screen.BatchPotUp.route,
        arguments = listOf(
            navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
            navArgument("speciesId") { type = NavType.LongType; defaultValue = -1L },
        ),
    ) {
        BatchPotUpScreen(onBack = { navController.popBackStack() })
    }
    composable(
        Screen.PlantOut.route,
        arguments = listOf(
            navArgument("plantId") { type = NavType.LongType },
            navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
        ),
    ) {
        PlantActivityScreen(onBack = { navController.popBackStack() })
    }
    composable(
        Screen.Harvest.route,
        arguments = listOf(
            navArgument("plantId") { type = NavType.LongType },
            navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
        ),
    ) {
        HarvestActivityScreen(onBack = { navController.popBackStack() })
    }
    composable(
        Screen.Recover.route,
        arguments = listOf(
            navArgument("plantId") { type = NavType.LongType },
            navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
        ),
    ) {
        RecoverActivityScreen(onBack = { navController.popBackStack() })
    }
    composable(
        Screen.Discard.route,
        arguments = listOf(
            navArgument("plantId") { type = NavType.LongType },
            navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
        ),
    ) {
        DiscardActivityScreen(onBack = { navController.popBackStack() })
    }
    composable(
        Screen.ApplySupply.route,
        arguments = listOf(
            navArgument("bedId") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("trayLocationId") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("plantIds") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("stepId") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("supplyTypeId") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("quantity") { type = NavType.StringType; nullable = true; defaultValue = null },
        ),
    ) { backStackEntry ->
        val bedId = backStackEntry.arguments?.getString("bedId")?.toLongOrNull()
        val trayLocationId = backStackEntry.arguments?.getString("trayLocationId")?.toLongOrNull()
        val plantIds = backStackEntry.arguments?.getString("plantIds")
            ?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        val stepId = backStackEntry.arguments?.getString("stepId")?.toLongOrNull()
        val supplyTypeId = backStackEntry.arguments?.getString("supplyTypeId")?.toLongOrNull()
        val quantity = backStackEntry.arguments?.getString("quantity")?.toDoubleOrNull()
        ApplySupplyScreen(
            bedId = bedId,
            trayLocationId = trayLocationId,
            initialPlantIds = plantIds,
            suggestedSupplyTypeId = supplyTypeId,
            suggestedQuantity = quantity,
            workflowStepId = stepId,
            onBack = { navController.popBackStack() },
        )
    }
}
