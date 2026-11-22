package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CycleLogEntity
import com.example.data.local.entity.ProgressPhotoEntity
import com.example.domain.calculator.CycleCalculator
import com.example.ui.components.AppleCollapsedNavBar
import com.example.ui.components.AppleLargeTitleHeader
import com.example.ui.components.BarEntry
import com.example.ui.components.BottomSheet
import com.example.ui.components.Card
import com.example.ui.components.EmptyState
import com.example.ui.components.InteractiveBarChart
import com.example.ui.components.InteractivePieChart
import com.example.ui.components.LocalPhotoImage
import com.example.ui.components.PieSliceData
import com.example.ui.components.PrimaryButton
import com.example.ui.components.StatChip
import com.example.ui.components.WeeklyHardSetsChart
import com.example.ui.components.applePressScale
import com.example.ui.theme.KinetixTokens
import com.example.ui.viewmodel.TodayUiState
import com.example.ui.viewmodel.WorkoutUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(
    todayState: TodayUiState,
    workoutState: WorkoutUiState,
    progressPhotos: List<ProgressPhotoEntity>,
    cycleLogs: List<CycleLogEntity>,
    onToggleCycleTracking: (Boolean) -> Unit,
    onAddProgressPhoto: (Uri, String, Double, String) -> Unit,
    onDeleteProgressPhoto: (ProgressPhotoEntity) -> Unit,
    onLogCycle: (Long, Long?, String, String, String) -> Unit,
    onDeleteCycleLog: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val navInsetBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomClearance = KinetixTokens.Spacing.NavBarClearance + navInsetBottom

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d · HH:mm") }
    val dayFmt = remember { DateTimeFormatter.ofPattern("MMM d") }
    val shortDayFmt = remember { DateTimeFormatter.ofPattern("MM/dd") }

    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var previewPhoto by remember { mutableStateOf<ProgressPhotoEntity?>(null) }
    var showLogPeriodSheet by remember { mutableStateOf(false) }

    val progressPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pendingPhotoUri = uri
    }

    val pieColors = remember {
        listOf(
            KinetixTokens.Colors.Workout,
            KinetixTokens.Colors.Calories,
            KinetixTokens.Colors.Hydration,
            KinetixTokens.Colors.Fat,
            KinetixTokens.Colors.StatusInRangeFg,
            KinetixTokens.Colors.Cycle
        )
    }

    val musclePieSlices = remember(workoutState.weeklyVolume) {
        workoutState.weeklyVolume
            .filter { it.hardSets > 0 }
            .mapIndexed { idx, vol ->
                PieSliceData(
                    label = vol.muscleGroup,
                    value = vol.hardSets.toFloat(),
                    valueText = "${vol.hardSets} sets",
                    color = pieColors[idx % pieColors.size]
                )
            }
    }

    val macroPieSlices = remember(
        todayState.consumedProteinG,
        todayState.consumedCarbsG,
        todayState.consumedFatG
    ) {
        val pKcal = (todayState.consumedProteinG * 4.0).toFloat()
        val cKcal = (todayState.consumedCarbsG * 4.0).toFloat()
        val fKcal = (todayState.consumedFatG * 9.0).toFloat()
        listOf(
            PieSliceData("Protein", pKcal, "${todayState.consumedProteinG.toInt()}g", KinetixTokens.Colors.Protein),
            PieSliceData("Carbs", cKcal, "${todayState.consumedCarbsG.toInt()}g", KinetixTokens.Colors.Carbs),
            PieSliceData("Fat", fKcal, "${todayState.consumedFatG.toInt()}g", KinetixTokens.Colors.Fat)
        )
    }

    val stepBarEntries = remember(todayState.recentSteps) {
        todayState.recentSteps.reversed().map { rec ->
            val label = LocalDate.ofEpochDay(rec.dateEpochDay).format(shortDayFmt)
            BarEntry(
                label = label,
                value = rec.stepsToday.toFloat(),
                valueText = "${rec.stepsToday} steps"
            )
        }
    }

    val cycleSummary = remember(cycleLogs, todayState.profile) {
        if (todayState.profile.trackCycle == 1) {
            CycleCalculator.summarize(
                logs = cycleLogs,
                cycleLengthDays = todayState.profile.cycleLengthDays,
                periodLengthDays = todayState.profile.periodLengthDays
            )
        } else null
    }

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
            item(key = "insights_header") {
                AppleLargeTitleHeader(
                    title = "Insights",
                    subtitle = "Analytics & Progress"
                )
            }

            // 1. Weekly Hard Sets per Muscle (10–20 optimal band highlighted)
            item(key = "weekly_muscle_volume") {
                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Weekly Hard Sets",
                                style = MaterialTheme.typography.titleLarge,
                                color = KinetixTokens.Colors.TextPrimary
                            )
                            Text(
                                text = "7-day muscle volume · 10–20 optimal band highlighted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KinetixTokens.Colors.TextSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    WeeklyHardSetsChart(volumes = workoutState.weeklyVolume)
                }
            }

            // 2. Daily Steps Trend
            item(key = "steps_history_chart") {
                Card {
                    Text(
                        text = "Daily Steps Trend",
                        style = MaterialTheme.typography.titleLarge,
                        color = KinetixTokens.Colors.TextPrimary
                    )
                    Spacer(Modifier.height(10.dp))
                    InteractiveBarChart(
                        entries = stepBarEntries,
                        goalValue = todayState.stepGoal.toFloat(),
                        emptyMessage = "Enable step counting to chart your daily step bars."
                    )
                }
            }

            // 3. Macro Split Donut
            item(key = "macro_split_pie") {
                Card {
                    Text(
                        text = "Macro Calorie Split",
                        style = MaterialTheme.typography.titleLarge,
                        color = KinetixTokens.Colors.TextPrimary
                    )
                    Text(
                        text = "Tap any slice or pill to inspect today's ratio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KinetixTokens.Colors.TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    InteractivePieChart(
                        slices = macroPieSlices,
                        emptyMessage = "Log a meal in Nutrition to view your macro split."
                    )
                }
            }

            // 4. Muscle Set Distribution Donut
            item(key = "muscle_volume_pie") {
                Card {
                    Text(
                        text = "Muscle Set Share (7 Days)",
                        style = MaterialTheme.typography.titleLarge,
                        color = KinetixTokens.Colors.TextPrimary
                    )
                    Text(
                        text = "Interactive distribution across muscle groups",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KinetixTokens.Colors.TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    InteractivePieChart(
                        slices = musclePieSlices,
                        emptyMessage = "Log workout sets to inspect your muscle distribution."
                    )
                }
            }

            // 5. Weekly Progress Photos
            item(key = "weekly_progress_photos") {
                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Progress Photos",
                                style = MaterialTheme.typography.titleLarge,
                                color = KinetixTokens.Colors.TextPrimary
                            )
                            Text(
                                text = "Private on-device weekly check-ins",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KinetixTokens.Colors.TextSecondary
                            )
                        }
                        Surface(
                            onClick = {
                                progressPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = KinetixTokens.Radius.PillShape,
                            color = KinetixTokens.Colors.HydrationMutedBg,
                            modifier = Modifier
                                .height(40.dp)
                                .applePressScale()
                                .testTag("add_progress_photo_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddAPhoto,
                                    contentDescription = null,
                                    tint = KinetixTokens.Colors.Hydration,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Add",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = KinetixTokens.Colors.Hydration
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    if (progressPhotos.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.PhotoLibrary,
                            message = "No progress photos yet.",
                            accentColor = KinetixTokens.Colors.Hydration,
                            actionLabel = "Add photo",
                            onAction = {
                                progressPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(progressPhotos, key = { it.id }) { photo ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = KinetixTokens.Colors.NestedSurface,
                                    border = BorderStroke(0.5.dp, KinetixTokens.Colors.Hairline),
                                    modifier = Modifier
                                        .width(136.dp)
                                        .applePressScale()
                                        .clickable { previewPhoto = photo }
                                ) {
                                    Column {
                                        LocalPhotoImage(
                                            filePath = photo.filePath,
                                            contentDescription = "${photo.weekLabel} progress photo",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(136.dp)
                                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(136.dp)
                                                    .background(KinetixTokens.Colors.NestedSurface),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Photo", color = KinetixTokens.Colors.TextSecondary)
                                            }
                                        }
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = photo.weekLabel,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = KinetixTokens.Colors.Hydration
                                            )
                                            Text(
                                                text = "${photo.weightKgSnapshot} kg · ${LocalDate.ofEpochDay(photo.dateEpochDay).format(dayFmt)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = KinetixTokens.Colors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Women's Period Cycle Tracker (shown only for Female profile)
            if (todayState.profile.sex == "FEMALE") {
                item(key = "cycle_tracker_card") {
                    Card {
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
                                    tint = KinetixTokens.Colors.Cycle
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Women's Cycle Tracker",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = KinetixTokens.Colors.TextPrimary
                                    )
                                    Text(
                                        text = if (todayState.profile.trackCycle == 1) {
                                            "Cycle ${todayState.profile.cycleLengthDays}d · Period ${todayState.profile.periodLengthDays}d"
                                        } else {
                                            "Enable to track cycle phases & predictions"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = KinetixTokens.Colors.TextSecondary
                                    )
                                }
                            }
                            Switch(
                                checked = todayState.profile.trackCycle == 1,
                                onCheckedChange = onToggleCycleTracking,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = KinetixTokens.Colors.TextPrimary,
                                    checkedTrackColor = KinetixTokens.Colors.Cycle
                                ),
                                modifier = Modifier.testTag("insights_cycle_switch")
                            )
                        }

                        if (todayState.profile.trackCycle == 1) {
                            Spacer(Modifier.height(12.dp))
                            if (cycleSummary != null) {
                                Surface(
                                    shape = KinetixTokens.Radius.NestedShape,
                                    color = KinetixTokens.Colors.CycleMutedBg,
                                    border = BorderStroke(0.5.dp, KinetixTokens.Colors.Cycle.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Day ${cycleSummary.cycleDay} · ${cycleSummary.phase.label}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = KinetixTokens.Colors.Cycle
                                            )
                                            Text(
                                                text = "Next ${cycleSummary.predictedNextDateLabel} (${cycleSummary.daysUntilNextPeriod}d)",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = KinetixTokens.Colors.TextPrimary
                                            )
                                        }
                                        Text(
                                            text = cycleSummary.phase.trainingNote,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = KinetixTokens.Colors.TextSecondary
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${cycleLogs.size} cycles logged",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = KinetixTokens.Colors.TextSecondary
                                )
                                PrimaryButton(
                                    text = "Log Period",
                                    onClick = { showLogPeriodSheet = true },
                                    containerColor = KinetixTokens.Colors.Cycle,
                                    contentColor = KinetixTokens.Colors.TextPrimary,
                                    modifier = Modifier.testTag("log_period_btn")
                                )
                            }
                            if (cycleLogs.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                cycleLogs.take(3).forEach { log ->
                                    val startStr = LocalDate.ofEpochDay(log.startDateEpochDay).format(dayFmt)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "$startStr · ${log.flowIntensity} flow",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = KinetixTokens.Colors.TextPrimary
                                            )
                                            if (log.symptomsCsv.isNotBlank()) {
                                                Text(
                                                    text = log.symptomsCsv,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = KinetixTokens.Colors.TextSecondary
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { onDeleteCycleLog(log.id) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Delete cycle log",
                                                tint = KinetixTokens.Colors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Completed Sessions
            item(key = "session_history_title") {
                Text(
                    text = "Completed Sessions",
                    style = MaterialTheme.typography.titleLarge,
                    color = KinetixTokens.Colors.TextPrimary
                )
            }

            if (workoutState.completedSessions.isEmpty()) {
                item(key = "no_sessions") {
                    Card {
                        EmptyState(
                            icon = Icons.Rounded.FitnessCenter,
                            message = "Finish a workout to log session history.",
                            accentColor = KinetixTokens.Colors.Workout
                        )
                    }
                }
            } else {
                items(workoutState.completedSessions, key = { it.id }) { session ->
                    val sets = workoutState.historySetsBySession[session.id].orEmpty()
                    val dateText = Instant.ofEpochMilli(session.startedAtEpochMs)
                        .atZone(ZoneId.systemDefault())
                        .format(dateFormatter)
                    Card(contentPadding = 14.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = session.splitDayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = KinetixTokens.Colors.TextPrimary
                            )
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.labelMedium,
                                color = KinetixTokens.Colors.TextSecondary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        StatChip(
                            text = "${sets.size} sets · ${session.trainingGoalSnapshot}",
                            foregroundColor = KinetixTokens.Colors.Workout,
                            backgroundColor = KinetixTokens.Colors.WorkoutMutedBg
                        )
                    }
                }
            }
        }

        AppleCollapsedNavBar(
            title = "Insights",
            listState = listState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    pendingPhotoUri?.let { uri ->
        val nextWeekNum = progressPhotos.size + 1
        var weekLabel by remember(uri) { mutableStateOf("Week $nextWeekNum") }
        var weightText by remember(uri) { mutableStateOf(todayState.profile.weightKg.toString()) }
        var notes by remember(uri) { mutableStateOf("") }

        BottomSheet(
            title = "Save Weekly Check-In",
            subtitle = "Private on-device snapshot",
            onDismiss = { pendingPhotoUri = null }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weekLabel,
                    onValueChange = { weekLabel = it },
                    label = { Text("Week label (e.g. Week 4)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (lighting, pump, energy)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton(
                    text = "Save Photo",
                    onClick = {
                        onAddProgressPhoto(
                            uri,
                            weekLabel,
                            weightText.toDoubleOrNull() ?: todayState.profile.weightKg,
                            notes
                        )
                        pendingPhotoUri = null
                    },
                    containerColor = KinetixTokens.Colors.Hydration,
                    contentColor = KinetixTokens.Colors.OnHydration,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    previewPhoto?.let { photo ->
        BottomSheet(
            title = "${photo.weekLabel} · ${photo.weightKgSnapshot} kg",
            onDismiss = { previewPhoto = null }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LocalPhotoImage(
                    filePath = photo.filePath,
                    contentDescription = photo.weekLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(KinetixTokens.Colors.NestedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Photo unavailable", color = KinetixTokens.Colors.TextSecondary)
                    }
                }
                if (photo.notes.isNotBlank()) {
                    Text(
                        text = photo.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KinetixTokens.Colors.TextSecondary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDeleteProgressPhoto(photo)
                            previewPhoto = null
                        },
                        shape = KinetixTokens.Radius.ControlShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Delete", color = KinetixTokens.Colors.StatusLowFg)
                    }
                    PrimaryButton(
                        text = "Done",
                        onClick = { previewPhoto = null },
                        containerColor = KinetixTokens.Colors.NestedSurface,
                        contentColor = KinetixTokens.Colors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showLogPeriodSheet) {
        var daysAgo by remember { mutableIntStateOf(0) }
        var flow by remember { mutableStateOf("Medium") }
        val selectedSymptoms = remember { mutableStateListOf<String>() }
        var notes by remember { mutableStateOf("") }

        BottomSheet(
            title = "Log Period Start",
            subtitle = "Track symptoms and cycle phase",
            largeDetent = true,
            onDismiss = { showLogPeriodSheet = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Start Date", style = MaterialTheme.typography.labelLarge, color = KinetixTokens.Colors.TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Today", 1 to "Yesterday", 2 to "2d ago", 3 to "3d ago").forEach { (d, label) ->
                        FilterChip(
                            selected = daysAgo == d,
                            onClick = { daysAgo = d },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KinetixTokens.Colors.CycleMutedBg,
                                selectedLabelColor = KinetixTokens.Colors.Cycle
                            )
                        )
                    }
                }
                Text("Flow Intensity", style = MaterialTheme.typography.labelLarge, color = KinetixTokens.Colors.TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Spotting", "Light", "Medium", "Heavy").forEach { option ->
                        FilterChip(
                            selected = flow == option,
                            onClick = { flow = option },
                            label = { Text(option) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KinetixTokens.Colors.CycleMutedBg,
                                selectedLabelColor = KinetixTokens.Colors.Cycle
                            )
                        )
                    }
                }
                Text("Symptoms", style = MaterialTheme.typography.labelLarge, color = KinetixTokens.Colors.TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cramps", "Fatigue", "Bloating", "Headache", "Mood").forEach { sym ->
                        val active = sym in selectedSymptoms
                        FilterChip(
                            selected = active,
                            onClick = {
                                if (active) selectedSymptoms.remove(sym) else selectedSymptoms.add(sym)
                            },
                            label = { Text(sym) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KinetixTokens.Colors.CycleMutedBg,
                                selectedLabelColor = KinetixTokens.Colors.Cycle
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton(
                    text = "Save Period Log",
                    onClick = {
                        val startDay = LocalDate.now().minusDays(daysAgo.toLong()).toEpochDay()
                        onLogCycle(startDay, null, flow, selectedSymptoms.joinToString(", "), notes)
                        showLogPeriodSheet = false
                    },
                    containerColor = KinetixTokens.Colors.Cycle,
                    contentColor = KinetixTokens.Colors.TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
