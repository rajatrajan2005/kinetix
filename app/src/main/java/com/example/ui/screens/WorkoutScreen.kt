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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import com.example.domain.model.VolumeStatus
import com.example.ui.components.KinetixAmbientBackdrop
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
    onDeleteSplitDay: (Long) -> Unit,
    onRemoveSplitExercise: (Long) -> Unit,
    onAddExerciseToSplitDay: (Long, ExerciseEntity) -> Unit,
    onImportSplitText: (String) -> Unit
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

    KinetixAmbientBackdrop(
        primaryOrb = ElectricVolt,
        secondaryOrb = GlacialCyan,
        tertiaryOrb = WarmAmber
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
        ) {
            // Compact universal music controller on workout screen
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
                        contentPadding = PaddingValues(16.dp),
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
                        contentPadding = PaddingValues(16.dp),
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
                                        Text("Paste Split")
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
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

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Weight stepper
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { weightKg = (weightKg - 2.5).coerceAtLeast(0.0) },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) { Text("-2.5") }
                Text(
                    text = "${weightKg}kg",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                OutlinedButton(
                    onClick = { weightKg += 2.5 },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) { Text("+2.5") }
            }

            // Reps stepper
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { reps = (reps - 1).coerceAtLeast(1) },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) { Text("-") }
                Text(
                    text = "${reps}r",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                OutlinedButton(
                    onClick = { reps += 1 },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) { Text("+") }
            }

            Button(
                onClick = { onLogSet(block.loggedSets.size + 1, weightKg, reps) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricVolt,
                    contentColor = OnElectricVolt
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("log_set_${block.exerciseId}")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Log")
            }
        }
    }
}
