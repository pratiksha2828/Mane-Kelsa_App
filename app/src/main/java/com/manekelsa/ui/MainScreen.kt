package com.manekelsa.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.manekelsa.ui.navigation.Screen
import com.manekelsa.ui.model.UserRole
import com.manekelsa.ui.screens.HomeScreen
import com.manekelsa.ui.screens.ProfileScreen
import com.manekelsa.ui.screens.RoleSelectionScreen
import com.manekelsa.ui.screens.CallHistoryScreen
import com.manekelsa.ui.screens.ResidentProfileScreen
import com.manekelsa.ui.screens.SearchScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var userRole by rememberSaveable { mutableStateOf<UserRole?>(null) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }

    if (userRole == null) {
        RoleSelectionScreen(
            onContinue = { selectedRole, name, phone ->
                userRole = selectedRole
                displayName = name
                phoneNumber = phone
            }
        )
    } else {
        val navItems = when (userRole) {
            UserRole.HIRER -> listOf(Screen.Home, Screen.Search, Screen.ResidentProfile)
            UserRole.WORKER -> listOf(Screen.Home, Screen.Search, Screen.Profile)
            else -> emptyList()
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    navItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(stringResource(screen.titleRes)) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    val profileRoute = if (userRole == UserRole.HIRER) {
                        Screen.ResidentProfile.route
                    } else {
                        Screen.Profile.route
                    }
                    HomeScreen(
                        onNavigateToProfile = { navController.navigate(profileRoute) },
                        onNavigateToCallHistory = { navController.navigate(Screen.CallHistory.route) },
                        userRole = userRole ?: UserRole.HIRER,
                        onChangeRole = {
                            userRole = if (userRole == UserRole.HIRER) {
                                UserRole.WORKER
                            } else {
                                UserRole.HIRER
                            }
                        },
                        displayName = displayName,
                        phoneNumber = phoneNumber
                    )
                }
                composable(Screen.Search.route) { SearchScreen() }
                composable(Screen.Profile.route) { ProfileScreen() }
                composable(Screen.ResidentProfile.route) {
                    ResidentProfileScreen(
                        displayName = displayName,
                        phoneNumber = phoneNumber
                    )
                }
                composable(Screen.Availability.route) {
                    ProfileScreen()
                }
                composable(Screen.CallHistory.route) {
                    CallHistoryScreen()
                }
            }
        }
    }
}
