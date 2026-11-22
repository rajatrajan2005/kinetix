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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.domain.model.VolumeStatus
import com.example.ui.components.BarEntry
import com.example.ui.components.InteractiveBarChart
import com.example.ui.components.InteractivePieChart
import com.example.ui.components.KinetixCard
import com.example.ui.components.LocalPhotoImage
import com.example.ui.components.PieSliceData
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CycleContainer
import com.example.ui.theme.CycleRose
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ExerciseRingGreen
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.GlassRimHighlight
import com.example.ui.theme.MoveRingRed
import com.example.ui.theme.OnElectricVolt
import com.example.ui.theme.StandRingCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
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
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d · HH:mm") }
    val dayFmt = remember { DateTimeFormatter.ofPattern("MMM d") }
    val shortDayFmt = remember { DateTimeFormatter.ofPattern("MM/dd") }

    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var previewPhoto by remember { mutableStateOf<ProgressPhotoEntity?>(null) }
    var showLogPeriodDialog by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker for weekly progress photos
    val progressPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pendingPhotoUri = uri
    }

    val pieColors = remember {
        listOf(
            ElectricVolt,
            MoveRingRed,
            StandRingCyan,
            WarmAmber,
            EmeraldPulse,
            GlacialCyan
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
            PieSliceData("Protein", pKcal, "${todayState.consumedProteinG.toInt()}g", ExerciseRingGreen),
            PieSliceData("Carbs", cKcal, "${todayState.consumedCarbsG.toInt()}g", StandRingCyan),
            PieSliceData("Fat", fKcal, "${todayState.consumedFatG.toInt()}g", MoveRingRed)
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
                item(key = "insights_header") {
                    Column {
                        Text("ANALYTICS & PROGRESS", style = MaterialTheme.typography.labelMedium, color = ElectricVolt)
                        Text("Insights", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    }
                }

                // Weekly Progress Photos
                item(key = "weekly_progress_photos") {
                    KinetixCard(accentGlow = StandRingCyan) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Weekly Progress Photos", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                Text(
                                    text = "Stored privately on-device for visual physique tracking",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    progressPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("add_progress_photo_btn")
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add")
                            }
                        }

                        if (progressPhotos.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(progressPhotos, key = { it.id }) { photo ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = CarbonSurface,
                                        border = BorderStroke(1.dp, GlassRimHighlight.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clickable { previewPhoto = photo }
                                    ) {
                                        Column {
                                            LocalPhotoImage(
                                                filePath = photo.filePath,
                                                contentDescription = "${photo.weekLabel} progress photo",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(140.dp)
                                                        .background(ElevatedCarbon),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Photo", color = TextMuted)
                                                }
                                            }
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = photo.weekLabel,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = StandRingCyan
                                                )
                                                Text(
                                                    text = "${photo.weightKgSnapshot} kg · ${LocalDate.ofEpochDay(photo.dateEpochDay).format(dayFmt)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Women's Period Cycle Tracker (shown only for Female profile)
                if (todayState.profile.sex == "FEMALE") {
                    item(key = "cycle_tracker_card") {
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
                                    Text("Women's Cycle Tracker", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                    Text(
                                        text = if (todayState.profile.trackCycle == 1) {
                                            "Cycle ${todayState.profile.cycleLengthDays}d · Period ${todayState.profile.periodLengthDays}d"
                                        } else {
                                            "Enable to track cycle days, phases & predictions"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Switch(
                                checked = todayState.profile.trackCycle == 1,
                                onCheckedChange = onToggleCycleTracking,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = CycleRose
                                ),
                                modifier = Modifier.testTag("insights_cycle_switch")
                            )
                        }

                        if (todayState.profile.trackCycle == 1) {
                            Spacer(Modifier.height(10.dp))
                            if (cycleSummary != null) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = CycleContainer,
                                    border = BorderStroke(1.dp, CycleRose.copy(alpha = 0.4f))
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
                                                color = CycleRose
                                            )
                                            Text(
                                                text = "Next: ${cycleSummary.predictedNextDateLabel} (${cycleSummary.daysUntilNextPeriod}d)",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TextPrimary
                                            )
                                        }
                                        Text(
                                            text = cycleSummary.phase.trainingNote,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${cycleLogs.size} cycles logged",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                Button(
                                    onClick = { showLogPeriodDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CycleRose,
                                        contentColor = TextPrimary
                                    ),
                                    modifier = Modifier.testTag("log_period_btn")
                                ) {
                                    Text("Log Period")
                                }
                            }
                            if (cycleLogs.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                cycleLogs.take(3).forEach { log ->
                                    val startStr = LocalDate.ofEpochDay(log.startDateEpochDay).format(dayFmt)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "$startStr · ${log.flowIntensity} flow",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary
                                            )
                                            if (log.symptomsCsv.isNotBlank()) {
                                                Text(
                                                    text = log.symptomsCsv,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { onDeleteCycleLog(log.id) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete cycle log", tint = TextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }

                // Interactive Pie Chart #1: Weekly Muscle Set Distribution
                item(key = "muscle_volume_pie") {
                    KinetixCard(accentGlow = ElectricVolt) {
                        Text("Muscle Set Distribution (7 Days)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("Tap any slice or pill to inspect volume share", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        InteractivePieChart(
                            slices = musclePieSlices,
                            emptyMessage = "Log workout sets to see your interactive muscle breakdown pie chart."
                        )
                    }
                }

                // Interactive Pie Chart #2: Daily Calorie Source Breakdown
                item(key = "macro_split_pie") {
                    KinetixCard(accentGlow = WarmAmber) {
                        Text("Today's Macro Calorie Split", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("Interactive energy share across Protein, Carbs & Fat", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        InteractivePieChart(
                            slices = macroPieSlices,
                            emptyMessage = "Log meals in Nutrition to inspect your interactive macro breakdown."
                        )
                    }
                }

                // Interactive Bar Chart: Daily Steps
                item(key = "steps_history_chart") {
                    KinetixCard(accentGlow = EmeraldPulse) {
                        Text("Daily Steps Trend", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        InteractiveBarChart(
                            entries = stepBarEntries,
                            goalValue = todayState.stepGoal.toFloat(),
                            emptyMessage = "Walk with Kinetix enabled to populate daily step bars."
                        )
                    }
                }

                item(key = "weekly_muscle_volume") {
                    KinetixCard(accentGlow = ElectricVolt) {
                        Text(
                            text = "Weekly Hard Sets per Muscle (Target: 10–20)",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            workoutState.weeklyVolume.forEach { vol ->
                                val (statusLabel, color, bg) = when (vol.status) {
                                    VolumeStatus.UNDER -> Triple("Under (<10)", WarmAmber, AmberContainer)
                                    VolumeStatus.OPTIMAL -> Triple("Optimal", EmeraldPulse, EmeraldContainer)
                                    VolumeStatus.EXCESS -> Triple("High (>20)", CrimsonAlert, CrimsonContainer)
                                }
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(vol.muscleGroup, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${vol.hardSets} sets  ", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                            Text(
                                                text = statusLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = color,
                                                modifier = Modifier
                                                    .background(bg, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (vol.hardSets / 22f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = color,
                                        trackColor = ElevatedCarbon
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "session_history_title") {
                    Text("Completed Sessions", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                }

                if (workoutState.completedSessions.isEmpty()) {
                    item(key = "no_sessions") {
                        Text(
                            text = "Finish a workout session to track progressive overload and session history.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                } else {
                    items(workoutState.completedSessions, key = { it.id }) { session ->
                        val sets = workoutState.historySetsBySession[session.id].orEmpty()
                        val dateText = Instant.ofEpochMilli(session.startedAtEpochMs)
                            .atZone(ZoneId.systemDefault())
                            .format(dateFormatter)
                        KinetixCard(containerColor = CarbonSurface, accentGlow = GlacialCyan) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(session.splitDayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(dateText, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${sets.size} sets logged · ${session.trainingGoalSnapshot}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ElectricVolt
                            )
                        }
                    }
                }
            }
        }

    pendingPhotoUri?.let { uri ->
        val nextWeekNum = progressPhotos.size + 1
        var weekLabel by remember(uri) { mutableStateOf("Week $nextWeekNum") }
        var weightText by remember(uri) { mutableStateOf(todayState.profile.weightKg.toString()) }
        var notes by remember(uri) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { pendingPhotoUri = null },
            title = { Text("Save Weekly Check-In") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weekLabel,
                        onValueChange = { weekLabel = it },
                        label = { Text("Week label (e.g. Week 4)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Weight (kg)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (lighting, pump, energy)") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddProgressPhoto(
                            uri,
                            weekLabel,
                            weightText.toDoubleOrNull() ?: todayState.profile.weightKg,
                            notes
                        )
                        pendingPhotoUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricVolt, contentColor = OnElectricVolt)
                ) { Text("Save Photo") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPhotoUri = null }) { Text("Cancel") }
            }
        )
    }

    previewPhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = { previewPhoto = null },
            title = { Text("${photo.weekLabel} · ${photo.weightKgSnapshot} kg") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LocalPhotoImage(
                        filePath = photo.filePath,
                        contentDescription = photo.weekLabel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(ElevatedCarbon),
                            contentAlignment = Alignment.Center
                        ) { Text("Photo unavailable", color = TextMuted) }
                    }
                    if (photo.notes.isNotBlank()) {
                        Text(photo.notes, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewPhoto = null }) { Text("Close") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteProgressPhoto(photo)
                        previewPhoto = null
                    }
                ) { Text("Delete", color = CrimsonAlert) }
            }
        )
    }

    if (showLogPeriodDialog) {
        var daysAgo by remember { mutableIntStateOf(0) }
        var flow by remember { mutableStateOf("Medium") }
        val selectedSymptoms = remember { mutableStateListOf<String>() }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLogPeriodDialog = false },
            title = { Text("Log Period Start") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Start Date", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "Today", 1 to "Yesterday", 2 to "2d ago", 3 to "3d ago").forEach { (d, label) ->
                            FilterChip(
                                selected = daysAgo == d,
                                onClick = { daysAgo = d },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CycleContainer,
                                    selectedLabelColor = CycleRose
                                )
                            )
                        }
                    }
                    Text("Flow Intensity", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Spotting", "Light", "Medium", "Heavy").forEach { option ->
                            FilterChip(
                                selected = flow == option,
                                onClick = { flow = option },
                                label = { Text(option) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CycleContainer,
                                    selectedLabelColor = CycleRose
                                )
                            )
                        }
                    }
                    Text("Symptoms", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Cramps", "Fatigue", "Bloating", "Headache", "Mood").forEach { sym ->
                            val active = sym in selectedSymptoms
                            FilterChip(
                                selected = active,
                                onClick = {
                                    if (active) selectedSymptoms.remove(sym) else selectedSymptoms.add(sym)
                                },
                                label = { Text(sym) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CycleContainer,
                                    selectedLabelColor = CycleRose
                                )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val startDay = LocalDate.now().minusDays(daysAgo.toLong()).toEpochDay()
                        onLogCycle(startDay, null, flow, selectedSymptoms.joinToString(", "), notes)
                        showLogPeriodDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CycleRose, contentColor = TextPrimary)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showLogPeriodDialog = false }) { Text("Cancel") }
            }
        )
    }
}
