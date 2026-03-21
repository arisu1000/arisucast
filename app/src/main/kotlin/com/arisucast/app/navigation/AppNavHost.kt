package com.arisucast.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.arisucast.feature.episodes.presentation.EpisodeListScreen
import com.arisucast.feature.home.presentation.HomeScreen
import com.arisucast.feature.player.presentation.PlayerScreen
import com.arisucast.feature.search.presentation.SearchScreen
import com.arisucast.feature.settings.presentation.SettingsScreen
import com.arisucast.feature.subscriptions.presentation.SubscriptionsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Home.route,
        modifier = modifier
    ) {
        composable(AppDestination.Home.route) {
            HomeScreen(
                onPodcastClick = { podcastId ->
                    navController.navigate(AppRoutes.episodes(podcastId))
                },
                onPlayerClick = {
                    navController.navigate(AppRoutes.PLAYER)
                }
            )
        }

        composable(AppDestination.Search.route) {
            SearchScreen(
                onPodcastClick = { podcastId ->
                    navController.navigate(AppRoutes.episodes(podcastId))
                }
            )
        }

        composable(
            route = AppDestination.Library.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "arisucast://subscribe?url={feedUrl}" }
            ),
            arguments = listOf(navArgument("feedUrl") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val deepLinkUrl = backStackEntry.arguments?.getString("feedUrl")
            SubscriptionsScreen(
                deepLinkFeedUrl = deepLinkUrl,
                onPodcastClick = { podcastId ->
                    navController.navigate(AppRoutes.episodes(podcastId))
                }
            )
        }

        composable(AppDestination.Settings.route) {
            SettingsScreen()
        }

        composable(
            route = AppRoutes.EPISODES,
            arguments = listOf(navArgument("podcastId") { type = NavType.StringType })
        ) { backStackEntry ->
            val podcastId = backStackEntry.arguments?.getString("podcastId") ?: return@composable
            EpisodeListScreen(
                podcastId = podcastId,
                onNavigateUp = { navController.navigateUp() },
                onPlayerClick = { navController.navigate(AppRoutes.PLAYER) }
            )
        }

        composable(AppRoutes.PLAYER) {
            PlayerScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }
    }
}
