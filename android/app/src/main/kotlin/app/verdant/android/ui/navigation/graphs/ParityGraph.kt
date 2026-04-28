package app.verdant.android.ui.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.verdant.android.ui.analytics.AnalyticsScreen
import app.verdant.android.ui.bouquet.BouquetRecipesScreen
import app.verdant.android.ui.bouquet.BouquetsScreen
import app.verdant.android.ui.customer.CustomerListScreen
import app.verdant.android.ui.navigation.Screen
import app.verdant.android.ui.pest.PestDiseaseLogScreen
import app.verdant.android.ui.succession.SuccessionSchedulesScreen
import app.verdant.android.ui.targets.ProductionTargetsScreen
import app.verdant.android.ui.trials.VarietyTrialsScreen
import app.verdant.android.ui.workflow.WorkflowProgressScreen

/**
 * Less-used screens that exist for parity with the web admin UI: pest log,
 * customers, successions, targets, trials, bouquets, analytics, workflows.
 */
fun NavGraphBuilder.parityGraph(navController: NavController) {
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
    composable(
        Screen.WorkflowProgress.route,
        arguments = listOf(navArgument("speciesId") { type = NavType.LongType }),
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
                    ),
                )
            },
        )
    }
}
