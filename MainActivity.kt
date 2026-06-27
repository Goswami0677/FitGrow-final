package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.FloatingBottomNav
import com.example.ui.components.NavItem
import com.example.ui.navigation.AppNavigation
import com.example.ui.navigation.Screen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request high / dynamic refresh rate (e.g. 144Hz/120Hz) for ultra-smooth UI performance
        try {
            val layoutParams = window.attributes
            layoutParams.preferredRefreshRate = 144f
            window.attributes = layoutParams
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val app = application as FitGrowApplication
            val viewModel: MainViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return MainViewModel(app, app.repository) as T
                    }
                }
            )
            
            MyApplicationTheme(darkTheme = true) {

                val navController = rememberNavController()
                val profile = viewModel.profile.collectAsState().value

                val startRoute = if (profile == null || profile.email.isNullOrBlank()) {
                    Screen.Login.route
                } else if (profile.isSetupComplete) {
                    Screen.Home.route
                } else {
                    Screen.Onboarding.route
                }

                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route ?: startRoute

                val hasUnreadCoach = viewModel.hasUnreadCoachMessage.collectAsState().value

                val navItems = listOf(
                    NavItem("Home", Screen.Home.route, Icons.Default.Home),
                    NavItem("Workout", Screen.Workout.route, Icons.Default.PlayArrow),
                    NavItem("Progress", Screen.Progress.route, Icons.Default.Face),
                    NavItem("Nutrition", Screen.Nutrition.route, Icons.AutoMirrored.Filled.List),
                    NavItem("Coach", Screen.Coach.route, Icons.Default.Email, hasBadge = hasUnreadCoach)
                )

                val showBottomNav = currentRoute != Screen.Onboarding.route && currentRoute != Screen.Splash.route && currentRoute != Screen.Login.route

                var totalDragX by remember { androidx.compose.runtime.mutableStateOf(0f) }
                val currentRouteIndex = navItems.indexOfFirst { it.route == currentRoute }
                val isTabRoute = currentRouteIndex >= 0

                val swipeModifier = if (isTabRoute) {
                    Modifier.pointerInput(currentRoute) {
                        val screenWidth = this.size.width.toFloat()
                        detectHorizontalDragGestures(
                            onDragStart = {
                                totalDragX = 0f
                            },
                            onDragEnd = {
                                if (screenWidth > 0) {
                                    val dragPercentage = totalDragX / screenWidth
                                    if (dragPercentage < -0.15f) { // dragging right-to-left -> go NEXT tab
                                        val nextIndex = (currentRouteIndex + 1).coerceAtMost(navItems.lastIndex)
                                        if (nextIndex != currentRouteIndex) {
                                            val targetRoute = navItems[nextIndex].route
                                            if (targetRoute == Screen.Home.route) {
                                                navController.popBackStack(Screen.Home.route, false)
                                            } else {
                                                navController.navigate(targetRoute) {
                                                    popUpTo(Screen.Home.route) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    } else if (dragPercentage > 0.15f) { // dragging left-to-right -> go PREVIOUS tab
                                        val prevIndex = (currentRouteIndex - 1).coerceAtLeast(0)
                                        if (prevIndex != currentRouteIndex) {
                                            val targetRoute = navItems[prevIndex].route
                                            if (targetRoute == Screen.Home.route) {
                                                navController.popBackStack(Screen.Home.route, false)
                                            } else {
                                                navController.navigate(targetRoute) {
                                                    popUpTo(Screen.Home.route) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    }
                                }
                                totalDragX = 0f
                            },
                            onDragCancel = {
                                totalDragX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount
                            }
                        )
                    }
                } else {
                    Modifier
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomNav) {
                            FloatingBottomNav(
                                items = navItems,
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    if (route == Screen.Home.route) {
                                        navController.popBackStack(Screen.Home.route, false)
                                    } else {
                                        navController.navigate(route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .then(swipeModifier)
                    ) {
                        AppNavigation(navController, startRoute, viewModel)
                    }
                }
            }
        }
    }
}

