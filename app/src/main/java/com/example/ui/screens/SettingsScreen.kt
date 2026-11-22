package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.UserProfileEntity
import com.example.domain.calculator.MacroCalculator
import com.example.domain.calculator.TrainingTuningCalculator
import com.example.domain.model.ActivityLevel
import com.example.domain.model.NutritionGoal
import com.example.domain.model.Sex
import com.example.domain.model.SportDiscipline
import com.example.domain.model.TrainingGoal
import com.example.ui.components.AvatarAdjustDialog
import com.example.ui.components.KinetixCard
import com.example.ui.components.LocalPhotoImage
import com.example.ui.theme.CycleRose
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.OnElectricVolt
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltContainer
import com.example.ui.theme.WarmAmber
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    profile: UserProfileEntity,
    onSaveAvatarBitmap: (Bitmap) -> Unit,
    onUpdateProfile: (
        displayName: String?,
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
        stepGoal: Int?,
        trackCycle: Boolean?,
        cycleLengthDays: Int?,
        periodLengthDays: Int?,
        sportType: SportDiscipline?
    ) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val currentSex = runCatching { Sex.valueOf(profile.sex) }.getOrDefault(Sex.MALE)
    val currentActivity = runCatching { ActivityLevel.valueOf(profile.activityLevel) }
        .getOrDefault(ActivityLevel.MODERATE)
    val currentNGoal = runCatching { NutritionGoal.valueOf(profile.nutritionGoal) }
        .getOrDefault(NutritionGoal.MAINTAIN)
    val currentTGoal = runCatching { TrainingGoal.valueOf(profile.trainingGoal) }
        .getOrDefault(TrainingGoal.HYPERTROPHY)
    val currentSport = runCatching { SportDiscipline.valueOf(profile.sportType) }
        .getOrDefault(SportDiscipline.NONE)
    val macroPreview = remember(profile) {
        MacroCalculator.calculate(
            age = profile.age,
            sex = currentSex,
            heightCm = profile.heightCm,
            weightKg = profile.weightKg,
            activityLevel = currentActivity,
            nutritionGoal = currentNGoal,
            cutDeficitPct = profile.cutDeficitPct,
            proteinPerKg = profile.proteinPerKg
        )
    }

    var nameInput by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var weightInput by remember(profile.weightKg) { mutableStateOf(profile.weightKg.toString()) }
    var heightInput by remember(profile.heightCm) { mutableStateOf(profile.heightCm.toString()) }
    var ageInput by remember(profile.age) { mutableStateOf(profile.age.toString()) }

    var nameSavedToast by remember { mutableStateOf(false) }
    var biometricsSavedToast by remember { mutableStateOf(false) }

    var cropSourceUri by remember { mutableStateOf<Uri?>(null) }
    var cropExistingPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(nameSavedToast) {
        if (nameSavedToast) {
            delay(2_200)
            nameSavedToast = false
        }
    }
    LaunchedEffect(biometricsSavedToast) {
        if (biometricsSavedToast) {
            delay(2_200)
            biometricsSavedToast = false
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            cropExistingPath = null
            cropSourceUri = uri
        }
    }

    val cleanTitleName = profile.displayName.trim().ifBlank { "Athlete" }
    val initialChar = cleanTitleName.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
    val isNameDirty = nameInput.trim() != profile.displayName.trim()
    val isBiometricsDirty =
        ageInput.trim() != profile.age.toString() ||
            heightInput.trim() != profile.heightCm.toString() ||
            weightInput.trim() != profile.weightKg.toString()

    val saveNameAction = {
        val cleaned = nameInput.trim().take(40).ifBlank { "Athlete" }
        nameInput = cleaned
        onUpdateProfile(
            cleaned,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        )
        focusManager.clearFocus()
        nameSavedToast = true
    }

    val saveBiometricsAction = {
        val parsedAge = ageInput.trim().toIntOrNull()?.coerceIn(13, 100) ?: profile.age
        val parsedHeight = heightInput.trim().toDoubleOrNull()?.coerceIn(110.0, 240.0) ?: profile.heightCm
        val parsedWeight = weightInput.trim().toDoubleOrNull()?.coerceIn(30.0, 280.0) ?: profile.weightKg
        ageInput = parsedAge.toString()
        heightInput = parsedHeight.toString()
        weightInput = parsedWeight.toString()
        onUpdateProfile(
            null, parsedAge, null, parsedHeight, parsedWeight,
            null, null, null, null, null, null, null, null, null, null, null, null, null
        )
        focusManager.clearFocus()
        biometricsSavedToast = true
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "settings_header") {
                Column {
                    Text("PROFILE & SETTINGS", style = MaterialTheme.typography.labelMedium, color = ElectricVolt)
                    Text(
                        text = "$cleanTitleName's Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        modifier = Modifier.testTag("profile_title_heading")
                    )
                }
            }

            // Named Profile + Profile Picture Card
            item(key = "identity_avatar_card") {
                KinetixCard(accentGlow = ElectricVolt) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .clickable {
                                    avatarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LocalPhotoImage(
                                filePath = profile.profilePhotoPath,
                                contentDescription = "$cleanTitleName avatar",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(ElevatedCarbon),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initialChar,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = ElectricVolt
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cleanTitleName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Text(
                                text = "${currentTGoal.label} · ${profile.weightKg} kg",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        avatarPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.testTag("pick_profile_photo_btn")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (profile.profilePhotoPath.isBlank()) "Add Photo" else "Change Photo")
                                }
                                if (profile.profilePhotoPath.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            cropSourceUri = null
                                            cropExistingPath = profile.profilePhotoPath
                                        },
                                        modifier = Modifier.testTag("adjust_profile_photo_btn")
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Adjust")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it.take(40) },
                        label = { Text("Your Name") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { saveNameAction() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(visible = nameSavedToast) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(EmeraldContainer, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldPulse,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Saved", style = MaterialTheme.typography.labelMedium, color = EmeraldPulse)
                            }
                        }
                        if (!nameSavedToast) {
                            Spacer(Modifier.width(1.dp))
                        }
                        Button(
                            onClick = saveNameAction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNameDirty) ElectricVolt else VoltContainer,
                                contentColor = if (isNameDirty) OnElectricVolt else ElectricVolt
                            ),
                            modifier = Modifier.testTag("save_profile_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Profile")
                        }
                    }
                }
            }

            // Biometrics & Activity directly beneath Profile
            item(key = "biometrics") {
                KinetixCard(accentGlow = GlacialCyan) {
                    Text("Biometrics & Activity", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Sex.entries.forEach { sex ->
                            FilterChip(
                                selected = currentSex == sex,
                                onClick = {
                                    onUpdateProfile(
                                        null, null, sex, null, null, null, null, null,
                                        null, null, null, null, null, null,
                                        if (sex == Sex.MALE) false else null,
                                        null, null, null
                                    )
                                },
                                label = { Text(sex.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VoltContainer,
                                    selectedLabelColor = ElectricVolt
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it.filter(Char::isDigit).take(3) },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { saveBiometricsAction() }),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(visible = biometricsSavedToast) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(EmeraldContainer, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldPulse,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Biometrics saved", style = MaterialTheme.typography.labelMedium, color = EmeraldPulse)
                            }
                        }
                        if (!biometricsSavedToast) {
                            Spacer(Modifier.width(1.dp))
                        }
                        Button(
                            onClick = saveBiometricsAction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBiometricsDirty) ElectricVolt else VoltContainer,
                                contentColor = if (isBiometricsDirty) OnElectricVolt else ElectricVolt
                            ),
                            modifier = Modifier.testTag("save_biometrics_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Biometrics")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActivityLevel.entries.forEach { act ->
                            FilterChip(
                                selected = currentActivity == act,
                                onClick = {
                                    onUpdateProfile(null, null, null, null, null, act, null, null, null, null, null, null, null, null, null, null, null, null)
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

                // Women's Cycle Tracker Settings (shown only for Female profile)
                if (currentSex == Sex.FEMALE) {
                    item(key = "cycle_settings") {
                    KinetixCard(accentGlow = CycleRose) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = CycleRose)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Women's Period Cycle", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                    Text("Cycle tracking & Insights predictions", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                            Switch(
                                checked = profile.trackCycle == 1,
                                onCheckedChange = { enabled ->
                                    onUpdateProfile(null, null, null, null, null, null, null, null, null, null, null, null, null, null, enabled, null, null, null)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = CycleRose
                                ),
                                modifier = Modifier.testTag("settings_cycle_switch")
                            )
                        }
                        if (profile.trackCycle == 1) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Average Cycle Length: ${profile.cycleLengthDays} days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Slider(
                                value = profile.cycleLengthDays.toFloat(),
                                onValueChange = {
                                    onUpdateProfile(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, it.roundToInt(), null, null)
                                },
                                valueRange = 21f..35f,
                                colors = SliderDefaults.colors(thumbColor = CycleRose, activeTrackColor = CycleRose)
                            )
                            Text(
                                text = "Typical Period Duration: ${profile.periodLengthDays} days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Slider(
                                value = profile.periodLengthDays.toFloat(),
                                onValueChange = {
                                    onUpdateProfile(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, it.roundToInt(), null)
                                },
                                valueRange = 2f..8f,
                                colors = SliderDefaults.colors(thumbColor = CycleRose, activeTrackColor = CycleRose)
                            )
                        }
                    }
                    }
                }

                item(key = "training_goal") {
                    val activeGoalTargets = TrainingTuningCalculator.recommendedTargetsFor(currentTGoal)
                    KinetixCard(accentGlow = ElectricVolt) {
                        Text("Fitness & Training Goal", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(
                            text = "Switching goals auto-tunes calories, protein, carbs, water, steps & workout sets/reps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Synced: ${macroPreview.targetKcal} kcal · ${macroPreview.proteinG}g P · ${macroPreview.carbsG}g C · ${(profile.weightKg * profile.waterMlPerKg).roundToInt()}ml · ${profile.stepGoal} steps",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricVolt
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            TrainingGoal.entries.forEach { goal ->
                                FilterChip(
                                    selected = currentTGoal == goal,
                                    onClick = {
                                        onUpdateProfile(null, null, null, null, null, null, null, goal, null, null, null, null, null, null, null, null, null, null)
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
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = activeGoalTargets.summaryNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                item(key = "sport_discipline") {
                    KinetixCard(accentGlow = GlacialCyan) {
                        Text("Sport Conditioning Focus", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(
                            text = currentSport.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SportDiscipline.entries.forEach { sport ->
                                FilterChip(
                                    selected = currentSport == sport,
                                    onClick = {
                                        onUpdateProfile(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, sport)
                                    },
                                    label = { Text(sport.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VoltContainer,
                                        selectedLabelColor = ElectricVolt
                                    )
                                )
                            }
                        }
                    }
                }

                item(key = "nutrition_goal") {
                    KinetixCard(accentGlow = WarmAmber) {
                        Text("Calorie & Macro Fine-Tuning", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(
                            text = "Derived carbs auto-adjust: ${macroPreview.carbsG}g carbs · ${macroPreview.fatG}g fat @ ${macroPreview.targetKcal} kcal",
                            style = MaterialTheme.typography.labelMedium,
                            color = WarmAmber
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NutritionGoal.entries.forEach { goal ->
                                FilterChip(
                                    selected = currentNGoal == goal,
                                    onClick = {
                                        onUpdateProfile(null, null, null, null, null, null, goal, null, null, null, null, null, null, null, null, null, null, null)
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
                                    onUpdateProfile(null, null, null, null, null, null, null, null, it.toDouble(), null, null, null, null, null, null, null, null, null)
                                },
                                valueRange = 0.15f..0.20f,
                                colors = SliderDefaults.colors(thumbColor = ElectricVolt, activeTrackColor = ElectricVolt)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Protein: ${"%.1f".format(profile.proteinPerKg)} g/kg bodyweight (${macroPreview.proteinG}g/day)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Slider(
                            value = profile.proteinPerKg.toFloat(),
                            onValueChange = {
                                onUpdateProfile(null, null, null, null, null, null, null, null, null, it.toDouble(), null, null, null, null, null, null, null, null)
                            },
                            valueRange = 1.6f..2.2f,
                            colors = SliderDefaults.colors(thumbColor = ElectricVolt, activeTrackColor = ElectricVolt)
                        )
                    }
                }

                item(key = "hydration_permissions") {
                    KinetixCard(accentGlow = GlacialCyan) {
                        Text("Hydration, Steps & Permissions", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Water Target: ${profile.waterMlPerKg.roundToInt()} ml/kg (30–35 ml/kg)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Slider(
                            value = profile.waterMlPerKg.toFloat(),
                            onValueChange = {
                                onUpdateProfile(null, null, null, null, null, null, null, null, null, null, null, null, it.toDouble(), null, null, null, null, null)
                            },
                            valueRange = 30f..35f,
                            colors = SliderDefaults.colors(thumbColor = GlacialCyan, activeTrackColor = GlacialCyan)
                        )
                        Text(
                            text = "Daily Step Goal: ${profile.stepGoal} steps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Slider(
                            value = profile.stepGoal.toFloat().coerceIn(5000f, 15000f),
                            onValueChange = {
                                val roundedSteps = (it / 500f).roundToInt() * 500
                                onUpdateProfile(null, null, null, null, null, null, null, null, null, null, null, null, null, roundedSteps, null, null, null, null)
                            },
                            valueRange = 5000f..15000f,
                            colors = SliderDefaults.colors(thumbColor = EmeraldPulse, activeTrackColor = EmeraldPulse)
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

    if (cropSourceUri != null || !cropExistingPath.isNullOrBlank()) {
        AvatarAdjustDialog(
            sourceUri = cropSourceUri,
            existingFilePath = cropExistingPath,
            onSaveBitmap = { bitmap ->
                onSaveAvatarBitmap(bitmap)
                cropSourceUri = null
                cropExistingPath = null
            },
            onDismiss = {
                cropSourceUri = null
                cropExistingPath = null
            }
        )
    }
}
