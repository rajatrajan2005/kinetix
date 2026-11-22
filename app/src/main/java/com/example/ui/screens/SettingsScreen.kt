package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.components.AppleCollapsedNavBar
import com.example.ui.components.AppleCustomSlider
import com.example.ui.components.AppleGroupedSection
import com.example.ui.components.AppleLargeTitleHeader
import com.example.ui.components.AppleRowDivider
import com.example.ui.components.AppleSavedToast
import com.example.ui.components.AvatarAdjustDialog
import com.example.ui.components.BottomSheet
import com.example.ui.components.Card
import com.example.ui.components.ListRow
import com.example.ui.components.LocalPhotoImage
import com.example.ui.components.SegmentedControl
import com.example.ui.components.applePressScale
import com.example.ui.components.formatPossessiveProfileTitle
import com.example.ui.theme.KinetixTokens
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
    onOpenNotificationListenerSettings: () -> Unit,
    canScheduleExactAlarms: Boolean = true,
    hasNotificationListener: Boolean = false
) {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val navInsetBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomClearance = KinetixTokens.Spacing.NavBarClearance + navInsetBottom

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

    var savedToastCounter by remember { mutableIntStateOf(0) }
    var showSavedToast by remember { mutableStateOf(false) }

    var showActivitySheet by remember { mutableStateOf(false) }
    var showSportSheet by remember { mutableStateOf(false) }
    var showNutritionGoalSheet by remember { mutableStateOf(false) }

    var cropSourceUri by remember { mutableStateOf<Uri?>(null) }
    var cropExistingPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(savedToastCounter) {
        if (savedToastCounter > 0) {
            showSavedToast = true
            delay(1_600)
            showSavedToast = false
        }
    }

    fun triggerSavedToast() {
        savedToastCounter += 1
    }

    // Auto-save name changes smoothly after a brief typing pause
    LaunchedEffect(nameInput) {
        val trimmed = nameInput.trim().take(40)
        if (trimmed.isNotBlank() && trimmed != profile.displayName.trim()) {
            delay(500)
            onUpdateProfile(
                trimmed,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
            )
            triggerSavedToast()
        }
    }

    // Auto-save numeric biometrics smoothly when valid values change
    LaunchedEffect(ageInput, heightInput, weightInput) {
        val parsedAge = ageInput.trim().toIntOrNull()?.takeIf { it in 13..100 }
        val parsedHeight = heightInput.trim().toDoubleOrNull()?.takeIf { it in 110.0..240.0 }
        val parsedWeight = weightInput.trim().toDoubleOrNull()?.takeIf { it in 30.0..280.0 }
        val ageChanged = parsedAge != null && parsedAge != profile.age
        val heightChanged = parsedHeight != null && parsedHeight != profile.heightCm
        val weightChanged = parsedWeight != null && parsedWeight != profile.weightKg
        if (ageChanged || heightChanged || weightChanged) {
            delay(550)
            onUpdateProfile(
                null,
                if (ageChanged) parsedAge else null,
                null,
                if (heightChanged) parsedHeight else null,
                if (weightChanged) parsedWeight else null,
                null, null, null, null, null, null, null, null, null, null, null, null, null
            )
            triggerSavedToast()
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

    val profileTitle = remember(profile.displayName) {
        formatPossessiveProfileTitle(profile.displayName)
    }
    val cleanTitleName = remember(profile.displayName) {
        val raw = profile.displayName.trim().ifBlank { "Athlete" }
        raw.split(Regex("\\s+")).joinToString(" ") { w ->
            w.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
        }
    }
    val initialChar = cleanTitleName.firstOrNull()?.uppercaseChar()?.toString() ?: "A"

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "settings_header") {
                AppleLargeTitleHeader(
                    title = profileTitle,
                    subtitle = "Profile & Preferences",
                    modifier = Modifier.testTag("profile_title_heading")
                )
            }

            // 1. Identity & Avatar Inset Section (Auto-saved)
            item(key = "identity_avatar_card") {
                AppleGroupedSection(header = "ATHLETE") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .applePressScale()
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
                                        .background(KinetixTokens.Colors.NestedSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initialChar,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = KinetixTokens.Colors.TextPrimary
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cleanTitleName,
                                style = MaterialTheme.typography.titleLarge,
                                color = KinetixTokens.Colors.TextPrimary
                            )
                            Text(
                                text = "${currentTGoal.label} · ${profile.weightKg} kg",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KinetixTokens.Colors.TextSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        avatarPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = KinetixTokens.Radius.PillShape,
                                    color = KinetixTokens.Colors.NestedSurface,
                                    modifier = Modifier
                                        .height(36.dp)
                                        .applePressScale()
                                        .testTag("pick_profile_photo_btn")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CameraAlt,
                                            contentDescription = null,
                                            tint = KinetixTokens.Colors.TextPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (profile.profilePhotoPath.isBlank()) "Add Photo" else "Change",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = KinetixTokens.Colors.TextPrimary
                                        )
                                    }
                                }
                                if (profile.profilePhotoPath.isNotBlank()) {
                                    Surface(
                                        onClick = {
                                            cropSourceUri = null
                                            cropExistingPath = profile.profilePhotoPath
                                        },
                                        shape = KinetixTokens.Radius.PillShape,
                                        color = KinetixTokens.Colors.NestedSurface,
                                        modifier = Modifier
                                            .height(36.dp)
                                            .applePressScale()
                                            .testTag("adjust_profile_photo_btn")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Tune,
                                                contentDescription = null,
                                                tint = KinetixTokens.Colors.TextPrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Adjust",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = KinetixTokens.Colors.TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    AppleRowDivider()
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it.take(40) },
                            label = { Text("Your Name") },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val cleaned = nameInput.trim().take(40).ifBlank { "Athlete" }
                                    nameInput = cleaned
                                    onUpdateProfile(
                                        cleaned,
                                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
                                    )
                                    focusManager.clearFocus()
                                    triggerSavedToast()
                                }
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input")
                        )
                    }
                }
            }

            // 2. Body & Activity Inset Section
            item(key = "biometrics") {
                AppleGroupedSection(
                    header = "BODY & ACTIVITY",
                    footer = "Changes auto-save and immediately update BMR, TDEE, and hydration targets."
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        SegmentedControl(
                            options = Sex.entries,
                            selected = currentSex,
                            labelFor = { it.label },
                            onSelect = { sex ->
                                onUpdateProfile(
                                    null, null, sex, null, null, null, null, null,
                                    null, null, null, null, null, null,
                                    if (sex == Sex.MALE) false else null,
                                    null, null, null
                                )
                                triggerSavedToast()
                            },
                            tagPrefix = "profile_sex"
                        )
                    }
                    AppleRowDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AppleRowDivider()
                    ListRow(
                        title = "Activity Level",
                        subtitle = currentActivity.subtitle,
                        trailingText = currentActivity.label,
                        showChevron = true,
                        onClick = { showActivitySheet = true }
                    )
                }
            }

            // 3. Women's Cycle Tracker Settings (shown only for Female profile)
            if (currentSex == Sex.FEMALE) {
                item(key = "cycle_settings") {
                    AppleGroupedSection(header = "PERIOD CYCLE") {
                        ListRow(
                            title = "Women's Period Cycle",
                            subtitle = "Cycle tracking & Insights predictions",
                            leadingIcon = Icons.Rounded.Favorite,
                            leadingTint = KinetixTokens.Colors.Cycle,
                            leadingBg = KinetixTokens.Colors.CycleMutedBg,
                            trailingContent = {
                                Switch(
                                    checked = profile.trackCycle == 1,
                                    onCheckedChange = { enabled ->
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onUpdateProfile(
                                            null, null, null, null, null, null, null, null,
                                            null, null, null, null, null, null, enabled, null, null, null
                                        )
                                        triggerSavedToast()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = KinetixTokens.Colors.TextPrimary,
                                        checkedTrackColor = KinetixTokens.Colors.Cycle
                                    ),
                                    modifier = Modifier.testTag("settings_cycle_switch")
                                )
                            }
                        )
                        if (profile.trackCycle == 1) {
                            AppleRowDivider()
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                AppleCustomSlider(
                                    title = "Average Cycle Length",
                                    valueText = "${profile.cycleLengthDays} days",
                                    value = profile.cycleLengthDays.toFloat(),
                                    valueRange = 21f..35f,
                                    stepSize = 1f,
                                    accentColor = KinetixTokens.Colors.Cycle,
                                    onValueChange = {
                                        onUpdateProfile(
                                            null, null, null, null, null, null, null, null,
                                            null, null, null, null, null, null, null, it.roundToInt(), null, null
                                        )
                                    }
                                )
                                AppleCustomSlider(
                                    title = "Typical Period Duration",
                                    valueText = "${profile.periodLengthDays} days",
                                    value = profile.periodLengthDays.toFloat(),
                                    valueRange = 2f..8f,
                                    stepSize = 1f,
                                    accentColor = KinetixTokens.Colors.Cycle,
                                    onValueChange = {
                                        onUpdateProfile(
                                            null, null, null, null, null, null, null, null,
                                            null, null, null, null, null, null, null, null, it.roundToInt(), null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Primary Fitness Goal: 1 Summary Card + Single-Choice List
            item(key = "training_goal") {
                val activeGoalTargets = TrainingTuningCalculator.recommendedTargetsFor(currentTGoal)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PRIMARY FITNESS GOAL",
                        style = MaterialTheme.typography.labelMedium,
                        color = KinetixTokens.Colors.TextSecondary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    Card {
                        Text(
                            text = currentTGoal.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = KinetixTokens.Colors.TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${macroPreview.targetKcal} kcal · ${macroPreview.proteinG}g P · ${macroPreview.carbsG}g C · ${(profile.weightKg * profile.waterMlPerKg).roundToInt()}ml · ${profile.stepGoal} steps",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = KinetixTokens.Colors.Workout
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = activeGoalTargets.summaryNote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = KinetixTokens.Colors.TextSecondary
                        )
                    }
                    AppleGroupedSection {
                        TrainingGoal.entries.forEachIndexed { idx, goal ->
                            if (idx > 0) AppleRowDivider()
                            ListRow(
                                title = goal.label,
                                subtitle = goal.subtitle,
                                selectedCheckmark = currentTGoal == goal,
                                checkmarkTint = KinetixTokens.Colors.Workout,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onUpdateProfile(
                                        null, null, null, null, null, null, null, goal,
                                        null, null, null, null, null, null, null, null, null, null
                                    )
                                    triggerSavedToast()
                                },
                                modifier = Modifier.testTag("settings_training_goal_${goal.name}")
                            )
                        }
                    }
                }
            }

            // 5. Sport Conditioning Focus (Single-choice BottomSheet)
            item(key = "sport_discipline") {
                AppleGroupedSection(header = "SPORT CONDITIONING") {
                    ListRow(
                        title = "Primary Sport",
                        subtitle = currentSport.subtitle,
                        leadingIcon = Icons.Rounded.DirectionsRun,
                        leadingTint = KinetixTokens.Colors.Hydration,
                        leadingBg = KinetixTokens.Colors.HydrationMutedBg,
                        trailingText = currentSport.label,
                        showChevron = true,
                        onClick = { showSportSheet = true }
                    )
                }
            }

            // 6. Macro, Water & Step Sliders (Custom iOS sliders with white thumb + haptic ticks)
            item(key = "macro_hydration_tuning") {
                AppleGroupedSection(header = "NUTRITION, HYDRATION & STEPS") {
                    ListRow(
                        title = "Calorie Target Mode",
                        subtitle = "${macroPreview.targetKcal} kcal · ${macroPreview.carbsG}g C · ${macroPreview.fatG}g F",
                        trailingText = currentNGoal.label,
                        showChevron = true,
                        onClick = { showNutritionGoalSheet = true }
                    )
                    AppleRowDivider()
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        if (currentNGoal == NutritionGoal.CUT) {
                            AppleCustomSlider(
                                title = "Cut Deficit",
                                valueText = "${(profile.cutDeficitPct * 100).roundToInt()}%",
                                value = profile.cutDeficitPct.toFloat().coerceIn(0.15f, 0.20f),
                                valueRange = 0.15f..0.20f,
                                stepSize = 0.005f,
                                accentColor = KinetixTokens.Colors.Calories,
                                onValueChange = {
                                    onUpdateProfile(
                                        null, null, null, null, null, null, null, null,
                                        it.toDouble(), null, null, null, null, null, null, null, null, null
                                    )
                                }
                            )
                        }
                        AppleCustomSlider(
                            title = "Protein Target",
                            valueText = "${"%.1f".format(profile.proteinPerKg)} g/kg (${macroPreview.proteinG}g)",
                            value = profile.proteinPerKg.toFloat().coerceIn(1.6f, 2.2f),
                            valueRange = 1.6f..2.2f,
                            stepSize = 0.1f,
                            accentColor = KinetixTokens.Colors.Protein,
                            onValueChange = {
                                onUpdateProfile(
                                    null, null, null, null, null, null, null, null,
                                    null, it.toDouble(), null, null, null, null, null, null, null, null
                                )
                            }
                        )
                        AppleCustomSlider(
                            title = "Water Target",
                            valueText = "${profile.waterMlPerKg.roundToInt()} ml/kg",
                            value = profile.waterMlPerKg.toFloat().coerceIn(30f, 35f),
                            valueRange = 30f..35f,
                            stepSize = 1f,
                            accentColor = KinetixTokens.Colors.Hydration,
                            onValueChange = {
                                onUpdateProfile(
                                    null, null, null, null, null, null, null, null,
                                    null, null, null, null, it.toDouble(), null, null, null, null, null
                                )
                            }
                        )
                        AppleCustomSlider(
                            title = "Daily Step Goal",
                            valueText = "${profile.stepGoal}",
                            value = profile.stepGoal.toFloat().coerceIn(5000f, 15000f),
                            valueRange = 5000f..15000f,
                            stepSize = 500f,
                            accentColor = KinetixTokens.Colors.Hydration,
                            onValueChange = {
                                val roundedSteps = (it / 500f).roundToInt() * 500
                                onUpdateProfile(
                                    null, null, null, null, null, null, null, null,
                                    null, null, null, null, null, roundedSteps, null, null, null, null
                                )
                            }
                        )
                    }
                }
            }

            // 7. Plain iOS Permission List Rows
            item(key = "permissions_section") {
                AppleGroupedSection(header = "PERMISSIONS") {
                    ListRow(
                        title = "Exact Alarm Reminders",
                        subtitle = "Scheduled hydration alerts",
                        leadingIcon = Icons.Rounded.Alarm,
                        leadingTint = KinetixTokens.Colors.Hydration,
                        leadingBg = KinetixTokens.Colors.HydrationMutedBg,
                        trailingText = if (canScheduleExactAlarms) "Enabled" else "Off",
                        trailingColor = if (canScheduleExactAlarms) KinetixTokens.Colors.StatusInRangeFg else KinetixTokens.Colors.TextSecondary,
                        showChevron = true,
                        onClick = onOpenExactAlarmSettings
                    )
                    AppleRowDivider()
                    ListRow(
                        title = "Music Control Access",
                        subtitle = "Workout mini-player & headphones badge",
                        leadingIcon = Icons.Rounded.MusicNote,
                        leadingTint = KinetixTokens.Colors.Calories,
                        leadingBg = KinetixTokens.Colors.CaloriesMutedBg,
                        trailingText = if (hasNotificationListener) "Enabled" else "Off",
                        trailingColor = if (hasNotificationListener) KinetixTokens.Colors.StatusInRangeFg else KinetixTokens.Colors.TextSecondary,
                        showChevron = true,
                        onClick = onOpenNotificationListenerSettings
                    )
                }
            }
        }

        AppleCollapsedNavBar(
            title = profileTitle,
            listState = listState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        AppleSavedToast(
            visible = showSavedToast,
            message = "Saved",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        )
    }

    if (showActivitySheet) {
        BottomSheet(
            title = "Activity Level",
            subtitle = "Adjusts your TDEE multiplier",
            onDismiss = { showActivitySheet = false }
        ) {
            AppleGroupedSection {
                ActivityLevel.entries.forEachIndexed { idx, act ->
                    if (idx > 0) AppleRowDivider()
                    ListRow(
                        title = act.label,
                        subtitle = act.subtitle,
                        selectedCheckmark = currentActivity == act,
                        checkmarkTint = KinetixTokens.Colors.Workout,
                        onClick = {
                            onUpdateProfile(
                                null, null, null, null, null, act, null, null,
                                null, null, null, null, null, null, null, null, null, null
                            )
                            triggerSavedToast()
                            showActivitySheet = false
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showSportSheet) {
        BottomSheet(
            title = "Sport Conditioning",
            subtitle = "Adds tailored drills on Workout",
            largeDetent = true,
            onDismiss = { showSportSheet = false }
        ) {
            AppleGroupedSection {
                SportDiscipline.entries.forEachIndexed { idx, sport ->
                    if (idx > 0) AppleRowDivider()
                    ListRow(
                        title = sport.label,
                        subtitle = sport.subtitle,
                        selectedCheckmark = currentSport == sport,
                        checkmarkTint = KinetixTokens.Colors.Hydration,
                        onClick = {
                            onUpdateProfile(
                                null, null, null, null, null, null, null, null,
                                null, null, null, null, null, null, null, null, null, sport
                            )
                            triggerSavedToast()
                            showSportSheet = false
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showNutritionGoalSheet) {
        BottomSheet(
            title = "Calorie Target Mode",
            subtitle = "Fine-tunes deficit, maintenance, or surplus",
            onDismiss = { showNutritionGoalSheet = false }
        ) {
            AppleGroupedSection {
                NutritionGoal.entries.forEachIndexed { idx, goal ->
                    if (idx > 0) AppleRowDivider()
                    ListRow(
                        title = goal.label,
                        subtitle = goal.subtitle,
                        selectedCheckmark = currentNGoal == goal,
                        checkmarkTint = KinetixTokens.Colors.Calories,
                        onClick = {
                            onUpdateProfile(
                                null, null, null, null, null, null, goal, null,
                                null, null, null, null, null, null, null, null, null, null
                            )
                            triggerSavedToast()
                            showNutritionGoalSheet = false
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
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
                triggerSavedToast()
            },
            onDismiss = {
                cropSourceUri = null
                cropExistingPath = null
            }
        )
    }
}
