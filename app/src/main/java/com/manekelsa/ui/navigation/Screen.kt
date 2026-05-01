package com.manekelsa.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.vector.ImageVector
import com.manekelsa.R

sealed class Screen(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.app_name, Icons.Default.Home)
    object Search : Screen("search", R.string.action_find_workers, Icons.Default.Search)
    object Profile : Screen("profile", R.string.action_my_profile, Icons.Default.Person)
    object CallHistory : Screen("call_history", R.string.action_call_history, Icons.Default.History)
    object Availability : Screen("availability", R.string.action_im_available, Icons.Default.CheckCircle)
    object ResidentProfile : Screen("resident_profile", R.string.resident_profile, Icons.Default.Person)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Profile
)
