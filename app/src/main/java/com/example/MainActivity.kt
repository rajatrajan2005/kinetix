package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.di.AppContainer
import com.example.ui.components.KinetixAmbientBackdrop
import com.example.ui.navigation.KinetixRoute
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.NutritionScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WorkoutScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.FloatingDockSurface
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.GlassRimHighlight
import com.example.ui.theme.GlassRimShadow
import com.example.ui.theme.GlassTopSheen
import com.example.ui.theme.KinetixTheme
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltContainer
import com.example.ui.theme.WarmAmber
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
    val tag: String
)

private val NAV_ITEMS = listOf(
    NavItem(KinetixRoute.Today, "Today", Icons.Filled.Home, Icons.Outlined.Home, "nav_today"),
    NavItem(KinetixRoute.Workout, "Workout", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter, "nav_workout"),
    NavItem(KinetixRoute.Nutrition, "Nutrition", Icons.Filled.Restaurant, Icons.Outlined.Restaurant, "nav_nutrition"),
    NavItem(KinetixRoute.Insights, "Insights", Icons.Filled.Insights, Icons.Outlined.Insights, "nav_insights"),
    NavItem(KinetixRoute.Settings, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_settings")
)

private val SlideSpec = spring(
    dampingRatio = 0.90f,
    stiffness = 460f,
    visibilityThreshold = IntOffset.VisibilityThreshold
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
    val container = AppContainer.get(context)

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

    // Collect ONLY onboarding status at root so timer ticks, sensor ticks, and profile edits
    // never force root/AnimatedContent/FloatingGlassTaskbar recomposition.
    val onboardingDone by remember(settingsVm) {
        settingsVm.profileState
            .map { it.hasCompletedOnboarding != 0 }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    if (!onboardingDone) {
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

    var currentRoute by rememberSaveable(stateSaver = KinetixRouteSaver) {
        mutableStateOf<KinetixRoute>(KinetixRoute.Today)
    }
    val tabBackStack = remember { mutableStateListOf<KinetixRoute>() }
    val saveableStateHolder = rememberSaveableStateHolder()

    val navigateTab: (KinetixRoute) -> Unit = remember {
        { target ->
            if (target != currentRoute) {
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

    KinetixAmbientBackdrop(
        primaryOrb = ElectricVolt,
        secondaryOrb = GlacialCyan,
        tertiaryOrb = WarmAmber
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
                        NavigationRail(containerColor = ObsidianSurface) {
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

                    // Pure GPU-accelerated AnimatedContent horizontal pager slide with zero alpha saveLayers
                    AnimatedContent(
                        targetState = currentRoute,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clipToBounds(),
                        transitionSpec = {
                            val fromIdx = tabIndexFor(initialState)
                            val toIdx = tabIndexFor(targetState)
                            val dir = if (toIdx >= fromIdx) 1 else -1
                            val enter = slideInHorizontally(
                                animationSpec = SlideSpec,
                                initialOffsetX = { fullWidth -> dir * fullWidth }
                            )
                            val exit = slideOutHorizontally(
                                animationSpec = SlideSpec,
                                targetOffsetX = { fullWidth -> -dir * fullWidth }
                            )
                            (enter togetherWith exit).apply {
                                targetContentZIndex = if (toIdx >= fromIdx) 1f else 0f
                            }.using(SizeTransform(clip = false))
                        },
                        contentKey = { route -> tabIndexFor(route) },
                        label = "kinetix_tab_slide"
                    ) { route ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { }
                        ) {
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
                                            onUpdateSplitDay = workoutVm::updateSplitDay,
                                            onMoveSplitExercise = workoutVm::moveSplitExercise,
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
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Transparent frosted-glass floating taskbar inspired by iOS / Apple Fitness
                if (!useRail) {
                    FloatingGlassTaskbar(
                        currentRoute = currentRoute,
                        onSelect = navigateTab,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingGlassTaskbar(
    currentRoute: KinetixRoute,
    onSelect: (KinetixRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)
    val rimBrush = remember {
        Brush.linearGradient(listOf(GlassRimHighlight, GlassRimShadow))
    }
    val glassGradient = remember {
        Brush.verticalGradient(
            listOf(GlassTopSheen, Color.Transparent)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        shape = shape,
        color = FloatingDockSurface,
        border = BorderStroke(1.dp, rimBrush)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(glassGradient)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NAV_ITEMS.forEach { item ->
                val selected = currentRoute == item.route
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (selected) ElectricVolt.copy(alpha = 0.16f) else Color.Transparent
                        )
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
                            tint = if (selected) ElectricVolt else TextSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) ElectricVolt else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
