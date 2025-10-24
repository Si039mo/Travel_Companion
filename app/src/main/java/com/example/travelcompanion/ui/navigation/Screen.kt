package com.example.travelcompanion.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object TripList : Screen("trip_list")
    object TripDetail : Screen("trip_detail/{tripId}") {
        fun createRoute(tripId: Long) = "trip_detail/$tripId"
    }
    object Stats : Screen("stats")
}
