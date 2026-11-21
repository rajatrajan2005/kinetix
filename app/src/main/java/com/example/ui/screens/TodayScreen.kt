package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.calculator.WaterScheduleCalculator
import com.example.ui.components.CalorieRingGauge
import com.example.ui.components.KinetixAmbientBackdrop
import com.example.ui.components.KinetixCard
import com.example.ui.components.MacroProgressRow
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.OnElectricVolt
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltContainer
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.TodayUiState
import java.time.format.DateTimeFormatter

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
    onNavigateToNutrition: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    KinetixAmbientBackdrop(
        primaryOrb = ElectricVolt,
        secondaryOrb = GlacialCyan,
        tertiaryOrb = WarmAmber
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KINETIX",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricVolt
                        )
                        Text(
                            text = "Today's Engine",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                    }
                    FilledTonalButton(
                        onClick = onNavigateToWorkout,
                        modifier = Modifier.testTag("today_quick_workout_btn")
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (state.hasActiveWorkout) "Resume" else "Train")
                    }
                }
            }

            item(key = "calories_macros") {
                KinetixCard(accentGlow = WarmAmber) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = WarmAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Calories & Protein",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        TextButton(
                            onClick = onNavigateToNutrition,
                            modifier = Modifier.testTag("today_log_food_btn")
                        ) {
                            Text("Log Food", color = WarmAmber)
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
                            accentColor = WarmAmber
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
                                color = ElectricVolt
                            )
                            MacroProgressRow(
                                label = "Carbs",
                                consumedG = state.consumedCarbsG,
                                targetG = state.macroTargets.carbsG,
                                color = GlacialCyan
                            )
                            MacroProgressRow(
                                label = "Fat",
                                consumedG = state.consumedFatG,
                                targetG = state.macroTargets.fatG,
                                color = WarmAmber
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "BMR ${state.macroTargets.bmrKcal} · TDEE ${state.macroTargets.tdeeKcal} kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            item(key = "hydration") {
                val waterProgress by animateFloatAsState(
                    targetValue = if (state.waterTargetMl > 0) {
                        (state.waterConsumedMl.toFloat() / state.waterTargetMl.toFloat()).coerceIn(0f, 1f)
                    } else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "waterProgress"
                )
                val animatedWaterMl by animateIntAsState(
                    targetValue = state.waterConsumedMl,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "waterMl"
                )
                val wakeMins = state.waterSchedule?.wakeMinutes ?: state.profile.wakeTimeMinutes
                val bedMins = state.waterSchedule?.bedMinutes ?: state.profile.bedTimeMinutes

                KinetixCard(accentGlow = GlacialCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = GlacialCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Hydration",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "$animatedWaterMl / ${state.waterTargetMl} ml",
                            style = MaterialTheme.typography.labelMedium,
                            color = GlacialCyan
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { waterProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = GlacialCyan,
                        trackColor = ElevatedCarbon
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onLogWater(250) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanContainer,
                                contentColor = GlacialCyan
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("log_water_250_btn")
                        ) {
                            Text("+250 ml")
                        }
                        Button(
                            onClick = { onLogWater(500) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanContainer,
                                contentColor = GlacialCyan
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("log_water_500_btn")
                        ) {
                            Text("+500 ml")
                        }
                        OutlinedButton(
                            onClick = onMarkWakeUpNow,
                            modifier = Modifier.testTag("im_up_btn")
                        ) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("I'm up")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val wakeStr = "%02d:%02d".format(wakeMins / 60, wakeMins % 60)
                        val cutoffMins = (bedMins - 150 + 1440) % 1440
                        val cutoffStr = "%02d:%02d".format(cutoffMins / 60, cutoffMins % 60)
                        Text(
                            text = "Window $wakeStr → $cutoffStr (2.5h pre-bed)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        TextButton(onClick = { showWakeOverrideDialog = true }) {
                            Text("Edit wake", color = GlacialCyan)
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
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }

                    AnimatedVisibility(visible = !hasNotificationPerm || !state.canScheduleExactAlarms) {
                        Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!hasNotificationPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                OutlinedButton(
                                    onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Allow Hydration Notifications")
                                }
                            }
                            if (!state.canScheduleExactAlarms) {
                                OutlinedButton(
                                    onClick = onRequestExactAlarmSettings,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("exact_alarm_permission_btn")
                                ) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
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
                    targetValue = if (state.stepGoal > 0) {
                        (state.stepsToday.toFloat() / state.stepGoal.toFloat()).coerceIn(0f, 1f)
                    } else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "stepProgress"
                )
                val animatedSteps by animateIntAsState(
                    targetValue = state.stepsToday.toInt(),
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "stepCount"
                )
                KinetixCard(accentGlow = EmeraldPulse) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = EmeraldPulse,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Steps", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        }
                        Text(
                            text = if (state.isStepSensorAvailable) "HW Batched" else "No HW Sensor",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPulse,
                            modifier = Modifier
                                .background(EmeraldContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$animatedSteps",
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Goal ${state.stepGoal}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { stepProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = EmeraldPulse,
                        trackColor = ElevatedCarbon
                    )
                    if (!hasActivityPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { activityPermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldContainer,
                                contentColor = EmeraldPulse
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grant_step_permission_btn")
                        ) {
                            Text("Enable Physical Activity Step Counter")
                        }
                    }
                }
            }

            item(key = "training_tuning") {
                KinetixCard(accentGlow = ElectricVolt, onClick = onNavigateToWorkout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = ElectricVolt)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Training Prescription", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                Text(
                                    text = "${state.prescription.sets} sets × ${state.prescription.repMin}–${state.prescription.repMax} reps · ${state.prescription.restSeconds}s rest",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricVolt
                                )
                            }
                        }
                        Text(
                            text = "${state.splitDays.size} days",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.prescription.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
        }
    }

    if (showWakeOverrideDialog) {
        var hourInput by remember { mutableStateOf((state.profile.wakeTimeMinutes / 60).toString()) }
        var minInput by remember { mutableStateOf((state.profile.wakeTimeMinutes % 60).toString()) }
        AlertDialog(
            onDismissRequest = { showWakeOverrideDialog = false },
            title = { Text("Override Wake Time") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = hourInput,
                        onValueChange = { hourInput = it.filter(Char::isDigit).take(2) },
                        label = { Text("Hour (0-23)") },
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
                        containerColor = ElectricVolt,
                        contentColor = OnElectricVolt
                    )
                ) { Text("Update Schedule") }
            },
            dismissButton = {
                TextButton(onClick = { showWakeOverrideDialog = false }) { Text("Cancel") }
            }
        )
    }
}
