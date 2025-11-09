package com.example.travelcompanion.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.travelcompanion.ui.home.HomeScreen
import com.example.travelcompanion.ui.stats.StatsScreen
import com.example.travelcompanion.ui.tripdetail.TripDetailScreen
import com.example.travelcompanion.ui.triplist.TripListScreen
import com.example.travelcompanion.ui.prediction.PredictionScreen


@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTripList = {
                    navController.navigate(Screen.TripList.route)
                },
                onNavigateToTripDetail = { tripId ->
                    navController.navigate(Screen.TripDetail.createRoute(tripId))
                }
            )
        }

        composable(Screen.TripList.route) {
            TripListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTripClick = { tripId ->
                    navController.navigate(Screen.TripDetail.createRoute(tripId))
                }
            )
        }

        composable(
            route = Screen.TripDetail.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getLong("tripId") ?: return@composable
            TripDetailScreen(
                tripId = tripId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Prediction.route) {
            PredictionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
