package com.arisucast.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : AppDestination(
        route = "home",
        label = "홈",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Search : AppDestination(
        route = "search",
        label = "검색",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )

    object Library : AppDestination(
        route = "library",
        label = "라이브러리",
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic
    )

    object Settings : AppDestination(
        route = "settings",
        label = "설정",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

val bottomNavDestinations = listOf(
    AppDestination.Home,
    AppDestination.Search,
    AppDestination.Library,
    AppDestination.Settings
)

// Non-bottom-nav destinations
object AppRoutes {
    const val PLAYER = "player"
    const val EPISODES = "episodes/{podcastId}"
    const val SUBSCRIPTIONS = "subscriptions"

    fun episodes(podcastId: String) = "episodes/$podcastId"
}
