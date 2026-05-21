package com.example.runcoach

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import com.example.runcoach.data.health.HealthConnectManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.runcoach.presentation.MainViewModel
import com.example.runcoach.presentation.MainViewModelFactory
import com.example.runcoach.presentation.screens.CalendarScreen
import com.example.runcoach.presentation.screens.DashboardScreen
import com.example.runcoach.presentation.screens.HistoryScreen
import com.example.runcoach.presentation.screens.OnboardingScreen
import com.example.runcoach.presentation.screens.PermissionSetupScreen
import com.example.runcoach.presentation.screens.PlanScreen
import com.example.runcoach.presentation.screens.TestRunScreen
import com.example.runcoach.ui.theme.RunCoachTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as RunCoachApplication
        MainViewModelFactory(
            prefsRepository = app.userPreferencesRepository,
            workoutDao = app.database.workoutDao(),
            application = app
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userPrefs by viewModel.userPreferences.collectAsState()
            val context = LocalContext.current

            val isDarkTheme = when (userPrefs.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            RunCoachTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    val healthConnectManager = remember { HealthConnectManager(context) }
                    var hasCheckedPermissions by remember { mutableStateOf(false) }
                    var hcPermissionsGranted by remember { mutableStateOf(false) }

                    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }

                    val hcStatus = remember {
                        try {
                            HealthConnectClient.getSdkStatus(context)
                        } catch (e: Exception) {
                            HealthConnectClient.SDK_UNAVAILABLE
                        }
                    }
                    val isHcAvailable = hcStatus == HealthConnectClient.SDK_AVAILABLE

                    LaunchedEffect(Unit) {
                        hcPermissionsGranted = healthConnectManager.hasPermissions()
                        hasCheckedPermissions = true

                        val isHcOk = hcPermissionsGranted || !isHcAvailable
                        if (hasNotificationPermission && isHcOk && !userPrefs.hasCompletedPermissionSetup && userPrefs.raceDate.isNotEmpty()) {
                            com.example.runcoach.utils.AppLogger.d("All permissions already granted. Auto-completing permission setup.")
                            viewModel.completePermissionSetup()
                        }
                    }

                    val startDestination = when {
                        userPrefs.raceDate.isEmpty() -> "onboarding"
                        !userPrefs.hasCompletedPermissionSetup -> {
                            val isHcOk = hcPermissionsGranted || !isHcAvailable
                            if (hasCheckedPermissions && hasNotificationPermission && isHcOk) {
                                if (!userPrefs.hasCompletedTestRun) "test_run" else "dashboard"
                            } else {
                                "permission_setup"
                            }
                        }
                        !userPrefs.hasCompletedTestRun -> "test_run"
                        else -> "dashboard"
                    }

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val bottomNavRoutes = listOf("dashboard", "calendar", "history", "plan")

                    Scaffold(
                        bottomBar = {
                            if (currentRoute in bottomNavRoutes) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ) {
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ") },
                                        label = { Text("Trang chủ") },
                                        selected = currentRoute == "dashboard",
                                        onClick = {
                                            navController.navigate("dashboard") {
                                                popUpTo("dashboard") { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.List, contentDescription = "Giáo án") },
                                        label = { Text("Giáo án") },
                                        selected = currentRoute == "plan",
                                        onClick = {
                                            navController.navigate("plan") {
                                                popUpTo("dashboard") { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.History, contentDescription = "Lịch sử") },
                                        label = { Text("Lịch sử") },
                                        selected = currentRoute == "history",
                                        onClick = {
                                            navController.navigate("history") {
                                                popUpTo("dashboard") { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Lịch tháng") },
                                        label = { Text("Lịch tháng") },
                                        selected = currentRoute == "calendar",
                                        onClick = {
                                            navController.navigate("calendar") {
                                                popUpTo("dashboard") { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        contentWindowInsets = WindowInsets(0.dp)
                    ) { innerPadding ->
                        NavHost(
                            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                            navController = navController,
                            startDestination = startDestination,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(300))
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(300))
                        }
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                viewModel = viewModel,
                                onNavigateToTestRun = {
                                    navController.navigate("permission_setup") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("permission_setup") {
                            PermissionSetupScreen(
                                viewModel = viewModel,
                                onNavigateNext = {
                                    navController.navigate("test_run") {
                                        popUpTo("permission_setup") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("test_run") {
                            TestRunScreen(
                                viewModel = viewModel,
                                onNavigateToDashboard = {
                                    navController.navigate("dashboard") {
                                        popUpTo("test_run") { inclusive = true }
                                    }
                                },
                                onNavigateToOnboarding = {
                                    navController.navigate("onboarding") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("dashboard") {
                            // Guard: if state was reset, redirect back to onboarding
                            if (userPrefs.raceDate.isEmpty()) {
                                navController.navigate("onboarding") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else if (!userPrefs.hasCompletedTestRun) {
                                navController.navigate("test_run") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToPlan = { navController.navigate("plan") },
                                    onNavigateToHistory = { navController.navigate("history") },
                                    onNavigateToCalendar = { navController.navigate("calendar") }
                                )
                            }
                        }

                        composable("plan") {
                            PlanScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("history") {
                            HistoryScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("calendar") {
                            CalendarScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
            }
        }
    }
}