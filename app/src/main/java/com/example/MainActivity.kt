package com.example

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.di.AppContainer
import com.example.ui.components.applePressScale
import com.example.ui.navigation.KinetixRoute
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.NutritionScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WorkoutScreen
import com.example.ui.theme.KinetixTheme
import com.example.ui.theme.KinetixTokens
import com.example.ui.theme.rememberReduceAnimations
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.NutritionViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.WorkoutViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
    val activeAccent: Color,
    val tag: String
)

private val NAV_ITEMS = listOf(
    NavItem(KinetixRoute.Today, "Today", Icons.Filled.Home, Icons.Outlined.Home, KinetixTokens.Colors.Calories, "nav_today"),
    NavItem(KinetixRoute.Workout, "Workout", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter, KinetixTokens.Colors.Workout, "nav_workout"),
    NavItem(KinetixRoute.Nutrition, "Nutrition", Icons.Filled.Restaurant, Icons.Outlined.Restaurant, KinetixTokens.Colors.Calories, "nav_nutrition"),
    NavItem(KinetixRoute.Insights, "Insights", Icons.Filled.Insights, Icons.Outlined.Insights, KinetixTokens.Colors.Hydration, "nav_insights"),
    NavItem(KinetixRoute.Settings, "Profile", Icons.Filled.Person, Icons.Outlined.Person, KinetixTokens.Colors.TextPrimary, "nav_settings")
)

private fun tabIndexFor(route: KinetixRoute): Int {
    return NAV_ITEMS.indexOfFirst { it.route == route }.coerceAtLeast(0)
}

private val KinetixRouteSaver: Saver<KinetixRoute, Int> = Saver(
    save = { tabIndexFor(it) },
    restore = { idx -> NAV_ITEMS.getOrElse(idx) { NAV_ITEMS.first() }.route }
)

@Composable
private fun KinetixRoot() {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val container = AppContainer.get(context)
    val reduceMotion = rememberReduceAnimations()

    val dashboardVm: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            userProfileRepository = container.userProfileRepository,
            nutritionRepository = container.nutritionRepository,
            waterRepository = container.waterRepository,
            stepRepository = container.stepRepository,
            workoutRepository = container.workoutRepository,
            progressAndCycleRepository = container.progressAndCycleRepository,
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
            waterRepository = container.waterRepository,
            progressAndCycleRepository = container.progressAndCycleRepository
        )
    )

    var canExactAlarm by remember { mutableStateOf(container.waterAlarmScheduler.canScheduleExactAlarms()) }
    var hasMediaListener by remember { mutableStateOf(container.mediaControlRepository.isAccessGranted()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canExactAlarm = container.waterAlarmScheduler.canScheduleExactAlarms()
                hasMediaListener = container.mediaControlRepository.isAccessGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onboardingDone by remember(settingsVm) {
        settingsVm.profileState
            .map { it.hasCompletedOnboarding != 0 }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    if (!onboardingDone) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = KinetixTokens.Colors.Background,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                OnboardingScreen(onComplete = dashboardVm::completeOnboarding)
            }
        }
        return
    }

    var currentRoute by rememberSaveable(stateSaver = KinetixRouteSaver) {
        mutableStateOf<KinetixRoute>(KinetixRoute.Today)
    }
    val tabBackStack = remember { mutableStateListOf<KinetixRoute>() }
    val saveableStateHolder = rememberSaveableStateHolder()

    val navigateTab: (KinetixRoute) -> Unit = remember {
        { target ->
            if (target != currentRoute) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                if (target == KinetixRoute.Today) {
                    tabBackStack.clear()
                } else {
                    tabBackStack.remove(target)
                    tabBackStack.add(currentRoute)
                }
                currentRoute = target
            }
        }
    }

    val navigateBack: () -> Unit = remember {
        {
            currentRoute = if (tabBackStack.isNotEmpty()) {
                tabBackStack.removeAt(tabBackStack.lastIndex)
            } else {
                KinetixRoute.Today
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KinetixTokens.Colors.Background)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    if (useRail) {
                        NavigationRail(containerColor = KinetixTokens.Colors.CardSurface) {
                            NAV_ITEMS.forEach { item ->
                                val selected = currentRoute == item.route
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
                                        selectedIconColor = item.activeAccent,
                                        selectedTextColor = item.activeAccent,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = KinetixTokens.Colors.TextSecondary,
                                        unselectedTextColor = KinetixTokens.Colors.TextSecondary
                                    ),
                                    modifier = Modifier.testTag(item.tag)
                                )
                            }
                        }
                    }

                    // Smooth ~200ms cross-fade + slight scale (0.98f -> 1f), respecting system reduce-motion
                    AnimatedContent(
                        targetState = currentRoute,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        transitionSpec = {
                            val enter = fadeIn(
                                animationSpec = KinetixTokens.Motion.tabFadeTween(reduceMotion)
                            ) + scaleIn(
                                initialScale = if (reduceMotion) 1f else 0.98f,
                                animationSpec = KinetixTokens.Motion.tabFadeTween(reduceMotion)
                            )
                            val exit = fadeOut(
                                animationSpec = KinetixTokens.Motion.tabFadeTween(reduceMotion)
                            )
                            enter togetherWith exit
                        },
                        contentKey = { route -> tabIndexFor(route) },
                        label = "kinetix_tab_transition"
                    ) { route ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            saveableStateHolder.SaveableStateProvider(key = tabIndexFor(route)) {
                                when (route) {
                                    KinetixRoute.Today -> {
                                        val todayState by dashboardVm.uiState.collectAsStateWithLifecycle()
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
                                            onNavigateToNutrition = { navigateTab(KinetixRoute.Nutrition) },
                                            onNavigateToInsights = { navigateTab(KinetixRoute.Insights) },
                                            onNavigateToProfile = { navigateTab(KinetixRoute.Settings) }
                                        )
                                    }

                                    KinetixRoute.Workout -> {
                                        BackHandler(onBack = navigateBack)
                                        val workoutState by workoutVm.uiState.collectAsStateWithLifecycle()
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
                                            onSelectActiveExercise = workoutVm::selectActiveExercise,
                                            onUpdateExerciseConfig = workoutVm::updateExerciseSessionConfig,
                                            onSkipActiveExercise = workoutVm::skipExerciseForToday,
                                            onInspectExercise = workoutVm::inspectExerciseById,
                                            onDismissExerciseInspect = workoutVm::dismissExerciseInspection,
                                            onLogSet = workoutVm::logSet,
                                            onDeleteSet = workoutVm::deleteSet,
                                            onToggleRestPause = workoutVm::toggleRestTimerPause,
                                            onAdjustRestTimer = workoutVm::adjustRestTimer,
                                            onStopRestTimer = workoutVm::stopRestTimer,
                                            onStartRestTimer = workoutVm::startRestTimer,
                                            onSelectMuscleFilter = workoutVm::selectMuscleFilter,
                                            onSelectHeadFilter = workoutVm::selectHeadFilter,
                                            onPickerQueryChange = workoutVm::updatePickerQuery,
                                            onAddAdHocExercise = workoutVm::addAdHocExerciseToToday,
                                            onConfirmMakePermanent = workoutVm::confirmMakePermanent,
                                            onDismissMakePermanent = workoutVm::dismissMakePermanent,
                                            onAddSplitDay = workoutVm::addSplitDay,
                                            onAddRestDay = workoutVm::addRestDay,
                                            onUpdateSplitDay = workoutVm::updateSplitDay,
                                            onReorderSplitExercises = workoutVm::reorderSplitExercises,
                                            onDeleteSplitDay = workoutVm::deleteSplitDay,
                                            onRemoveSplitExercise = workoutVm::removeExerciseFromSplit,
                                            onAddExerciseToSplitDay = workoutVm::addExerciseToSplitDay,
                                            onImportSplitText = workoutVm::importSplitText,
                                            onApplySplitPreset = workoutVm::applySplitPreset,
                                            onSelectSport = workoutVm::selectSport
                                        )
                                    }

                                    KinetixRoute.Nutrition -> {
                                        BackHandler(onBack = navigateBack)
                                        val nutritionState by nutritionVm.uiState.collectAsStateWithLifecycle()
                                        NutritionScreen(
                                            state = nutritionState,
                                            onQueryChange = nutritionVm::onQueryChanged,
                                            onRefreshSearch = nutritionVm::triggerImmediateRemoteSearch,
                                            onLogFood = nutritionVm::logFood,
                                            onAddCustomMealTotals = nutritionVm::addCustomMealTotals,
                                            onAddCustomFoodPer100g = nutritionVm::addCustomFood,
                                            onDeleteCustomFood = nutritionVm::deleteCustomFood,
                                            onUpdateLog = nutritionVm::updateFoodLog,
                                            onDeleteLog = nutritionVm::deleteLog
                                        )
                                    }

                                    KinetixRoute.Insights -> {
                                        BackHandler(onBack = navigateBack)
                                        val todayState by dashboardVm.uiState.collectAsStateWithLifecycle()
                                        val workoutState by workoutVm.uiState.collectAsStateWithLifecycle()
                                        val progressPhotos by settingsVm.progressPhotos.collectAsStateWithLifecycle()
                                        val cycleLogs by settingsVm.cycleLogs.collectAsStateWithLifecycle()
                                        InsightsScreen(
                                            todayState = todayState,
                                            workoutState = workoutState,
                                            progressPhotos = progressPhotos,
                                            cycleLogs = cycleLogs,
                                            onToggleCycleTracking = { enabled ->
                                                settingsVm.updateProfile(trackCycle = enabled)
                                            },
                                            onAddProgressPhoto = settingsVm::addProgressPhoto,
                                            onDeleteProgressPhoto = settingsVm::deleteProgressPhoto,
                                            onLogCycle = settingsVm::logCycle,
                                            onDeleteCycleLog = settingsVm::deleteCycleLog
                                        )
                                    }

                                    KinetixRoute.Settings -> {
                                        BackHandler(onBack = navigateBack)
                                        val profileState by settingsVm.profileState.collectAsStateWithLifecycle()
                                        SettingsScreen(
                                            profile = profileState,
                                            onSaveAvatarBitmap = settingsVm::saveProfileAvatarBitmap,
                                            onUpdateProfile = settingsVm::updateProfile,
                                            onOpenExactAlarmSettings = {
                                                context.startActivity(container.waterAlarmScheduler.buildExactAlarmPermissionIntent())
                                            },
                                            onOpenNotificationListenerSettings = {
                                                context.startActivity(container.mediaControlRepository.buildNotificationListenerSettingsIntent())
                                            },
                                            canScheduleExactAlarms = canExactAlarm,
                                            hasNotificationListener = hasMediaListener
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!useRail) {
                    AppleFloatingTabBar(
                        currentRoute = currentRoute,
                        onSelect = navigateTab,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = KinetixTokens.Spacing.NavBarMargin, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleFloatingTabBar(
    currentRoute: KinetixRoute,
    onSelect: (KinetixRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        shape = RoundedCornerShape(28.dp),
        color = KinetixTokens.Colors.BlurredChrome,
        border = BorderStroke(0.5.dp, KinetixTokens.Colors.Hairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(KinetixTokens.Spacing.NavBarHeight)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NAV_ITEMS.forEach { item ->
                val selected = currentRoute == item.route
                val interactionSource = remember { MutableInteractionSource() }
                val tint = if (selected) item.activeAccent else KinetixTokens.Colors.TextSecondary

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .applePressScale(interactionSource = interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelect(item.route) }
                        )
                        .testTag(item.tag),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = tint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
