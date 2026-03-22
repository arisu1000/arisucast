package com.arisucast.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arisucast.app.navigation.AppNavHost
import com.arisucast.app.navigation.AppRoutes
import com.arisucast.app.navigation.bottomNavDestinations
import com.arisucast.app.viewmodel.MainViewModel
import com.arisucast.core.ui.component.AnimatedMiniPlayer
import com.arisucast.core.ui.theme.ArisuCastTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — Media3 will still work, just without notification */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val darkTheme by mainViewModel.darkTheme.collectAsStateWithLifecycle()
            ArisuCastTheme(darkTheme = darkTheme) {
                ArisuCastApp(mainViewModel = mainViewModel)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun ArisuCastApp(
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val playbackState by mainViewModel.playbackState.collectAsStateWithLifecycle()

    val isPlayerScreen = currentDestination?.route == AppRoutes.PLAYER
    val hasCurrentEpisode = playbackState.currentEpisode != null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isPlayerScreen) {
                Column {
                    // MiniPlayer above bottom nav
                    AnimatedMiniPlayer(
                        visible = hasCurrentEpisode,
                        episodeTitle = playbackState.currentEpisode?.title ?: "",
                        podcastTitle = playbackState.currentPodcastTitle ?: "",
                        imageUrl = playbackState.currentEpisode?.imageUrl ?: "",
                        isPlaying = playbackState.isPlaying,
                        progress = playbackState.progress,
                        onPlayPauseClick = mainViewModel::togglePlayPause,
                        onSkipNextClick = { /* queue - Phase 6 */ },
                        onPlayerClick = { navController.navigate(AppRoutes.PLAYER) }
                    )
                    NavigationBar {
                        bottomNavDestinations.forEach { destination ->
                            val isSelected = currentDestination?.hierarchy?.any {
                                it.route == destination.route
                            } == true

                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) destination.selectedIcon
                                                      else destination.unselectedIcon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = { Text(destination.label) },
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(destination.route) {
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
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
