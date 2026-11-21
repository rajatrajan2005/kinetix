package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.di.AppContainer
import com.example.ui.navigation.KinetixRoute
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.NutritionScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WorkoutScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.KinetixTheme
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltContainer
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.NutritionViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.WorkoutViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KinetixTheme {
                KinetixRoot()
            }
        }
    }
}

private data class NavItem(
    val route: KinetixRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val NAV_ITEMS = listOf(
    NavItem(KinetixRoute.Today, "Today", Icons.Filled.Home, Icons.Outlined.Home, "nav_today"),
    NavItem(KinetixRoute.Workout, "Workout", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter, "nav_workout"),
    NavItem(KinetixRoute.Nutrition, "Nutrition", Icons.Filled.Restaurant, Icons.Outlined.Restaurant, "nav_nutrition"),
    NavItem(KinetixRoute.Insights, "Insights", Icons.Filled.Insights, Icons.Outlined.Insights, "nav_insights"),
    NavItem(KinetixRoute.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
private fun KinetixRoot() {
    val context = LocalContext.current
    val container = AppContainer.get(context)

    val dashboardVm: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            userProfileRepository = container.userProfileRepository,
            nutritionRepository = container.nutritionRepository,
            waterRepository = container.waterRepository,
            stepRepository = container.stepRepository,
            workoutRepository = container.workoutRepository,
            stepSensorManager = container.stepSensorManager,
            waterAlarmScheduler = container.waterAlarmScheduler
        )
    )
    val workoutVm: WorkoutViewModel = viewModel(
        factory = WorkoutViewModel.Factory(
            workoutRepository = container.workoutRepository,
            userProfileRepository = container.userProfileRepository,
            mediaControlRepository = container.mediaControlRepository
        )
    )
    val nutritionVm: NutritionViewModel = viewModel(
        factory = NutritionViewModel.Factory(
            nutritionRepository = container.nutritionRepository
        )
    )
    val settingsVm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            userProfileRepository = container.userProfileRepository,
            waterRepository = container.waterRepository
        )
    )

    val todayState by dashboardVm.uiState.collectAsStateWithLifecycle()
    val workoutState by workoutVm.uiState.collectAsStateWithLifecycle()
    val nutritionState by nutritionVm.uiState.collectAsStateWithLifecycle()
    val profileState by settingsVm.profileState.collectAsStateWithLifecycle()

    if (profileState.hasCompletedOnboarding == 0) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AmoledBlack,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                OnboardingScreen(onComplete = dashboardVm::completeOnboarding)
            }
        }
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val navigateTab: (KinetixRoute) -> Unit = { target ->
        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AmoledBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 0.dp
                    ) {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricVolt,
                                    selectedTextColor = ElectricVolt,
                                    indicatorColor = VoltContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = ObsidianSurface) {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricVolt,
                                    selectedTextColor = ElectricVolt,
                                    indicatorColor = VoltContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = KinetixRoute.Today,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<KinetixRoute.Today> {
                        TodayScreen(
                            state = todayState,
                            onLogWater = dashboardVm::logWater,
                            onMarkWakeUpNow = dashboardVm::markWakeUpNow,
                            onSetWakeTime = dashboardVm::setWakeTime,
                            onRequestExactAlarmSettings = {
                                context.startActivity(container.waterAlarmScheduler.buildExactAlarmPermissionIntent())
                            },
                            onStartStepListener = dashboardVm::startForegroundStepListener,
                            onRefreshPermissions = dashboardVm::refreshPermissionsState,
                            onNavigateToWorkout = { navigateTab(KinetixRoute.Workout) },
                            onNavigateToNutrition = { navigateTab(KinetixRoute.Nutrition) }
                        )
                    }
                    composable<KinetixRoute.Workout> {
                        WorkoutScreen(
                            state = workoutState,
                            onRefreshMedia = workoutVm::refreshMediaBinding,
                            onOpenListenerSettings = {
                                context.startActivity(container.mediaControlRepository.buildNotificationListenerSettingsIntent())
                            },
                            onMediaPlayPause = workoutVm::mediaPlayPause,
                            onMediaPrevious = workoutVm::mediaPrevious,
                            onMediaNext = workoutVm::mediaNext,
                            onMediaSeekTo = workoutVm::mediaSeekTo,
                            onStartWorkout = workoutVm::startWorkout,
                            onFinishWorkout = workoutVm::finishWorkout,
                            onDiscardWorkout = workoutVm::discardWorkout,
                            onLogSet = workoutVm::logSet,
                            onDeleteSet = workoutVm::deleteSet,
                            onAdjustRestTimer = workoutVm::adjustRestTimer,
                            onStopRestTimer = workoutVm::stopRestTimer,
                            onSelectMuscleFilter = workoutVm::selectMuscleFilter,
                            onSelectHeadFilter = workoutVm::selectHeadFilter,
                            onPickerQueryChange = workoutVm::updatePickerQuery,
                            onAddAdHocExercise = workoutVm::addAdHocExerciseToToday,
                            onConfirmMakePermanent = workoutVm::confirmMakePermanent,
                            onDismissMakePermanent = workoutVm::dismissMakePermanent,
                            onAddSplitDay = workoutVm::addSplitDay,
                            onDeleteSplitDay = workoutVm::deleteSplitDay,
                            onRemoveSplitExercise = workoutVm::removeExerciseFromSplit,
                            onAddExerciseToSplitDay = workoutVm::addExerciseToSplitDay,
                            onImportSplitText = workoutVm::importSplitText
                        )
                    }
                    composable<KinetixRoute.Nutrition> {
                        NutritionScreen(
                            state = nutritionState,
                            onQueryChange = nutritionVm::onQueryChanged,
                            onRefreshSearch = nutritionVm::triggerImmediateRemoteSearch,
                            onLogFood = nutritionVm::logFood,
                            onAddCustomMealTotals = nutritionVm::addCustomMealTotals,
                            onAddCustomFoodPer100g = nutritionVm::addCustomFood,
                            onDeleteLog = nutritionVm::deleteLog
                        )
                    }
                    composable<KinetixRoute.Insights> {
                        InsightsScreen(
                            todayState = todayState,
                            workoutState = workoutState
                        )
                    }
                    composable<KinetixRoute.Settings> {
                        SettingsScreen(
                            profile = profileState,
                            onUpdateProfile = settingsVm::updateProfile,
                            onOpenExactAlarmSettings = {
                                context.startActivity(container.waterAlarmScheduler.buildExactAlarmPermissionIntent())
                            },
                            onOpenNotificationListenerSettings = {
                                context.startActivity(container.mediaControlRepository.buildNotificationListenerSettingsIntent())
                            }
                        )
                    }
                }
            }
        }
    }
}
