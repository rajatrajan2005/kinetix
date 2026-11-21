package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.UserProfileEntity
import com.example.domain.model.ActivityLevel
import com.example.domain.model.NutritionGoal
import com.example.domain.model.Sex
import com.example.domain.model.TrainingGoal
import com.example.ui.components.KinetixAmbientBackdrop
import com.example.ui.components.KinetixCard
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltContainer
import com.example.ui.theme.WarmAmber
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    profile: UserProfileEntity,
    onUpdateProfile: (
        age: Int?,
        sex: Sex?,
        heightCm: Double?,
        weightKg: Double?,
        activityLevel: ActivityLevel?,
        nutritionGoal: NutritionGoal?,
        trainingGoal: TrainingGoal?,
        cutDeficitPct: Double?,
        proteinPerKg: Double?,
        wakeTimeMinutes: Int?,
        bedTimeMinutes: Int?,
        waterMlPerKg: Double?,
        stepGoal: Int?
    ) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit
) {
    val currentSex = runCatching { Sex.valueOf(profile.sex) }.getOrDefault(Sex.MALE)
    val currentActivity = runCatching { ActivityLevel.valueOf(profile.activityLevel) }
        .getOrDefault(ActivityLevel.MODERATE)
    val currentNGoal = runCatching { NutritionGoal.valueOf(profile.nutritionGoal) }
        .getOrDefault(NutritionGoal.MAINTAIN)
    val currentTGoal = runCatching { TrainingGoal.valueOf(profile.trainingGoal) }
        .getOrDefault(TrainingGoal.HYPERTROPHY)

    var weightInput by remember(profile.weightKg) { mutableStateOf(profile.weightKg.toString()) }
    var heightInput by remember(profile.heightCm) { mutableStateOf(profile.heightCm.toString()) }
    var ageInput by remember(profile.age) { mutableStateOf(profile.age.toString()) }

    KinetixAmbientBackdrop(
        primaryOrb = GlacialCyan,
        secondaryOrb = ElectricVolt,
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
            item(key = "settings_header") {
                Column {
                    Text("CONFIGURATION", style = MaterialTheme.typography.labelMedium, color = ElectricVolt)
                    Text("Profile & Goals", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                }
            }

            item(key = "training_goal") {
                KinetixCard(accentGlow = ElectricVolt) {
                    Text("Training Goal (Set/Rep Tuning)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text("Split stays untouched—only sets, reps, and rest adjust.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TrainingGoal.entries.forEach { goal ->
                            FilterChip(
                                selected = currentTGoal == goal,
                                onClick = {
                                    onUpdateProfile(null, null, null, null, null, null, goal, null, null, null, null, null, null)
                                },
                                label = { Text("${goal.label} · ${goal.subtitle}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VoltContainer,
                                    selectedLabelColor = ElectricVolt
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_training_goal_${goal.name}")
                            )
                        }
                    }
                }
            }

            item(key = "nutrition_goal") {
                KinetixCard(accentGlow = WarmAmber) {
                    Text("Calorie & Protein Targets", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutritionGoal.entries.forEach { goal ->
                            FilterChip(
                                selected = currentNGoal == goal,
                                onClick = {
                                    onUpdateProfile(null, null, null, null, null, goal, null, null, null, null, null, null, null)
                                },
                                label = { Text(goal.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VoltContainer,
                                    selectedLabelColor = ElectricVolt
                                )
                            )
                        }
                    }
                    if (currentNGoal == NutritionGoal.CUT) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Cut Deficit: ${(profile.cutDeficitPct * 100).roundToInt()}% below TDEE (15–20%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Slider(
                            value = profile.cutDeficitPct.toFloat(),
                            onValueChange = {
                                onUpdateProfile(null, null, null, null, null, null, null, it.toDouble(), null, null, null, null, null)
                            },
                            valueRange = 0.15f..0.20f,
                            colors = SliderDefaults.colors(thumbColor = ElectricVolt, activeTrackColor = ElectricVolt)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Protein: ${"%.1f".format(profile.proteinPerKg)} g/kg bodyweight (1.6–2.2)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Slider(
                        value = profile.proteinPerKg.toFloat(),
                        onValueChange = {
                            onUpdateProfile(null, null, null, null, null, null, null, null, it.toDouble(), null, null, null, null)
                        },
                        valueRange = 1.6f..2.2f,
                        colors = SliderDefaults.colors(thumbColor = ElectricVolt, activeTrackColor = ElectricVolt)
                    )
                }
            }

            item(key = "biometrics") {
                KinetixCard(accentGlow = GlacialCyan) {
                    Text("Biometrics & Activity", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Sex.entries.forEach { sex ->
                            FilterChip(
                                selected = currentSex == sex,
                                onClick = {
                                    onUpdateProfile(null, sex, null, null, null, null, null, null, null, null, null, null, null)
                                },
                                label = { Text(sex.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = {
                                ageInput = it
                                it.toIntOrNull()?.let { a ->
                                    onUpdateProfile(a, null, null, null, null, null, null, null, null, null, null, null, null)
                                }
                            },
                            label = { Text("Age") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = {
                                heightInput = it
                                it.toDoubleOrNull()?.let { h ->
                                    onUpdateProfile(null, null, h, null, null, null, null, null, null, null, null, null, null)
                                }
                            },
                            label = { Text("Height (cm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = {
                                weightInput = it
                                it.toDoubleOrNull()?.let { w ->
                                    onUpdateProfile(null, null, null, w, null, null, null, null, null, null, null, null, null)
                                }
                            },
                            label = { Text("Weight (kg)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActivityLevel.entries.forEach { act ->
                            FilterChip(
                                selected = currentActivity == act,
                                onClick = {
                                    onUpdateProfile(null, null, null, null, act, null, null, null, null, null, null, null, null)
                                },
                                label = { Text(act.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VoltContainer,
                                    selectedLabelColor = ElectricVolt
                                )
                            )
                        }
                    }
                }
            }

            item(key = "hydration_permissions") {
                KinetixCard(accentGlow = GlacialCyan) {
                    Text("Hydration & System Permissions", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Water Target: ${profile.waterMlPerKg.roundToInt()} ml/kg (30–35 ml/kg)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Slider(
                        value = profile.waterMlPerKg.toFloat(),
                        onValueChange = {
                            onUpdateProfile(null, null, null, null, null, null, null, null, null, null, null, it.toDouble(), null)
                        },
                        valueRange = 30f..35f,
                        colors = SliderDefaults.colors(thumbColor = GlacialCyan, activeTrackColor = GlacialCyan)
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onOpenExactAlarmSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Manage Exact Alarm Permission (Water)") }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onOpenNotificationListenerSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Manage Notification Listener (Music Control)") }
                }
            }
        }
        }
    }
}
