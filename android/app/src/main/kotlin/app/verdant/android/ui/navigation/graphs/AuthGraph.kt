package app.verdant.android.ui.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.verdant.android.ui.auth.AuthScreen
import app.verdant.android.ui.navigation.Screen
import app.verdant.android.ui.onboarding.create.CreateOrgScreen
import app.verdant.android.ui.onboarding.invite.InviteOfferScreen
import app.verdant.android.ui.onboarding.join.JoinOrgScreen
import app.verdant.android.ui.splash.SplashScreen

fun NavGraphBuilder.authGraph(navController: NavController) {
    composable(Screen.Splash.route) {
        SplashScreen(
            onNavigateToAuth = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } },
            onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) { inclusive = true } } },
            onNavigateToInviteOffer = { navController.navigate(Screen.InviteOffer.route) { popUpTo(0) { inclusive = true } } },
            onNavigateToNoOrg = { navController.navigate(Screen.NoOrg.route) { popUpTo(0) { inclusive = true } } },
        )
    }
    composable(Screen.Auth.route) {
        AuthScreen(
            onAuthSuccess = { navController.navigate(Screen.Splash.route) { popUpTo(0) { inclusive = true } } },
            onNeedsOnboarding = { navController.navigate(Screen.Splash.route) { popUpTo(0) { inclusive = true } } },
        )
    }
    composable(Screen.InviteOffer.route) {
        InviteOfferScreen(
            onJoined = { navController.navigate(Screen.Splash.route) { popUpTo(0) { inclusive = true } } },
            onAllDeclined = { navController.navigate(Screen.NoOrg.route) { popUpTo(0) { inclusive = true } } },
            onCreateOrg = { navController.navigate(Screen.CreateOrg.route) },
            onJoinByName = { navController.navigate(Screen.NoOrg.route) },
            onSignedOut = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } },
        )
    }
    composable(Screen.NoOrg.route) {
        JoinOrgScreen(
            onJoined = { navController.navigate(Screen.Splash.route) { popUpTo(0) { inclusive = true } } },
            onCreateOrg = { navController.navigate(Screen.CreateOrg.route) },
            onSignedOut = { navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } } },
        )
    }
    composable(Screen.CreateOrg.route) {
        CreateOrgScreen(
            onCreated = { navController.navigate(Screen.Splash.route) { popUpTo(0) { inclusive = true } } },
            onBack = { navController.popBackStack() },
        )
    }
}
