package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.calculator.WaterScheduleCalculator
import com.example.domain.model.SportDiscipline
import com.example.domain.model.TrainingGoal
import com.example.ui.components.AppleCollapsedNavBar
import com.example.ui.components.AppleFitnessRingsHero
import com.example.ui.components.AppleLargeTitleHeader
import com.example.ui.components.CalorieRingGauge
import com.example.ui.components.Card
import com.example.ui.components.LocalPhotoImage
import com.example.ui.components.MacroProgressRow
import com.example.ui.components.PrimaryButton
import com.example.ui.components.StatChip
import com.example.ui.components.applePressScale
import com.example.ui.theme.KinetixTokens
import com.example.ui.theme.rememberReduceAnimations
import com.example.ui.viewmodel.TodayUiState
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    state: TodayUiState,
    onLogWater: (Int) -> Unit,
    onMarkWakeUpNow: () -> Unit,
    onSetWakeTime: (Int, Int) -> Unit,
    onRequestExactAlarmSettings: () -> Unit,
    onStartStepListener: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onNavigateToWorkout: () -> Unit,
    onNavigateToNutrition: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val reduceMotion = rememberReduceAnimations()
    val listState = rememberLazyListState()
    val navInsetBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomClearance = KinetixTokens.Spacing.NavBarClearance + navInsetBottom

    var hasActivityPerm by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPerm by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val activityPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasActivityPerm = granted
        if (granted) onStartStepListener()
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPerm = granted
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshPermissions()
                if (hasActivityPerm) onStartStepListener()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showWakeOverrideDialog by remember { mutableStateOf(false) }
    var showCustomWaterDialog by remember { mutableStateOf(false) }

    val displayName = remember(state.profile.displayName) {
        val raw = state.profile.displayName.trim().ifBlank { "Athlete" }
        raw.split(Regex("\\s+")).joinToString(" ") { w ->
            w.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
        }
    }
    val initialChar = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
    val activeGoal = remember(state.profile.trainingGoal) {
        runCatching { TrainingGoal.valueOf(state.profile.trainingGoal) }.getOrDefault(TrainingGoal.HYPERTROPHY)
    }
    val activeSport = remember(state.profile.sportType) {
        runCatching { SportDiscipline.valueOf(state.profile.sportType) }.getOrDefault(SportDiscipline.NONE)
    }

    val moveProgress = if (state.macroTargets.targetKcal > 0) {
        state.consumedKcal.toFloat() / state.macroTargets.targetKcal.toFloat()
    } else 0f
    val proteinProgress = if (state.macroTargets.proteinG > 0) {
        (state.consumedProteinG / state.macroTargets.proteinG.toDouble()).toFloat()
    } else 0f
    val stepRatio = if (state.stepGoal > 0) {
        (state.stepsToday.toFloat() / state.stepGoal.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val waterRatio = if (state.waterTargetMl > 0) {
        (state.waterConsumedMl.toFloat() / state.waterTargetMl.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val standProgress = (stepRatio + waterRatio) / 2f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KinetixTokens.Colors.Background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(
                start = KinetixTokens.Spacing.ScreenHorizontal,
                top = 12.dp,
                end = KinetixTokens.Spacing.ScreenHorizontal,
                bottom = bottomClearance
            ),
            verticalArrangement = Arrangement.spacedBy(KinetixTokens.Spacing.CardGap)
        ) {
            item(key = "header") {
                AppleLargeTitleHeader(
                    title = "Today",
                    subtitle = "Hi, $displayName",
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .applePressScale()
                                .clickable(onClick = onNavigateToProfile),
                            contentAlignment = Alignment.Center
                        ) {
                            LocalPhotoImage(
                                filePath = state.profile.profilePhotoPath,
                                contentDescription = "$displayName profile photo",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(KinetixTokens.Colors.NestedSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initialChar,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = KinetixTokens.Colors.TextPrimary
                                    )
                                }
                            }
                        }
                        PrimaryButton(
                            text = if (state.hasActiveWorkout) "Resume" else "Train",
                            onClick = onNavigateToWorkout,
                            icon = Icons.Rounded.FitnessCenter,
                            modifier = Modifier.testTag("today_quick_workout_btn")
                        )
                    }
                )
            }

            item(key = "activity_rings_hero") {
                Card {
                    Text(
                        text = "Activity Rings",
                        style = MaterialTheme.typography.titleLarge,
                        color = KinetixTokens.Colors.TextPrimary
                    )
                    Spacer(Modifier.height(14.dp))
                    AppleFitnessRingsHero(
                        moveProgress = moveProgress,
                        moveValueLarge = "${state.consumedKcal}",
                        moveTargetLabel = "/ ${state.macroTargets.targetKcal} kcal",
                        exerciseProgress = proteinProgress,
                        exerciseValueLarge = "${state.consumedProteinG.toInt()}g",
                        exerciseTargetLabel = "/ ${state.macroTargets.proteinG}g",
                        standProgress = standProgress,
                        standValueLarge = "${state.waterConsumedMl} ml",
                        standTargetLabel = "· ${state.stepsToday} steps"
                    )
                }
            }

            if (state.profile.sex == "FEMALE" && state.profile.trackCycle == 1) {
                item(key = "cycle_summary_pill") {
                    val summary = state.cycleSummary
                    Card(onClick = onNavigateToInsights) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = KinetixTokens.Colors.Cycle,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (summary != null) {
                                        "Cycle Day ${summary.cycleDay} · ${summary.phase.label}"
                                    } else {
                                        "Period Cycle Tracker"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = KinetixTokens.Colors.TextPrimary
                                )
                            }
                            StatChip(
                                text = if (summary != null) "Next ${summary.predictedNextDateLabel}" else "Log period",
                                foregroundColor = KinetixTokens.Colors.Cycle,
                                backgroundColor = KinetixTokens.Colors.CycleMutedBg
                            )
                        }
                        if (summary != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = summary.phase.trainingNote,
                                style = MaterialTheme.typography.bodyMedium,
                                color = KinetixTokens.Colors.TextSecondary
                            )
                        }
                    }
                }
            }

            item(key = "calories_macros") {
                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Restaurant,
                                contentDescription = null,
                                tint = KinetixTokens.Colors.Calories,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Calories & Protein",
                                style = MaterialTheme.typography.titleLarge,
                                color = KinetixTokens.Colors.TextPrimary
                            )
                        }
                        TextButton(
                            onClick = onNavigateToNutrition,
                            modifier = Modifier.testTag("today_log_food_btn")
                        ) {
                            Text("Log Food", color = KinetixTokens.Colors.Calories)
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = KinetixTokens.Colors.Calories,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CalorieRingGauge(
                            consumedKcal = state.consumedKcal,
                            targetKcal = state.macroTargets.targetKcal,
                            size = 114.dp,
                            accentColor = KinetixTokens.Colors.Calories
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MacroProgressRow(
                                label = "Protein",
                                consumedG = state.consumedProteinG,
                                targetG = state.macroTargets.proteinG,
                                color = KinetixTokens.Colors.Protein
                            )
                            MacroProgressRow(
                                label = "Carbs",
                                consumedG = state.consumedCarbsG,
                                targetG = state.macroTargets.carbsG,
                                color = KinetixTokens.Colors.Carbs
                            )
                            MacroProgressRow(
                                label = "Fat",
                                consumedG = state.consumedFatG,
                                targetG = state.macroTargets.fatG,
                                color = KinetixTokens.Colors.Fat
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "BMR ${state.macroTargets.bmrKcal} · TDEE ${state.macroTargets.tdeeKcal} kcal",
                        style = MaterialTheme.typography.labelMedium,
                        color = KinetixTokens.Colors.TextSecondary
                    )
                }
            }

            item(key = "hydration") {
                val waterProgress by animateFloatAsState(
                    targetValue = waterRatio,
                    animationSpec = KinetixTokens.Motion.ringSpring(reduceMotion),
                    label = "waterProgress"
                )
                val animatedWaterMl by animateIntAsState(
                    targetValue = state.waterConsumedMl,
                    animationSpec = KinetixTokens.Motion.standardSpring(reduceMotion),
                    label = "waterMl"
                )
                val wakeMins = state.waterSchedule?.wakeMinutes ?: state.profile.wakeTimeMinutes
                val bedMins = state.waterSchedule?.bedMinutes ?: state.profile.bedTimeMinutes

                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = null,
                                tint = KinetixTokens.Colors.Hydration,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Hydration",
                                style = MaterialTheme.typography.titleLarge,
                                color = KinetixTokens.Colors.TextPrimary
                            )
                        }
                        Text(
                            text = "$animatedWaterMl / ${state.waterTargetMl} ml",
                            style = MaterialTheme.typography.labelLarge,
                            color = KinetixTokens.Colors.Hydration
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(KinetixTokens.Colors.Hydration.copy(alpha = 0.18f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(waterProgress.coerceAtLeast(if (state.waterConsumedMl > 0) 0.03f else 0f))
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(KinetixTokens.Colors.Hydration)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WaterQuickPill(
                            label = "1 Glass (250ml)",
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onLogWater(250)
                            },
                            modifier = Modifier.testTag("log_water_250_btn")
                        )
                        WaterQuickPill(
                            label = "Cup (150ml)",
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onLogWater(150)
                            },
                            modifier = Modifier.testTag("log_water_150_btn")
                        )
                        WaterQuickPill(
                            label = "Bottle (500ml)",
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onLogWater(500)
                            },
                            modifier = Modifier.testTag("log_water_500_btn")
                        )
                        Surface(
                            onClick = { showCustomWaterDialog = true },
                            shape = KinetixTokens.Radius.ControlShape,
                            color = KinetixTokens.Colors.NestedSurface,
                            border = BorderStroke(0.5.dp, KinetixTokens.Colors.Hairline),
                            modifier = Modifier
                                .height(48.dp)
                                .applePressScale()
                                .testTag("log_water_custom_btn")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = "+ Custom",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = KinetixTokens.Colors.TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val wakeStr = "%02d:%02d".format(wakeMins / 60, wakeMins % 60)
                        val cutoffMins = (bedMins - 150 + 1440) % 1440
                        val cutoffStr = "%02d:%02d".format(cutoffMins / 60, cutoffMins % 60)
                        Text(
                            text = "Window $wakeStr → $cutoffStr",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KinetixTokens.Colors.TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onMarkWakeUpNow,
                                modifier = Modifier.testTag("im_up_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WbSunny,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = KinetixTokens.Colors.Fat
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("I'm up", color = KinetixTokens.Colors.Fat)
                            }
                            TextButton(onClick = { showWakeOverrideDialog = true }) {
                                Text("Wake", color = KinetixTokens.Colors.Hydration)
                            }
                        }
                    }

                    if (state.upcomingWaterSlots.isNotEmpty()) {
                        val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
                        val slotPreview = state.upcomingWaterSlots.take(3).joinToString(" · ") { slot ->
                            val t = WaterScheduleCalculator.formatTime(slot.triggerEpochMs).format(formatter)
                            val tag = if (slot.isCatchUp) " catch-up" else ""
                            "$t (${slot.amountMl}ml$tag)"
                        }
                        Text(
                            text = "Next: $slotPreview",
                            style = MaterialTheme.typography.labelMedium,
                            color = KinetixTokens.Colors.TextSecondary
                        )
                    }

                    AnimatedVisibility(visible = !hasNotificationPerm || !state.canScheduleExactAlarms) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!hasNotificationPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                OutlinedButton(
                                    onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = KinetixTokens.Radius.ControlShape
                                ) {
                                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Allow Hydration Notifications")
                                }
                            }
                            if (!state.canScheduleExactAlarms) {
                                OutlinedButton(
                                    onClick = onRequestExactAlarmSettings,
                                    shape = KinetixTokens.Radius.ControlShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("exact_alarm_permission_btn")
                                ) {
                                    Icon(Icons.Rounded.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Grant Exact Alarm Permission")
                                }
                            }
                        }
                    }
                }
            }

            item(key = "steps") {
                val stepProgress by animateFloatAsState(
                    targetValue = stepRatio,
                    animationSpec = KinetixTokens.Motion.ringSpring(reduceMotion),
                    label = "stepProgress"
                )
                val animatedSteps by animateIntAsState(
                    targetValue = state.stepsToday.toInt(),
                    animationSpec = KinetixTokens.Motion.standardSpring(reduceMotion),
                    label = "stepCount"
                )
                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                                contentDescription = null,
                                tint = KinetixTokens.Colors.Hydration,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Steps",
                                style = MaterialTheme.typography.titleLarge,
                                color = KinetixTokens.Colors.TextPrimary
                            )
                        }
                        StatChip(
                            text = if (state.isStepSensorAvailable) "HW Batched" else "No HW Sensor",
                            foregroundColor = KinetixTokens.Colors.Hydration,
                            backgroundColor = KinetixTokens.Colors.HydrationMutedBg
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$animatedSteps",
                            style = MaterialTheme.typography.displaySmall,
                            color = KinetixTokens.Colors.TextPrimary
                        )
                        Text(
                            text = "Goal ${state.stepGoal}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KinetixTokens.Colors.TextSecondary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(KinetixTokens.Colors.Hydration.copy(alpha = 0.18f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(stepProgress.coerceAtLeast(if (state.stepsToday > 0) 0.03f else 0f))
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(KinetixTokens.Colors.Hydration)
                        )
                    }
                    if (!hasActivityPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Spacer(Modifier.height(12.dp))
                        PrimaryButton(
                            text = "Enable Step Counter",
                            onClick = { activityPermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                            containerColor = KinetixTokens.Colors.HydrationMutedBg,
                            contentColor = KinetixTokens.Colors.Hydration,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grant_step_permission_btn")
                        )
                    }
                }
            }

            item(key = "training_tuning") {
                Card(onClick = onNavigateToWorkout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = KinetixTokens.Colors.Workout,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                val sportSuffix = if (activeSport != SportDiscipline.NONE) " · ${activeSport.label}" else ""
                                Text(
                                    text = "${activeGoal.label}$sportSuffix",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = KinetixTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "${state.prescription.sets} sets × ${state.prescription.repMin}–${state.prescription.repMax} reps · ${state.prescription.restSeconds}s rest",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = KinetixTokens.Colors.Workout
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${state.splitDays.size} days",
                                style = MaterialTheme.typography.labelMedium,
                                color = KinetixTokens.Colors.TextSecondary
                            )
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = KinetixTokens.Colors.TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.prescription.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KinetixTokens.Colors.TextSecondary
                    )
                }
            }
        }

        AppleCollapsedNavBar(
            title = "Today",
            listState = listState,
            modifier = Modifier.align(Alignment.TopCenter),
            trailing = {
                TextButton(onClick = onNavigateToWorkout) {
                    Text(
                        text = if (state.hasActiveWorkout) "Resume" else "Train",
                        color = KinetixTokens.Colors.Workout,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )
    }

    if (showWakeOverrideDialog) {
        var hourInput by remember { mutableStateOf((state.profile.wakeTimeMinutes / 60).toString()) }
        var minInput by remember { mutableStateOf((state.profile.wakeTimeMinutes % 60).toString()) }
        AlertDialog(
            onDismissRequest = { showWakeOverrideDialog = false },
            containerColor = KinetixTokens.Colors.CardSurface,
            titleContentColor = KinetixTokens.Colors.TextPrimary,
            textContentColor = KinetixTokens.Colors.TextSecondary,
            title = { Text("Override Wake Time") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = hourInput,
                        onValueChange = { hourInput = it.filter(Char::isDigit).take(2) },
                        label = { Text("Hour (0–23)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minInput,
                        onValueChange = { minInput = it.filter(Char::isDigit).take(2) },
                        label = { Text("Minute") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val h = (hourInput.toIntOrNull() ?: 7).coerceIn(0, 23)
                        val m = (minInput.toIntOrNull() ?: 0).coerceIn(0, 59)
                        onSetWakeTime(h, m)
                        showWakeOverrideDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KinetixTokens.Colors.Hydration,
                        contentColor = KinetixTokens.Colors.OnHydration
                    )
                ) { Text("Update Schedule") }
            },
            dismissButton = {
                TextButton(onClick = { showWakeOverrideDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCustomWaterDialog) {
        var mlInput by remember { mutableStateOf("350") }
        AlertDialog(
            onDismissRequest = { showCustomWaterDialog = false },
            containerColor = KinetixTokens.Colors.CardSurface,
            titleContentColor = KinetixTokens.Colors.TextPrimary,
            textContentColor = KinetixTokens.Colors.TextSecondary,
            title = { Text("Custom Water Amount") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(200 to "Small", 330 to "Can", 750 to "Shaker", 1000 to "1 Liter").forEach { (ml, tag) ->
                            Surface(
                                onClick = { mlInput = ml.toString() },
                                shape = KinetixTokens.Radius.PillShape,
                                color = KinetixTokens.Colors.NestedSurface
                            ) {
                                Text(
                                    text = "$ml ml · $tag",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = KinetixTokens.Colors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = mlInput,
                        onValueChange = { mlInput = it.filter(Char::isDigit).take(4) },
                        label = { Text("Amount (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_water_ml_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ml = mlInput.toIntOrNull()?.coerceIn(25, 3000) ?: 250
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onLogWater(ml)
                        showCustomWaterDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KinetixTokens.Colors.Hydration,
                        contentColor = KinetixTokens.Colors.OnHydration
                    ),
                    modifier = Modifier.testTag("confirm_custom_water_btn")
                ) { Text("Add Water") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomWaterDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun WaterQuickPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = KinetixTokens.Radius.ControlShape,
        color = KinetixTokens.Colors.HydrationMutedBg,
        modifier = modifier
            .height(48.dp)
            .applePressScale()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = KinetixTokens.Colors.Hydration
            )
        }
    }
}
