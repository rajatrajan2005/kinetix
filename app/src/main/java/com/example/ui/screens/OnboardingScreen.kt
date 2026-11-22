package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.calculator.MacroCalculator
import com.example.domain.calculator.TrainingTuningCalculator
import com.example.domain.calculator.WaterScheduleCalculator
import com.example.domain.model.ActivityLevel
import com.example.domain.model.NutritionGoal
import com.example.domain.model.Sex
import com.example.domain.model.SportDiscipline
import com.example.domain.model.TrainingGoal
import com.example.ui.components.KinetixAmbientBackdrop
import com.example.ui.components.KinetixCard
import com.example.ui.theme.CycleRose
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.OnElectricVolt
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltContainer
import com.example.ui.theme.WarmAmber
import java.time.LocalTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onComplete: (
        displayName: String,
        age: Int,
        sex: Sex,
        heightCm: Double,
        weightKg: Double,
        activityLevel: ActivityLevel,
        nutritionGoal: NutritionGoal,
        trainingGoal: TrainingGoal,
        sportDiscipline: SportDiscipline,
        wakeTime: LocalTime,
        bedTime: LocalTime,
        trackCycle: Boolean
    ) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("27") }
    var heightText by remember { mutableStateOf("178") }
    var weightText by remember { mutableStateOf("76") }
    var sex by remember { mutableStateOf(Sex.MALE) }
    var trackCycle by remember { mutableStateOf(false) }
    var activityLevel by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var trainingGoal by remember { mutableStateOf(TrainingGoal.HYPERTROPHY) }
    var nutritionGoal by remember { mutableStateOf(TrainingTuningCalculator.recommendedTargetsFor(TrainingGoal.HYPERTROPHY).nutritionGoal) }
    var sportDiscipline by remember { mutableStateOf(SportDiscipline.NONE) }
    var wakeHour by remember { mutableStateOf("7") }
    var bedHour by remember { mutableStateOf("23") }

    val age = ageText.toIntOrNull()?.coerceIn(14, 100) ?: 27
    val heightCm = heightText.toDoubleOrNull()?.coerceIn(120.0, 240.0) ?: 178.0
    val weightKg = weightText.toDoubleOrNull()?.coerceIn(35.0, 250.0) ?: 76.0
    val alignedTargets = remember(trainingGoal) { TrainingTuningCalculator.recommendedTargetsFor(trainingGoal) }

    val previewMacros = remember(age, sex, heightCm, weightKg, activityLevel, nutritionGoal, alignedTargets) {
        MacroCalculator.calculate(
            age = age,
            sex = sex,
            heightCm = heightCm,
            weightKg = weightKg,
            activityLevel = activityLevel,
            nutritionGoal = nutritionGoal,
            proteinPerKg = alignedTargets.proteinPerKg
        )
    }
    val previewWater = remember(weightKg, alignedTargets) {
        WaterScheduleCalculator.dailyTargetMl(weightKg, alignedTargets.waterMlPerKg)
    }

    KinetixAmbientBackdrop(
        primaryOrb = ElectricVolt,
        secondaryOrb = GlacialCyan,
        tertiaryOrb = WarmAmber
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 600.dp)) {
                Text(
                    text = "KINETIX SETUP",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricVolt
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Build your profile",
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPrimary
                )
                Text(
                    text = "Personalizes your Activity Rings, goal-driven macros, hydration intervals, sport conditioning, and workout split.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(Modifier.height(16.dp))
                KinetixCard(accentGlow = ElectricVolt) {
                    Text("Your Name", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it.take(40) },
                        label = { Text("First or full name") },
                        placeholder = { Text("e.g. Ananya, Alex") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input")
                    )
                }

                Spacer(Modifier.height(12.dp))
                KinetixCard(accentGlow = GlacialCyan) {
                    Text("Biometrics", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Sex.entries.forEach { option ->
                            FilterChip(
                                selected = sex == option,
                                onClick = {
                                    sex = option
                                    trackCycle = (option == Sex.FEMALE)
                                },
                                label = { Text(option.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VoltContainer,
                                    selectedLabelColor = ElectricVolt
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter(Char::isDigit).take(3) },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it.take(5) },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it.take(5) },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AnimatedVisibility(visible = sex == Sex.FEMALE) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_cycle_row"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Women's Period Cycle Tracker", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("Cycle phase & recovery guidance", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                Switch(
                                    checked = trackCycle,
                                    onCheckedChange = { trackCycle = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TextPrimary,
                                        checkedTrackColor = CycleRose
                                    ),
                                    modifier = Modifier.testTag("onboarding_cycle_switch")
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                KinetixCard(accentGlow = ElectricVolt) {
                    Text("Primary Fitness Goal", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(
                        text = "Aligns sets/reps, calories, protein, carbs, water & daily steps together.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TrainingGoal.entries.forEach { goal ->
                            FilterChip(
                                selected = trainingGoal == goal,
                                onClick = {
                                    trainingGoal = goal
                                    nutritionGoal = TrainingTuningCalculator.recommendedTargetsFor(goal).nutritionGoal
                                },
                                label = { Text("${goal.label} · ${goal.subtitle}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VoltContainer,
                                    selectedLabelColor = ElectricVolt
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                KinetixCard(accentGlow = WarmAmber) {
                    Text("Activity & Calorie Mode", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActivityLevel.entries.forEach { level ->
                            FilterChip(
                                selected = activityLevel == level,
                                onClick = { activityLevel = level },
                                label = { Text(level.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VoltContainer,
                                    selectedLabelColor = ElectricVolt
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NutritionGoal.entries.forEach { goal ->
                            FilterChip(
                                selected = nutritionGoal == goal,
                                onClick = { nutritionGoal = goal },
                                label = { Text(goal.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VoltContainer,
                                    selectedLabelColor = ElectricVolt
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                KinetixCard(accentGlow = GlacialCyan) {
                    Text("Primary Sport (Optional)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(
                        text = "Adds tailored athletic conditioning exercises inside Workout.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SportDiscipline.entries.forEach { sport ->
                            FilterChip(
                                selected = sportDiscipline == sport,
                                onClick = { sportDiscipline = sport },
                                label = { Text(sport.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VoltContainer,
                                    selectedLabelColor = ElectricVolt
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                KinetixCard(accentGlow = GlacialCyan) {
                    Text("Sleep Window (Water Scheduling)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = wakeHour,
                            onValueChange = { wakeHour = it.filter(Char::isDigit).take(2) },
                            label = { Text("Wake hour (0–23)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bedHour,
                            onValueChange = { bedHour = it.filter(Char::isDigit).take(2) },
                            label = { Text("Bed hour (0–23)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Goal Targets: ${previewMacros.targetKcal} kcal · ${previewMacros.proteinG}g P · ${previewMacros.carbsG}g C · ${previewMacros.fatG}g F · $previewWater ml · ${alignedTargets.stepGoal} steps",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricVolt
                    )
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        val wH = (wakeHour.toIntOrNull() ?: 7).coerceIn(0, 23)
                        val bH = (bedHour.toIntOrNull() ?: 23).coerceIn(0, 23)
                        onComplete(
                            displayName,
                            age,
                            sex,
                            heightCm,
                            weightKg,
                            activityLevel,
                            nutritionGoal,
                            trainingGoal,
                            sportDiscipline,
                            LocalTime.of(wH, 0),
                            LocalTime.of(bH, 0),
                            trackCycle
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricVolt,
                        contentColor = OnElectricVolt
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("complete_onboarding_button")
                ) {
                    Text("Save & Launch Kinetix", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
