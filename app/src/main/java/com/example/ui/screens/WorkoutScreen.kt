package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.entity.ExerciseEntity
import com.example.data.local.entity.SplitDayEntity
import com.example.data.local.entity.SplitExerciseEntity
import com.example.domain.model.SplitPreset
import com.example.domain.model.SportDiscipline
import com.example.domain.model.VolumeStatus
import com.example.ui.components.KinetixCard
import com.example.ui.components.MiniMediaPlayerBar
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.OnElectricVolt
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltContainer
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.ActiveExerciseBlock
import com.example.ui.viewmodel.WorkoutUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    onRefreshMedia: () -> Unit,
    onOpenListenerSettings: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaSeekTo: (Long) -> Unit,
    onStartWorkout: (SplitDayEntity) -> Unit,
    onFinishWorkout: () -> Unit,
    onDiscardWorkout: () -> Unit,
    onLogSet: (
        exerciseId: String,
        exerciseName: String,
        muscleGroup: String,
        muscleHead: String,
        setNumber: Int,
        weightKg: Double,
        reps: Int,
        isAdHoc: Boolean
    ) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onAdjustRestTimer: (Int) -> Unit,
    onStopRestTimer: () -> Unit,
    onSelectMuscleFilter: (String) -> Unit,
    onSelectHeadFilter: (String) -> Unit,
    onPickerQueryChange: (String) -> Unit,
    onAddAdHocExercise: (ExerciseEntity) -> Unit,
    onConfirmMakePermanent: (ExerciseEntity) -> Unit,
    onDismissMakePermanent: () -> Unit,
    onAddSplitDay: (String, String) -> Unit,
    onUpdateSplitDay: (Long, String, String) -> Unit,
    onMoveSplitExercise: (Long, Long, Boolean) -> Unit,
    onDeleteSplitDay: (Long) -> Unit,
    onRemoveSplitExercise: (Long) -> Unit,
    onAddExerciseToSplitDay: (Long, ExerciseEntity) -> Unit,
    onImportSplitText: (String) -> Unit,
    onApplySplitPreset: (SplitPreset) -> Unit,
    onSelectSport: (SportDiscipline) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onRefreshMedia()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showPickerForDayId by remember { mutableStateOf<Long?>(null) }
    var showPickerForActive by remember { mutableStateOf(false) }
    var showPasteSplitDialog by remember { mutableStateOf(false) }
    var showAddDayDialog by remember { mutableStateOf(false) }
    var editingDayId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
            ) {
                // Compact universal music controller with earbuds detection on workout screen
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    MiniMediaPlayerBar(
                        mediaState = state.mediaState,
                        onPlayPause = onMediaPlayPause,
                        onPrevious = onMediaPrevious,
                        onNext = onMediaNext,
                        onSeekTo = onMediaSeekTo,
                        onOpenListenerSettings = onOpenListenerSettings
                    )
                }

                // Live Rest Timer banner
                AnimatedVisibility(visible = state.restTimer.isRunning || state.restTimer.remainingSeconds > 0) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = VoltContainer,
                        border = BorderStroke(1.dp, ElectricVolt.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = ElectricVolt)
                                Spacer(Modifier.width(8.dp))
                                val mins = state.restTimer.remainingSeconds / 60
                                val secs = state.restTimer.remainingSeconds % 60
                                Text(
                                    text = "Rest %d:%02d".format(mins, secs),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ElectricVolt
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { onAdjustRestTimer(-15) }) {
                                    Text("-15s", color = TextPrimary)
                                }
                                TextButton(onClick = { onAdjustRestTimer(15) }) {
                                    Text("+15s", color = TextPrimary)
                                }
                                TextButton(onClick = onStopRestTimer) {
                                    Text("Skip", color = ElectricVolt)
                                }
                            }
                        }
                    }
                }

                AnimatedContent(
                    targetState = state.activeSession != null,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "workoutMode"
                ) { isActive ->
                    if (isActive && state.activeSession != null) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 108.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(key = "active_header") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${state.trainingGoal.label.uppercase()} · ${state.prescription.sets}×${state.prescription.repMin}–${state.prescription.repMax}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ElectricVolt
                                        )
                                        Text(
                                            text = state.activeSession.splitDayName,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = TextPrimary
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = onDiscardWorkout) {
                                            Text("Discard", color = CrimsonAlert)
                                        }
                                        Button(
                                            onClick = onFinishWorkout,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ElectricVolt,
                                                contentColor = OnElectricVolt
                                            ),
                                            modifier = Modifier.testTag("finish_workout_btn")
                                        ) {
                                            Text("Finish")
                                        }
                                    }
                                }
                            }

                            if (state.pendingMakePermanentExercise != null) {
                                item(key = "make_permanent_banner") {
                                    val ex = state.pendingMakePermanentExercise
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = CyanContainer,
                                        border = BorderStroke(1.dp, GlacialCyan)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Added \"${ex.name}\" for today only",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "Your saved split is untouched.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextSecondary
                                                )
                                            }
                                            Row {
                                                TextButton(onClick = onDismissMakePermanent) {
                                                    Text("Keep today", color = TextSecondary)
                                                }
                                                Button(
                                                    onClick = { onConfirmMakePermanent(ex) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = GlacialCyan,
                                                        contentColor = AmoledBlack
                                                    ),
                                                    modifier = Modifier.testTag("make_permanent_btn")
                                                ) {
                                                    Text("Make Permanent")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            items(state.activeBlocks, key = { it.exerciseId }) { block ->
                                ActiveExerciseCard(
                                    block = block,
                                    prescriptionReps = "${state.prescription.sets} × ${state.prescription.repMin}–${state.prescription.repMax}",
                                    onLogSet = { setNumber, weight, reps ->
                                        onLogSet(
                                            block.exerciseId,
                                            block.exerciseName,
                                            block.muscleGroup,
                                            block.muscleHead,
                                            setNumber,
                                            weight,
                                            reps,
                                            block.isAdHoc
                                        )
                                    },
                                    onDeleteSet = onDeleteSet
                                )
                            }

                            item(key = "add_adhoc_btn") {
                                OutlinedButton(
                                    onClick = { showPickerForActive = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("open_exercise_picker_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Exercise via Muscle / Head Picker")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 108.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(key = "split_header") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${state.trainingGoal.label.uppercase()} TUNING",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ElectricVolt
                                        )
                                        Text(
                                            text = "My Split",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = TextPrimary
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { showPasteSplitDialog = true },
                                            modifier = Modifier.testTag("paste_split_btn")
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Paste")
                                        }
                                        FilledTonalButton(
                                            onClick = { showAddDayDialog = true },
                                            modifier = Modifier.testTag("add_split_day_btn")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text("Day")
                                        }
                                    }
                                }
                            }

                            item(key = "split_presets_card") {
                                KinetixCard(accentGlow = ElectricVolt) {
                                    Text("Split Presets (Repeating Cycle)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Spacer(Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        SplitPreset.entries.forEach { preset ->
                                            val isCurrent = when (preset) {
                                                SplitPreset.PPL -> state.splitDays.any { it.name.startsWith("Push", ignoreCase = true) }
                                                SplitPreset.DOUBLE_MUSCLE -> state.splitDays.any { it.name.contains("Back & Biceps", ignoreCase = true) }
                                            }
                                            FilledTonalButton(
                                                onClick = { onApplySplitPreset(preset) },
                                                modifier = Modifier
                                                    .height(44.dp)
                                                    .testTag("split_preset_${preset.name}")
                                            ) {
                                                if (isCurrent) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp), tint = ElectricVolt)
                                                    Spacer(Modifier.width(4.dp))
                                                }
                                                Text(preset.label)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Default Push/Pull/Legs or Double Muscle (Back+Bi · Chest+Tri · Legs+Shoulders · Repeat)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            item(key = "sport_conditioning_card") {
                                KinetixCard(
                                    accentGlow = GlacialCyan,
                                    modifier = Modifier.testTag("sport_conditioning_card")
                                ) {
                                    Text("Sport & Athletic Exercises", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                    Text(
                                        text = state.sportDiscipline.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(SportDiscipline.entries, key = { it.name }) { sport ->
                                            FilterChip(
                                                selected = state.sportDiscipline == sport,
                                                onClick = { onSelectSport(sport) },
                                                label = { Text(sport.label) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = VoltContainer,
                                                    selectedLabelColor = ElectricVolt
                                                ),
                                                modifier = Modifier.testTag("sport_chip_${sport.name}")
                                            )
                                        }
                                    }
                                    if (state.sportDiscipline != SportDiscipline.NONE && state.sportRecommendedExercises.isNotEmpty()) {
                                        Spacer(Modifier.height(10.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            state.sportRecommendedExercises.forEach { ex ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(CarbonSurface, RoundedCornerShape(10.dp))
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(ex.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                                        Text(
                                                            text = "${ex.muscleGroup} · ${ex.muscleHead}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = GlacialCyan
                                                        )
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        FilledTonalButton(
                                                            onClick = { onAddAdHocExercise(ex) },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(36.dp)
                                                        ) { Text("+ Today") }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            items(state.splitDays, key = { it.id }) { day ->
                                val exercises = state.splitExercisesByDay[day.id].orEmpty()
                                KinetixCard(accentGlow = ElectricVolt) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = day.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = day.targetMusclesCsv.ifBlank { "${exercises.size} exercises" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { editingDayId = day.id },
                                                modifier = Modifier
                                                    .testTag("edit_split_day_${day.id}")
                                                    .semantics { contentDescription = "Edit ${day.name}" }
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, tint = ElectricVolt)
                                            }
                                            IconButton(
                                                onClick = { showPickerForDayId = day.id },
                                                modifier = Modifier.semantics { contentDescription = "Add exercise to ${day.name}" }
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = GlacialCyan)
                                            }
                                            IconButton(
                                                onClick = { onDeleteSplitDay(day.id) },
                                                modifier = Modifier.semantics { contentDescription = "Delete ${day.name}" }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = TextMuted)
                                            }
                                            Button(
                                                onClick = { onStartWorkout(day) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = ElectricVolt,
                                                    contentColor = OnElectricVolt
                                                ),
                                                modifier = Modifier.testTag("start_split_day_${day.id}")
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Start")
                                            }
                                        }
                                    }

                                    if (exercises.isNotEmpty()) {
                                        Spacer(Modifier.height(10.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            exercises.forEach { ex ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(CarbonSurface, RoundedCornerShape(10.dp))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = ex.exerciseName,
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            color = TextPrimary
                                                        )
                                                        Text(
                                                            text = "${ex.muscleGroup} (${ex.muscleHead}) · ${state.prescription.sets}×${state.prescription.repMin}–${state.prescription.repMax}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { onRemoveSplitExercise(ex.id) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "Remove ${ex.exerciseName}",
                                                            tint = TextMuted,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item(key = "weekly_volume_preview") {
                                KinetixCard(accentGlow = GlacialCyan) {
                                    Text(
                                        text = "7-Day Hard Sets (10–20 target)",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    state.weeklyVolume.take(6).forEach { vol ->
                                        val (badgeText, badgeColor, badgeBg) = when (vol.status) {
                                            VolumeStatus.UNDER -> Triple("LOW <10", WarmAmber, AmberContainer)
                                            VolumeStatus.OPTIMAL -> Triple("OPTIMAL", EmeraldPulse, EmeraldContainer)
                                            VolumeStatus.EXCESS -> Triple("HIGH >20", CrimsonAlert, CrimsonContainer)
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(vol.muscleGroup, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${vol.hardSets} sets",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = TextSecondary
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = badgeText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = badgeColor,
                                                    modifier = Modifier
                                                        .background(badgeBg, RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    if (showPickerForActive || showPickerForDayId != null) {
        ExercisePickerSheet(
            muscleGroups = state.muscleGroups,
            muscleHeads = state.muscleHeads,
            selectedMuscle = state.selectedMuscle,
            selectedHead = state.selectedHead,
            query = state.pickerQuery,
            exercises = state.filteredExercises,
            onSelectMuscle = onSelectMuscleFilter,
            onSelectHead = onSelectHeadFilter,
            onQueryChange = onPickerQueryChange,
            onAddExercise = { ex ->
                val dayId = showPickerForDayId
                if (dayId != null) {
                    onAddExerciseToSplitDay(dayId, ex)
                    showPickerForDayId = null
                } else {
                    onAddAdHocExercise(ex)
                    showPickerForActive = false
                }
            },
            onDismiss = {
                showPickerForActive = false
                showPickerForDayId = null
            }
        )
    }

    editingDayId?.let { dayId ->
        val day = state.splitDays.firstOrNull { it.id == dayId }
        val exercises = state.splitExercisesByDay[dayId].orEmpty()
        if (day != null) {
            EditSplitDayDialog(
                day = day,
                exercises = exercises,
                onSaveDayMeta = { newName, newMuscles ->
                    onUpdateSplitDay(day.id, newName, newMuscles)
                },
                onMoveExercise = { exId, moveUp ->
                    onMoveSplitExercise(day.id, exId, moveUp)
                },
                onRemoveExercise = onRemoveSplitExercise,
                onOpenAddExercise = {
                    editingDayId = null
                    showPickerForDayId = day.id
                },
                onDismiss = { editingDayId = null }
            )
        }
    }

    if (showPasteSplitDialog) {
        var splitText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasteSplitDialog = false },
            title = { Text("Define Your Split") },
            text = {
                Column {
                    Text(
                        text = "Format one day per line: Day Name: Exercise 1, Exercise 2, ...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = splitText,
                        onValueChange = { splitText = it },
                        placeholder = { Text("[PASTE YOUR SPLIT HERE]") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paste_split_textfield")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportSplitText(splitText)
                        showPasteSplitDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricVolt,
                        contentColor = OnElectricVolt
                    )
                ) { Text("Import Split") }
            },
            dismissButton = {
                TextButton(onClick = { showPasteSplitDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddDayDialog) {
        var dayName by remember { mutableStateOf("") }
        var muscles by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDayDialog = false },
            title = { Text("New Split Day") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dayName,
                        onValueChange = { dayName = it },
                        label = { Text("Day name (e.g. Upper A)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = muscles,
                        onValueChange = { muscles = it },
                        label = { Text("Target muscles (e.g. Chest, Back)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dayName.isNotBlank()) onAddSplitDay(dayName, muscles)
                        showAddDayDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricVolt,
                        contentColor = OnElectricVolt
                    )
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDayDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EditSplitDayDialog(
    day: SplitDayEntity,
    exercises: List<SplitExerciseEntity>,
    onSaveDayMeta: (String, String) -> Unit,
    onMoveExercise: (Long, Boolean) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onOpenAddExercise: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(day.id) { mutableStateOf(day.name) }
    var muscles by remember(day.id) { mutableStateOf(day.targetMusclesCsv) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Split Day") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Day Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = muscles,
                    onValueChange = { muscles = it },
                    label = { Text("Target Muscles") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Exercises (${exercises.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    TextButton(onClick = onOpenAddExercise) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
                exercises.forEachIndexed { idx, ex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CarbonSurface, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ex.exerciseName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            IconButton(
                                onClick = { onMoveExercise(ex.id, true) },
                                enabled = idx > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onMoveExercise(ex.id, false) },
                                enabled = idx < exercises.lastIndex,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onRemoveExercise(ex.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = CrimsonAlert, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveDayMeta(name, muscles)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricVolt, contentColor = OnElectricVolt)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ActiveExerciseCard(
    block: ActiveExerciseBlock,
    prescriptionReps: String,
    onLogSet: (setNumber: Int, weightKg: Double, reps: Int) -> Unit,
    onDeleteSet: (Long) -> Unit
) {
    var weightKg by remember(block.exerciseId, block.suggestedWeightKg) {
        mutableStateOf(block.suggestedWeightKg)
    }
    var reps by remember(block.exerciseId, block.suggestedReps) {
        mutableIntStateOf(block.suggestedReps)
    }

    KinetixCard(accentGlow = if (block.isAdHoc) GlacialCyan else ElectricVolt) {
        // Upper Header Row: Exercise Name + Prominent Large Log Set / Done Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = block.exerciseName,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    if (block.isAdHoc) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "TODAY ONLY",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlacialCyan,
                            modifier = Modifier
                                .background(CyanContainer, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${block.muscleGroup} · ${block.muscleHead} · Target $prescriptionReps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { onLogSet(block.loggedSets.size + 1, weightKg, reps) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricVolt,
                    contentColor = OnElectricVolt
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("log_set_${block.exerciseId}")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Log Set", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (block.overloadSuggestion != null) {
            Spacer(Modifier.height(8.dp))
            val isBoost = block.overloadSuggestion.shouldIncrease
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isBoost) VoltContainer else CarbonSurface,
                border = BorderStroke(1.dp, if (isBoost) ElectricVolt else HairlineBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = if (isBoost) ElectricVolt else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = block.overloadSuggestion.reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isBoost) ElectricVolt else TextSecondary
                    )
                }
            }
        }

        if (block.loggedSets.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                block.loggedSets.forEach { set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CarbonSurface, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Set ${set.setNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricVolt
                        )
                        Text(
                            text = "${set.weightKg} kg × ${set.reps} reps",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        IconButton(
                            onClick = { onDeleteSet(set.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete set",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        // Dedicated spacious stepper row without the cramped Log button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { weightKg = (weightKg - 2.5).coerceAtLeast(0.0) },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(44.dp)
                ) { Text("-2.5") }
                Text(
                    text = "${weightKg} kg",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                OutlinedButton(
                    onClick = { weightKg += 2.5 },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(44.dp)
                ) { Text("+2.5") }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { reps = (reps - 1).coerceAtLeast(1) },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(44.dp)
                ) { Text("-") }
                Text(
                    text = "${reps} reps",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                OutlinedButton(
                    onClick = { reps += 1 },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(44.dp)
                ) { Text("+") }
            }
        }
    }
}
