package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.workout.WorkoutScreen
import com.example.ui.screens.nutrition.NutritionScreen
import com.example.ui.screens.progress.ProgressScreen
import com.example.ui.screens.coach.CoachScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.login.LoginScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Workout : Screen("workout")
    object Nutrition : Screen("nutrition")
    object Progress : Screen("progress")
    object Coach : Screen("coach")
    object Profile : Screen("profile")
    object Login : Screen("login")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    viewModel: com.example.MainViewModel
) {
    NavHost(
        navController = navController, 
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(220))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(animationSpec = tween(220))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(220))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(animationSpec = tween(220))
        }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onAnimationEnd = {
                navController.navigate(startDestination) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Login.route) {
            LoginScreen(viewModel, onLoginFinish = {
                val profile = viewModel.profile.value
                val nextStartRoute = if (profile?.isSetupComplete == true) Screen.Home.route else Screen.Onboarding.route
                navController.navigate(nextStartRoute) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(viewModel, onFinish = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onNutritionClick = {
                    viewModel.setScrollToNutritionTrends(true)
                    navController.navigate(Screen.Nutrition.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onWorkoutClick = {
                    navController.navigate(Screen.Workout.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onProgressClick = {
                    navController.navigate(Screen.Progress.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(Screen.Workout.route) {
            WorkoutScreen(viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Nutrition.route) {
            NutritionScreen(viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Progress.route) {
            ProgressScreen(viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Coach.route) {
            CoachScreen(viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEraseAllData = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
