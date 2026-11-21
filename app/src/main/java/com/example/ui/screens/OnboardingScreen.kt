package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.calculator.MacroCalculator
import com.example.domain.calculator.WaterScheduleCalculator
import com.example.domain.model.ActivityLevel
import com.example.domain.model.NutritionGoal
import com.example.domain.model.Sex
import com.example.domain.model.TrainingGoal
import com.example.ui.components.KinetixAmbientBackdrop
import com.example.ui.components.KinetixCard
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
        age: Int,
        sex: Sex,
        heightCm: Double,
        weightKg: Double,
        activityLevel: ActivityLevel,
        nutritionGoal: NutritionGoal,
        trainingGoal: TrainingGoal,
        wakeTime: LocalTime,
        bedTime: LocalTime
    ) -> Unit
) {
    var ageText by remember { mutableStateOf("27") }
    var heightText by remember { mutableStateOf("178") }
    var weightText by remember { mutableStateOf("76") }
    var sex by remember { mutableStateOf(Sex.MALE) }
    var activityLevel by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var nutritionGoal by remember { mutableStateOf(NutritionGoal.MAINTAIN) }
    var trainingGoal by remember { mutableStateOf(TrainingGoal.HYPERTROPHY) }
    var wakeHour by remember { mutableStateOf("7") }
    var bedHour by remember { mutableStateOf("23") }

    val age = ageText.toIntOrNull()?.coerceIn(14, 100) ?: 27
    val heightCm = heightText.toDoubleOrNull()?.coerceIn(120.0, 240.0) ?: 178.0
    val weightKg = weightText.toDoubleOrNull()?.coerceIn(35.0, 250.0) ?: 76.0

    val previewMacros = remember(age, sex, heightCm, weightKg, activityLevel, nutritionGoal) {
        MacroCalculator.calculate(age, sex, heightCm, weightKg, activityLevel, nutritionGoal)
    }
    val previewWater = remember(weightKg) { WaterScheduleCalculator.dailyTargetMl(weightKg) }

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
                text = "Calibrate your baseline",
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary
            )
            Text(
                text = "Calculates Mifflin-St Jeor macros, hydration intervals, and set/rep tuning. Changeable anytime in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(16.dp))
            KinetixCard(accentGlow = GlacialCyan) {
                Text("Biometrics", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sex.entries.forEach { option ->
                        FilterChip(
                            selected = sex == option,
                            onClick = { sex = option },
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
            }

            Spacer(Modifier.height(12.dp))
            KinetixCard(accentGlow = WarmAmber) {
                Text("Activity & Calorie Goal", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
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
            KinetixCard(accentGlow = ElectricVolt) {
                Text("Training Focus (Set/Rep Tuning)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(
                    text = "Only adjusts sets, reps & rest—your split stays untouched.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TrainingGoal.entries.forEach { goal ->
                        FilterChip(
                            selected = trainingGoal == goal,
                            onClick = { trainingGoal = goal },
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
                    text = "Preview: ${previewMacros.targetKcal} kcal · ${previewMacros.proteinG}g P · ${previewMacros.carbsG}g C · ${previewMacros.fatG}g F · $previewWater ml water",
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
                        age,
                        sex,
                        heightCm,
                        weightKg,
                        activityLevel,
                        nutritionGoal,
                        trainingGoal,
                        LocalTime.of(wH, 0),
                        LocalTime.of(bH, 0)
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
