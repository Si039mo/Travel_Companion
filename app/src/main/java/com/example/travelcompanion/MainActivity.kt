package com.example.travelcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.travelcompanion.ui.navigation.NavGraph
import com.example.travelcompanion.ui.navigation.Screen
import androidx.compose.material.icons.filled.Info

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar(currentRoute)) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        icon = { Icon(Icons.Default.Home, "Home") },
                        label = { Text("Home") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.TripList.route,
                        onClick = {
                            navController.navigate(Screen.TripList.route)
                        },
                        icon = { Icon(Icons.Default.List, "Viaggi") },
                        label = { Text("Viaggi") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Stats.route,
                        onClick = {
                            navController.navigate(Screen.Stats.route)
                        },
                        icon = { Icon(Icons.Default.Info, "Stats") },  // ← CAMBIATO
                        label = { Text("Stats") }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

private fun shouldShowBottomBar(route: String?): Boolean {
    return route in listOf(
        Screen.Home.route,
        Screen.TripList.route,
        Screen.Stats.route
    )
}