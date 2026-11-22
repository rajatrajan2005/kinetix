package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SportsScore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.local.entity.ExerciseEntity
import com.example.data.local.entity.SplitDayEntity
import com.example.data.local.entity.SplitExerciseEntity
import com.example.domain.model.SplitPreset
import com.example.domain.model.SportDiscipline
import com.example.ui.components.AppleCollapsedNavBar
import com.example.ui.components.AppleLargeTitleHeader
import com.example.ui.components.AppleRowDivider
import com.example.ui.components.BottomSheet
import com.example.ui.components.Card
import com.example.ui.components.EmptyState
import com.example.ui.components.ListRow
import com.example.ui.components.MiniMediaPlayerBar
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RingProgress
import com.example.ui.components.SegmentedControl
import com.example.ui.components.StatChip
import com.example.ui.components.Stepper
import com.example.ui.components.applePressScale
import com.example.ui.theme.KinetixTokens
import com.example.ui.theme.rememberReduceAnimations
import com.example.ui.viewmodel.ActiveExerciseBlock
import com.example.ui.viewmodel.ExerciseSessionConfig
import com.example.ui.viewmodel.RestTimerState
import com.example.ui.viewmodel.WorkoutUiState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    onSelectActiveExercise: (String) -> Unit,
    onUpdateExerciseConfig: (String, ExerciseSessionConfig) -> Unit,
    onSkipActiveExercise: (String) -> Unit,
    onInspectExercise: (exerciseId: String, name: String, muscle: String, head: String) -> Unit,
    onDismissExerciseInspect: () -> Unit,
    onLogSet: (
        exerciseId: String,
        exerciseName: String,
        muscleGroup: String,
        muscleHead: String,
        setNumber: Int,
        weightKg: Double,
        reps: Int,
        isAdHoc: Boolean,
        isWarmup: Boolean
    ) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onToggleRestPause: () -> Unit,
    onAdjustRestTimer: (Int) -> Unit,
    onStopRestTimer: () -> Unit,
    onStartRestTimer: (Int) -> Unit,
    onSelectMuscleFilter: (String) -> Unit,
    onSelectHeadFilter: (String) -> Unit,
    onPickerQueryChange: (String) -> Unit,
    onAddAdHocExercise: (ExerciseEntity) -> Unit,
    onConfirmMakePermanent: (ExerciseEntity) -> Unit,
    onDismissMakePermanent: () -> Unit,
    onAddSplitDay: (String, String) -> Unit,
    onAddRestDay: () -> Unit,
    onUpdateSplitDay: (Long, String, String) -> Unit,
    onReorderSplitExercises: (Long, List<Long>) -> Unit,
    onDeleteSplitDay: (Long) -> Unit,
    onRemoveSplitExercise: (Long) -> Unit,
    onAddExerciseToSplitDay: (Long, ExerciseEntity) -> Unit,
    onImportSplitText: (String) -> Unit,
    onApplySplitPreset: (SplitPreset) -> Unit,
    onSelectSport: (SportDiscipline) -> Unit
) {
    val view = LocalView.current
    val navInsetBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomPadding = KinetixTokens.Spacing.NavBarClearance + navInsetBottom
    val listState = rememberLazyListState()

    var showPickerForDayId by remember { mutableStateOf<Long?>(null) }
    var showPickerForActive by remember { mutableStateOf(false) }
    var showPasteSplitDialog by remember { mutableStateOf(false) }
    var showAddDayDialog by remember { mutableStateOf(false) }
    var showSplitMenu by remember { mutableStateOf(false) }
    var editingDayId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteExercise by remember { mutableStateOf<SplitExerciseEntity?>(null) }
    var expandedDayId by remember { mutableStateOf<Long?>(null) }
    var showSportPicker by remember { mutableStateOf(false) }

    val reduceMotion = rememberReduceAnimations()

    AnimatedContent(
        targetState = state.activeSession != null,
        transitionSpec = {
            fadeIn(KinetixTokens.Motion.tabFadeTween(reduceMotion)) togetherWith
                fadeOut(KinetixTokens.Motion.tabFadeTween(reduceMotion))
        },
        label = "workoutSessionTransition"
    ) { isActive ->
        if (isActive && state.activeSession != null) {
            ActiveWorkoutFullScreen(
                state = state,
                bottomClearance = listBottomPadding,
                onSelectExercise = onSelectActiveExercise,
                onUpdateConfig = onUpdateExerciseConfig,
                onSkipExercise = onSkipActiveExercise,
                onInspectExercise = onInspectExercise,
                onLogSet = onLogSet,
                onDeleteSet = onDeleteSet,
                onToggleRestPause = onToggleRestPause,
                onAdjustRestTimer = onAdjustRestTimer,
                onStopRestTimer = onStopRestTimer,
                onStartRestTimer = onStartRestTimer,
                onOpenPicker = { showPickerForActive = true },
                onConfirmMakePermanent = onConfirmMakePermanent,
                onDismissMakePermanent = onDismissMakePermanent,
                onFinishWorkout = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onFinishWorkout()
                },
                onDiscardWorkout = onDiscardWorkout,
                onRefreshMedia = onRefreshMedia,
                onOpenListenerSettings = onOpenListenerSettings,
                onMediaPlayPause = onMediaPlayPause,
                onMediaPrevious = onMediaPrevious,
                onMediaNext = onMediaNext,
                onMediaSeekTo = onMediaSeekTo
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
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
                        bottom = listBottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(KinetixTokens.Spacing.CardGap)
                ) {
                    item(key = "workout_large_title") {
                        val activeTrainingDays = state.splitDays.count { day ->
                            !(state.splitExercisesByDay[day.id].isNullOrEmpty() && day.name.contains("rest", ignoreCase = true))
                        }
                        val headerSubtitle = if (activeTrainingDays > 0) {
                            "${state.trainingGoal.label} · ${activeTrainingDays}-Day Training Schedule"
                        } else {
                            "${state.trainingGoal.label} · ${state.prescription.sets}×${state.prescription.repMin}–${state.prescription.repMax}"
                        }
                        AppleLargeTitleHeader(
                            title = "Workout",
                            subtitle = headerSubtitle,
                            trailing = {
                                Box {
                                    IconButton(
                                        onClick = { showSplitMenu = true },
                                        modifier = Modifier.testTag("split_actions_menu_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreHoriz,
                                            contentDescription = "Split options",
                                            tint = KinetixTokens.Colors.TextPrimary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showSplitMenu,
                                        onDismissRequest = { showSplitMenu = false },
                                        containerColor = KinetixTokens.Colors.NestedSurface
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Add split day") },
                                            onClick = {
                                                showSplitMenu = false
                                                showAddDayDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Add Rest day") },
                                            onClick = {
                                                showSplitMenu = false
                                                onAddRestDay()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Paste custom split") },
                                            onClick = {
                                                showSplitMenu = false
                                                showPasteSplitDialog = true
                                            },
                                            modifier = Modifier.testTag("paste_split_btn")
                                        )
                                        HorizontalDivider(color = KinetixTokens.Colors.Hairline)
                                        SplitPreset.entries.forEach { preset ->
                                            DropdownMenuItem(
                                                text = { Text("Use ${preset.label}") },
                                                onClick = {
                                                    showSplitMenu = false
                                                    onApplySplitPreset(preset)
                                                },
                                                modifier = Modifier.testTag("split_preset_${preset.name}")
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }

                    item(key = "mini_media_strip") {
                        MiniMediaPlayerBar(
                            mediaState = state.mediaState,
                            onPlayPause = onMediaPlayPause,
                            onPrevious = onMediaPrevious,
                            onNext = onMediaNext,
                            onSeekTo = onMediaSeekTo,
                            onOpenListenerSettings = onOpenListenerSettings
                        )
                    }

                    if (state.splitDays.isEmpty()) {
                        item(key = "empty_split") {
                            Card {
                                EmptyState(
                                    icon = Icons.Rounded.FitnessCenter,
                                    message = "No workout days configured yet.",
                                    accentColor = KinetixTokens.Colors.Workout,
                                    actionLabel = "Load 6-Day Double Muscle",
                                    onAction = { onApplySplitPreset(SplitPreset.DOUBLE_MUSCLE) }
                                )
                            }
                        }
                    } else {
                        items(state.splitDays, key = { it.id }) { day ->
                            val exercises = state.splitExercisesByDay[day.id].orEmpty()
                            val isExpanded = expandedDayId == day.id
                            SplitDayCard(
                                day = day,
                                exercises = exercises,
                                prescriptionSets = state.prescription.sets,
                                prescriptionReps = "${state.prescription.repMin}–${state.prescription.repMax}",
                                expanded = isExpanded,
                                onToggleExpand = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    expandedDayId = if (isExpanded) null else day.id
                                },
                                onStart = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onStartWorkout(day)
                                },
                                onEditDay = { editingDayId = day.id },
                                onAddExercise = { showPickerForDayId = day.id },
                                onDeleteDay = { onDeleteSplitDay(day.id) },
                                onInspectExercise = { ex ->
                                    onInspectExercise(
                                        ex.exerciseId,
                                        ex.exerciseName,
                                        ex.muscleGroup,
                                        ex.muscleHead
                                    )
                                },
                                onRequestDeleteExercise = { ex -> pendingDeleteExercise = ex },
                                onReorderExercises = { orderedIds ->
                                    onReorderSplitExercises(day.id, orderedIds)
                                }
                            )
                        }
                    }

                    item(key = "sport_conditioning_compact") {
                        Card(
                            modifier = Modifier.testTag("sport_conditioning_card"),
                            onClick = { showSportPicker = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(KinetixTokens.Colors.HydrationMutedBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SportsScore,
                                            contentDescription = null,
                                            tint = KinetixTokens.Colors.Hydration,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Sport Conditioning",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = KinetixTokens.Colors.TextPrimary
                                        )
                                        Text(
                                            text = state.sportDiscipline.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = KinetixTokens.Colors.TextSecondary
                                        )
                                    }
                                }
                                Text(
                                    text = "Change",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = KinetixTokens.Colors.Hydration
                                )
                            }

                            if (state.sportDiscipline != SportDiscipline.NONE && state.sportRecommendedExercises.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.sportRecommendedExercises.forEach { ex ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(KinetixTokens.Radius.NestedShape)
                                                .background(KinetixTokens.Colors.NestedSurface)
                                                .clickable {
                                                    onInspectExercise(ex.id, ex.name, ex.muscleGroup, ex.muscleHead)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ex.name,
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                                    color = KinetixTokens.Colors.TextPrimary
                                                )
                                                Text(
                                                    text = ex.muscleGroup,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = KinetixTokens.Colors.TextSecondary
                                                )
                                            }
                                            Surface(
                                                onClick = { onAddAdHocExercise(ex) },
                                                shape = KinetixTokens.Radius.PillShape,
                                                color = KinetixTokens.Colors.WorkoutMutedBg
                                            ) {
                                                Text(
                                                    text = "+ Today",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = KinetixTokens.Colors.Workout,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AppleCollapsedNavBar(
                    title = "Workout",
                    listState = listState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    trailing = {
                        IconButton(onClick = { showSplitMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "Split options",
                                tint = KinetixTokens.Colors.TextPrimary
                            )
                        }
                    }
                )
            }
        }
    }

    // Exercise Detail Bottom Sheet (shows full anatomical head & instructions)
    state.inspectedExercise?.let { ex ->
        BottomSheet(
            title = ex.name,
            subtitle = ex.muscleGroup,
            onDismiss = onDismissExerciseInspect
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatChip(
                        text = "Primary: ${ex.muscleGroup}",
                        foregroundColor = KinetixTokens.Colors.Workout,
                        backgroundColor = KinetixTokens.Colors.WorkoutMutedBg
                    )
                    StatChip(
                        text = "Target: ${ex.muscleHead}",
                        foregroundColor = KinetixTokens.Colors.Hydration,
                        backgroundColor = KinetixTokens.Colors.HydrationMutedBg
                    )
                    StatChip(
                        text = ex.equipment,
                        foregroundColor = KinetixTokens.Colors.TextPrimary,
                        backgroundColor = KinetixTokens.Colors.NestedSurface
                    )
                }
                if (ex.secondaryMuscles.isNotBlank()) {
                    Text(
                        text = "Secondary: ${ex.secondaryMuscles}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KinetixTokens.Colors.TextSecondary
                    )
                }
                Text(
                    text = ex.instructions.ifBlank { "Controlled eccentric, full range of motion." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = KinetixTokens.Colors.TextPrimary
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showSportPicker) {
        BottomSheet(
            title = "Sport Conditioning",
            subtitle = "Choose a sport for supplemental exercises",
            onDismiss = { showSportPicker = false }
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SportDiscipline.entries.forEachIndexed { idx, sport ->
                    ListRow(
                        title = sport.label,
                        subtitle = sport.subtitle,
                        selectedCheckmark = state.sportDiscipline == sport,
                        checkmarkTint = KinetixTokens.Colors.Hydration,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSelectSport(sport)
                            showSportPicker = false
                        },
                        modifier = Modifier.testTag("sport_chip_${sport.name}")
                    )
                    if (idx < SportDiscipline.entries.lastIndex) AppleRowDivider()
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

    pendingDeleteExercise?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteExercise = null },
            containerColor = KinetixTokens.Colors.CardSurface,
            title = { Text("Remove Exercise?") },
            text = { Text("Remove \"${target.exerciseName}\" from this split day?") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveSplitExercise(target.id)
                        pendingDeleteExercise = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KinetixTokens.Colors.StatusLowFg,
                        contentColor = KinetixTokens.Colors.TextPrimary
                    )
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteExercise = null }) { Text("Cancel") }
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
                onSaveDayMeta = { newName, newMuscles -> onUpdateSplitDay(day.id, newName, newMuscles) },
                onAddExercise = {
                    editingDayId = null
                    showPickerForDayId = day.id
                },
                onRemoveExercise = onRemoveSplitExercise,
                onDismiss = { editingDayId = null }
            )
        }
    }

    if (showPasteSplitDialog) {
        var splitText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasteSplitDialog = false },
            containerColor = KinetixTokens.Colors.CardSurface,
            title = { Text("Paste Custom Split") },
            text = {
                Column {
                    Text(
                        text = "One day per line: Day Name: Exercise 1, Exercise 2, ...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KinetixTokens.Colors.TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = splitText,
                        onValueChange = { splitText = it },
                        placeholder = { Text("Chest + Triceps: Barbell Bench Press, ...") },
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
                        containerColor = KinetixTokens.Colors.Workout,
                        contentColor = KinetixTokens.Colors.OnWorkout
                    )
                ) { Text("Import") }
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
            containerColor = KinetixTokens.Colors.CardSurface,
            title = { Text("New Split Day") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dayName,
                        onValueChange = { dayName = it },
                        label = { Text("Day name (e.g. Arms + Core)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = muscles,
                        onValueChange = { muscles = it },
                        label = { Text("Target muscles (e.g. Biceps, Triceps)") },
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
                        containerColor = KinetixTokens.Colors.Workout,
                        contentColor = KinetixTokens.Colors.OnWorkout
                    )
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDayDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SplitDayCard(
    day: SplitDayEntity,
    exercises: List<SplitExerciseEntity>,
    prescriptionSets: Int,
    prescriptionReps: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onStart: () -> Unit,
    onEditDay: () -> Unit,
    onAddExercise: () -> Unit,
    onDeleteDay: () -> Unit,
    onInspectExercise: (SplitExerciseEntity) -> Unit,
    onRequestDeleteExercise: (SplitExerciseEntity) -> Unit,
    onReorderExercises: (List<Long>) -> Unit
) {
    val view = LocalView.current
    val reduceMotion = rememberReduceAnimations()
    val chevronRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = KinetixTokens.Motion.standardSpring(reduceMotion),
        label = "expandChevron"
    )
    var showDayMenu by remember { mutableStateOf(false) }

    val isRestDay = exercises.isEmpty() && day.name.contains("rest", ignoreCase = true)

    val distinctMuscles = remember(exercises, day.targetMusclesCsv) {
        val fromEx = exercises.map { it.muscleGroup }.distinct()
        if (fromEx.isNotEmpty()) fromEx
        else day.targetMusclesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val estMinutes = remember(exercises, prescriptionSets) {
        if (exercises.isEmpty()) 0
        else {
            val totalSec = exercises.sumOf { ex ->
                val sets = if (ex.targetSets > 0) ex.targetSets else prescriptionSets
                val rest = if (ex.restSeconds > 0) ex.restSeconds else 90
                sets * (40 + rest)
            }
            (totalSec / 60.0).roundToInt().coerceAtLeast(15)
        }
    }

    Card(onClick = onToggleExpand) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = day.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = KinetixTokens.Colors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        isRestDay -> "Recovery & mobility"
                        exercises.isEmpty() -> "No exercises yet"
                        else -> "${exercises.size} exercises · ~$estMinutes min"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = KinetixTokens.Colors.TextSecondary
                )
            }
            Spacer(Modifier.width(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = KinetixTokens.Colors.TextSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(chevronRotation)
                )
                if (isRestDay) {
                    StatChip(
                        text = "Rest",
                        foregroundColor = KinetixTokens.Colors.Hydration,
                        backgroundColor = KinetixTokens.Colors.HydrationMutedBg,
                        modifier = Modifier.testTag("rest_split_day_${day.id}")
                    )
                } else {
                    PrimaryButton(
                        text = "Start",
                        onClick = onStart,
                        modifier = Modifier.testTag("start_split_day_${day.id}")
                    )
                }
            }
        }

        if (distinctMuscles.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                distinctMuscles.forEach { muscle ->
                    StatChip(
                        text = muscle,
                        foregroundColor = KinetixTokens.Colors.TextPrimary,
                        backgroundColor = KinetixTokens.Colors.NestedSurface
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                HorizontalDivider(thickness = 0.5.dp, color = KinetixTokens.Colors.Hairline)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRestDay) "Recovery focus" else "Hold drag handle to reorder · Swipe left to remove",
                        style = MaterialTheme.typography.labelMedium,
                        color = KinetixTokens.Colors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(
                            onClick = { showDayMenu = true },
                            modifier = Modifier.testTag("edit_split_day_${day.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "Day actions",
                                tint = KinetixTokens.Colors.TextPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showDayMenu,
                            onDismissRequest = { showDayMenu = false },
                            containerColor = KinetixTokens.Colors.NestedSurface
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit day") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    showDayMenu = false
                                    onEditDay()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add exercise") },
                                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                onClick = {
                                    showDayMenu = false
                                    onAddExercise()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete day", color = KinetixTokens.Colors.StatusLowFg) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = KinetixTokens.Colors.StatusLowFg
                                    )
                                },
                                onClick = {
                                    showDayMenu = false
                                    onDeleteDay()
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                when {
                    isRestDay -> {
                        val recoveryItems = remember(day.targetMusclesCsv) {
                            day.targetMusclesCsv.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .ifEmpty {
                                    listOf(
                                        "Light walking",
                                        "Mobility and stretching",
                                        "Recovery and proper sleep"
                                    )
                                }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recoveryItems.forEach { item ->
                                Surface(
                                    shape = KinetixTokens.Radius.NestedShape,
                                    color = KinetixTokens.Colors.NestedSurface,
                                    border = BorderStroke(0.5.dp, KinetixTokens.Colors.Hairline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = KinetixTokens.Colors.TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                    exercises.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Rounded.FitnessCenter,
                            message = "No exercises added to this day.",
                            actionLabel = "Add",
                            onAction = onAddExercise
                        )
                    }
                    else -> {
                        DraggableExerciseList(
                            exercises = exercises,
                            prescriptionSets = prescriptionSets,
                            prescriptionReps = prescriptionReps,
                            onInspect = onInspectExercise,
                            onRequestDelete = onRequestDeleteExercise,
                            onReordered = { orderedIds ->
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onReorderExercises(orderedIds)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraggableExerciseList(
    exercises: List<SplitExerciseEntity>,
    prescriptionSets: Int,
    prescriptionReps: String,
    onInspect: (SplitExerciseEntity) -> Unit,
    onRequestDelete: (SplitExerciseEntity) -> Unit,
    onReordered: (List<Long>) -> Unit
) {
    var localList by remember(exercises) { mutableStateOf(exercises) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 64.dp.toPx() }
    val view = LocalView.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        localList.forEachIndexed { index, ex ->
            val isDragging = draggingId == ex.id
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        onRequestDelete(ex)
                    }
                    false
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(KinetixTokens.Radius.NestedShape)
                            .background(KinetixTokens.Colors.StatusLowBg)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Remove exercise",
                            tint = KinetixTokens.Colors.StatusLowFg
                        )
                    }
                },
                modifier = Modifier
                    .zIndex(if (isDragging) 2f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffsetY else 0f
                        scaleX = if (isDragging) 1.02f else 1f
                        scaleY = if (isDragging) 1.02f else 1f
                    }
            ) {
                Surface(
                    shape = KinetixTokens.Radius.NestedShape,
                    color = KinetixTokens.Colors.NestedSurface,
                    border = BorderStroke(
                        0.5.dp,
                        if (isDragging) KinetixTokens.Colors.Workout.copy(alpha = 0.6f) else KinetixTokens.Colors.Hairline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInspect(ex) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ex.exerciseName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = KinetixTokens.Colors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            val setsCount = if (ex.targetSets > 0) ex.targetSets else prescriptionSets
                            val repsLabel = when {
                                ex.repMin > 0 && ex.repMax > 0 && ex.repMin == ex.repMax -> "${ex.repMin}"
                                ex.repMin > 0 && ex.repMax > 0 -> "${ex.repMin}–${ex.repMax}"
                                else -> prescriptionReps
                            }
                            val restLabel = if (ex.restSeconds > 0) {
                                val mins = ex.restSeconds / 60
                                val rem = ex.restSeconds % 60
                                val formatted = when {
                                    mins >= 2 && rem == 0 -> "${mins}m"
                                    mins >= 2 -> "${mins}m ${rem}s"
                                    else -> "${ex.restSeconds}s"
                                }
                                " · $formatted rest"
                            } else {
                                ""
                            }
                            Text(
                                text = "${ex.muscleGroup} · $setsCount × $repsLabel$restLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KinetixTokens.Colors.TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = "Drag to reorder ${ex.exerciseName}",
                            tint = KinetixTokens.Colors.TextSecondary,
                            modifier = Modifier
                                .size(36.dp)
                                .padding(6.dp)
                                .pointerInput(ex.id, localList.size) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            draggingId = ex.id
                                            dragOffsetY = 0f
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        },
                                        onDragEnd = {
                                            draggingId = null
                                            dragOffsetY = 0f
                                            onReordered(localList.map { it.id })
                                        },
                                        onDragCancel = {
                                            draggingId = null
                                            dragOffsetY = 0f
                                            localList = exercises
                                        },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount
                                            val curIdx = localList.indexOfFirst { it.id == ex.id }
                                            if (curIdx != -1) {
                                                val stepsMoved = (dragOffsetY / rowHeightPx).toInt()
                                                if (stepsMoved != 0) {
                                                    val targetIdx = (curIdx + stepsMoved).coerceIn(0, localList.lastIndex)
                                                    if (targetIdx != curIdx) {
                                                        val mutable = localList.toMutableList()
                                                        val item = mutable.removeAt(curIdx)
                                                        mutable.add(targetIdx, item)
                                                        localList = mutable
                                                        dragOffsetY -= stepsMoved * rowHeightPx
                                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-Screen Customizable Active Workout Session:
 * - Large exercise name & simple muscle readout
 * - Exercise switcher strip + "+ Add" exercise
 * - Animated Rest Timer Ring (pause/resume, -15s, +15s, quick presets, Skip)
 * - Big Weight & Reps Steppers with customizable per-exercise step increments & targets
 * - Warm-up toggle, per-exercise Customize BottomSheet, haptic "Complete set", and strong finish haptic
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveWorkoutFullScreen(
    state: WorkoutUiState,
    bottomClearance: androidx.compose.ui.unit.Dp,
    onSelectExercise: (String) -> Unit,
    onUpdateConfig: (String, ExerciseSessionConfig) -> Unit,
    onSkipExercise: (String) -> Unit,
    onInspectExercise: (String, String, String, String) -> Unit,
    onLogSet: (String, String, String, String, Int, Double, Int, Boolean, Boolean) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onToggleRestPause: () -> Unit,
    onAdjustRestTimer: (Int) -> Unit,
    onStopRestTimer: () -> Unit,
    onStartRestTimer: (Int) -> Unit,
    onOpenPicker: () -> Unit,
    onConfirmMakePermanent: (ExerciseEntity) -> Unit,
    onDismissMakePermanent: () -> Unit,
    onFinishWorkout: () -> Unit,
    onDiscardWorkout: () -> Unit,
    onRefreshMedia: () -> Unit,
    onOpenListenerSettings: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaSeekTo: (Long) -> Unit
) {
    val view = LocalView.current
    val session = state.activeSession ?: return
    val currentBlock = state.activeBlocks.firstOrNull { it.exerciseId == state.selectedActiveExerciseId }
        ?: state.activeBlocks.firstOrNull()

    var elapsedSec by remember(session.id) {
        mutableIntStateOf(((System.currentTimeMillis() - session.startedAtEpochMs) / 1000L).toInt().coerceAtLeast(0))
    }
    LaunchedEffect(session.id) {
        onRefreshMedia()
        while (true) {
            delay(1_000L)
            elapsedSec = ((System.currentTimeMillis() - session.startedAtEpochMs) / 1000L).toInt().coerceAtLeast(0)
        }
    }

    var customizingBlockId by remember { mutableStateOf<String?>(null) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    BackHandler { showDiscardConfirm = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KinetixTokens.Colors.Background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = KinetixTokens.Spacing.ScreenHorizontal,
                    top = 12.dp,
                    end = KinetixTokens.Spacing.ScreenHorizontal,
                    bottom = bottomClearance
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Session Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.splitDayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = KinetixTokens.Colors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val mins = elapsedSec / 60
                    val secs = elapsedSec % 60
                    Text(
                        text = "%02d:%02d elapsed · ${state.activeBlocks.sumOf { it.loggedSets.size }} sets logged".format(mins, secs),
                        style = MaterialTheme.typography.labelMedium,
                        color = KinetixTokens.Colors.TextSecondary
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showDiscardConfirm = true }) {
                        Text("End", color = KinetixTokens.Colors.StatusLowFg)
                    }
                    PrimaryButton(
                        text = "Finish",
                        onClick = onFinishWorkout,
                        strongHaptic = true,
                        modifier = Modifier.testTag("finish_workout_btn")
                    )
                }
            }

            // Slim music control
            MiniMediaPlayerBar(
                mediaState = state.mediaState,
                onPlayPause = onMediaPlayPause,
                onPrevious = onMediaPrevious,
                onNext = onMediaNext,
                onSeekTo = onMediaSeekTo,
                onOpenListenerSettings = onOpenListenerSettings
            )

            // Pending Make Permanent banner for ad-hoc exercises
            AnimatedVisibility(visible = state.pendingMakePermanentExercise != null) {
                state.pendingMakePermanentExercise?.let { ex ->
                    Card(containerColor = KinetixTokens.Colors.NestedSurface) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Added \"${ex.name}\" for today",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = KinetixTokens.Colors.TextPrimary
                                )
                                Text(
                                    text = "Save it to this split day?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KinetixTokens.Colors.TextSecondary
                                )
                            }
                            Row {
                                TextButton(onClick = onDismissMakePermanent) { Text("Today only") }
                                TextButton(
                                    onClick = { onConfirmMakePermanent(ex) },
                                    modifier = Modifier.testTag("make_permanent_btn")
                                ) {
                                    Text("Make Permanent", color = KinetixTokens.Colors.Workout)
                                }
                            }
                        }
                    }
                }
            }

            // Exercise switcher strip + "+ Add" button
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(state.activeBlocks, key = { it.exerciseId }) { block ->
                    val isSelected = currentBlock?.exerciseId == block.exerciseId
                    val doneCount = block.loggedSets.size
                    val target = block.config.targetSets
                    Surface(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSelectExercise(block.exerciseId)
                        },
                        shape = KinetixTokens.Radius.PillShape,
                        color = if (isSelected) KinetixTokens.Colors.TextPrimary else KinetixTokens.Colors.CardSurface,
                        border = BorderStroke(
                            0.5.dp,
                            if (doneCount >= target) KinetixTokens.Colors.Workout.copy(alpha = 0.6f)
                            else KinetixTokens.Colors.Hairline
                        ),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (doneCount >= target) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = if (isSelected) KinetixTokens.Colors.Background else KinetixTokens.Colors.Workout,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = "${block.exerciseName} ($doneCount/$target)",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) KinetixTokens.Colors.Background else KinetixTokens.Colors.TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
                item(key = "add_exercise_pill") {
                    Surface(
                        onClick = onOpenPicker,
                        shape = KinetixTokens.Radius.PillShape,
                        color = KinetixTokens.Colors.NestedSurface,
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("open_exercise_picker_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Add Exercise",
                                tint = KinetixTokens.Colors.Workout,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelLarge,
                                color = KinetixTokens.Colors.Workout
                            )
                        }
                    }
                }
            }

            if (currentBlock == null) {
                Card {
                    EmptyState(
                        icon = Icons.Rounded.FitnessCenter,
                        message = "No exercises in this session yet.",
                        accentColor = KinetixTokens.Colors.Workout,
                        actionLabel = "Add Exercise",
                        onAction = onOpenPicker
                    )
                }
            } else {
                FocusedExerciseSessionCard(
                    block = currentBlock,
                    restTimer = state.restTimer,
                    onInspect = {
                        onInspectExercise(
                            currentBlock.exerciseId,
                            currentBlock.exerciseName,
                            currentBlock.muscleGroup,
                            currentBlock.muscleHead
                        )
                    },
                    onOpenCustomize = { customizingBlockId = currentBlock.exerciseId },
                    onToggleWarmup = {
                        onUpdateConfig(
                            currentBlock.exerciseId,
                            currentBlock.config.copy(isWarmupNext = !currentBlock.config.isWarmupNext)
                        )
                    },
                    onLogSet = { setNumber, weight, reps ->
                        onLogSet(
                            currentBlock.exerciseId,
                            currentBlock.exerciseName,
                            currentBlock.muscleGroup,
                            currentBlock.muscleHead,
                            setNumber,
                            weight,
                            reps,
                            currentBlock.isAdHoc,
                            currentBlock.config.isWarmupNext
                        )
                    },
                    onDeleteSet = onDeleteSet,
                    onToggleRestPause = onToggleRestPause,
                    onAdjustRestTimer = onAdjustRestTimer,
                    onStopRestTimer = onStopRestTimer,
                    onStartRestTimer = onStartRestTimer
                )
            }
        }
    }

    customizingBlockId?.let { id ->
        val block = state.activeBlocks.firstOrNull { it.exerciseId == id }
        if (block != null) {
            CustomizeExerciseSheet(
                block = block,
                onSaveConfig = { updated -> onUpdateConfig(block.exerciseId, updated) },
                onSkipToday = {
                    onSkipExercise(block.exerciseId)
                    customizingBlockId = null
                },
                onDismiss = { customizingBlockId = null }
            )
        }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            containerColor = KinetixTokens.Colors.CardSurface,
            title = { Text("End Workout Early?") },
            text = { Text("Finish and save logged sets, or discard this session.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirm = false
                        onFinishWorkout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KinetixTokens.Colors.Workout,
                        contentColor = KinetixTokens.Colors.OnWorkout
                    )
                ) { Text("Finish & Save") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showDiscardConfirm = false
                            onDiscardWorkout()
                        }
                    ) { Text("Discard", color = KinetixTokens.Colors.StatusLowFg) }
                    TextButton(onClick = { showDiscardConfirm = false }) { Text("Resume") }
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusedExerciseSessionCard(
    block: ActiveExerciseBlock,
    restTimer: RestTimerState,
    onInspect: () -> Unit,
    onOpenCustomize: () -> Unit,
    onToggleWarmup: () -> Unit,
    onLogSet: (Int, Double, Int) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onToggleRestPause: () -> Unit,
    onAdjustRestTimer: (Int) -> Unit,
    onStopRestTimer: () -> Unit,
    onStartRestTimer: (Int) -> Unit
) {
    val view = LocalView.current
    var weightKg by remember(block.exerciseId, block.suggestedWeightKg) {
        mutableStateOf(block.suggestedWeightKg)
    }
    var reps by remember(block.exerciseId, block.suggestedReps) {
        mutableIntStateOf(block.suggestedReps)
    }

    Card {
        // Large Exercise Name + Customize & Anatomy Detail Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.exerciseName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = KinetixTokens.Colors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip(
                        text = block.muscleGroup,
                        foregroundColor = KinetixTokens.Colors.TextPrimary,
                        backgroundColor = KinetixTokens.Colors.NestedSurface
                    )
                    Text(
                        text = "${block.config.targetSets} sets × ${block.config.repMin}–${block.config.repMax} · ${block.config.restSeconds}s rest",
                        style = MaterialTheme.typography.labelMedium,
                        color = KinetixTokens.Colors.TextSecondary
                    )
                }
            }
            Row {
                IconButton(onClick = onInspect) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Exercise details",
                        tint = KinetixTokens.Colors.TextSecondary
                    )
                }
                IconButton(
                    onClick = onOpenCustomize,
                    modifier = Modifier.testTag("customize_exercise_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Customize exercise",
                        tint = KinetixTokens.Colors.Workout
                    )
                }
            }
        }

        if (block.config.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = block.config.note,
                style = MaterialTheme.typography.bodyMedium,
                color = KinetixTokens.Colors.Hydration
            )
        }

        if (block.overloadSuggestion != null) {
            Spacer(Modifier.height(10.dp))
            val isBoost = block.overloadSuggestion.shouldIncrease
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KinetixTokens.Radius.NestedShape)
                    .background(if (isBoost) KinetixTokens.Colors.WorkoutMutedBg else KinetixTokens.Colors.NestedSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                    contentDescription = null,
                    tint = if (isBoost) KinetixTokens.Colors.Workout else KinetixTokens.Colors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = block.overloadSuggestion.reason,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isBoost) KinetixTokens.Colors.Workout else KinetixTokens.Colors.TextSecondary
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        // Rest Timer Ring Card (always customizable right inside active session)
        val timerActive = restTimer.isRunning || restTimer.isPaused || restTimer.remainingSeconds > 0
        val timerProgress = if (timerActive && restTimer.totalSeconds > 0) {
            restTimer.remainingSeconds.toFloat() / restTimer.totalSeconds.toFloat()
        } else {
            1f
        }
        Surface(
            shape = KinetixTokens.Radius.NestedShape,
            color = KinetixTokens.Colors.NestedSurface,
            border = BorderStroke(0.5.dp, KinetixTokens.Colors.Hairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RingProgress(
                        progress = timerProgress,
                        color = if (timerActive) KinetixTokens.Colors.Hydration else KinetixTokens.Colors.TextTertiary,
                        size = 58.dp,
                        strokeWidth = 6.dp
                    ) {
                        val disp = if (timerActive) restTimer.remainingSeconds else block.config.restSeconds
                        Text(
                            text = "%d:%02d".format(disp / 60, disp % 60),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = KinetixTokens.Colors.TextPrimary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when {
                                restTimer.isRunning -> "Resting"
                                restTimer.isPaused -> "Timer paused"
                                else -> "Rest timer ready"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = KinetixTokens.Colors.TextPrimary
                        )
                        Text(
                            text = "Set #${block.loggedSets.size + 1} · ${block.config.restSeconds}s preset",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KinetixTokens.Colors.TextSecondary
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (timerActive) {
                        IconButton(onClick = { onAdjustRestTimer(-15) }) {
                            Text("-15", style = MaterialTheme.typography.labelLarge, color = KinetixTokens.Colors.TextSecondary)
                        }
                        IconButton(onClick = onToggleRestPause) {
                            Icon(
                                imageVector = if (restTimer.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = if (restTimer.isPaused) "Resume timer" else "Pause timer",
                                tint = KinetixTokens.Colors.Hydration
                            )
                        }
                        IconButton(onClick = { onAdjustRestTimer(15) }) {
                            Text("+15", style = MaterialTheme.typography.labelLarge, color = KinetixTokens.Colors.TextSecondary)
                        }
                        IconButton(onClick = onStopRestTimer) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Skip rest", tint = KinetixTokens.Colors.TextSecondary)
                        }
                    } else {
                        TextButton(onClick = { onStartRestTimer(block.config.restSeconds) }) {
                            Text("Start", color = KinetixTokens.Colors.Hydration)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        // Big Weight & Reps Steppers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Stepper(
                label = "Weight (±${block.config.weightStepKg} kg)",
                valueText = if (weightKg % 1.0 == 0.0) "${weightKg.toInt()} kg" else "$weightKg kg",
                onDecrement = { weightKg = (weightKg - block.config.weightStepKg).coerceAtLeast(0.0) },
                onIncrement = { weightKg += block.config.weightStepKg },
                accentColor = KinetixTokens.Colors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Stepper(
                label = "Reps",
                valueText = "$reps",
                onDecrement = { reps = (reps - 1).coerceAtLeast(1) },
                onIncrement = { reps += 1 },
                accentColor = KinetixTokens.Colors.Workout,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))
        // Warm-up vs Working quick toggle + Complete Set primary CTA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = block.config.isWarmupNext,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onToggleWarmup()
                },
                label = { Text(if (block.config.isWarmupNext) "Warm-up" else "Working") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = KinetixTokens.Colors.HydrationMutedBg,
                    selectedLabelColor = KinetixTokens.Colors.Hydration
                )
            )
            PrimaryButton(
                text = "Complete set",
                icon = Icons.Rounded.Check,
                onClick = { onLogSet(block.loggedSets.size + 1, weightKg, reps) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("log_set_${block.exerciseId}")
            )
        }

        if (block.loggedSets.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(thickness = 0.5.dp, color = KinetixTokens.Colors.Hairline)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                block.loggedSets.forEach { set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KinetixTokens.Colors.NestedSurface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (set.isWarmup) "Warm-up" else "Set ${set.setNumber}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (set.isWarmup) KinetixTokens.Colors.Hydration else KinetixTokens.Colors.Workout
                            )
                            Text(
                                text = "${set.weightKg} kg × ${set.reps} reps",
                                style = MaterialTheme.typography.bodyLarge,
                                color = KinetixTokens.Colors.TextPrimary
                            )
                        }
                        IconButton(
                            onClick = { onDeleteSet(set.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Delete set",
                                tint = KinetixTokens.Colors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizeExerciseSheet(
    block: ActiveExerciseBlock,
    onSaveConfig: (ExerciseSessionConfig) -> Unit,
    onSkipToday: () -> Unit,
    onDismiss: () -> Unit
) {
    var cfg by remember(block.exerciseId) { mutableStateOf(block.config) }
    val stepOptions = listOf(0.5, 1.25, 2.5, 5.0)

    BottomSheet(
        title = "Customize Exercise",
        subtitle = block.exerciseName,
        largeDetent = true,
        headerAction = {
            TextButton(
                onClick = {
                    onSaveConfig(cfg)
                    onDismiss()
                }
            ) {
                Text("Done", color = KinetixTokens.Colors.Workout)
            }
        },
        onDismiss = {
            onSaveConfig(cfg)
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stepper(
                    label = "Target Sets",
                    valueText = "${cfg.targetSets}",
                    onDecrement = { cfg = cfg.copy(targetSets = (cfg.targetSets - 1).coerceAtLeast(1)) },
                    onIncrement = { cfg = cfg.copy(targetSets = (cfg.targetSets + 1).coerceAtMost(12)) },
                    modifier = Modifier.weight(1f)
                )
                Stepper(
                    label = "Rest (sec)",
                    valueText = "${cfg.restSeconds}s",
                    onDecrement = { cfg = cfg.copy(restSeconds = (cfg.restSeconds - 15).coerceAtLeast(15)) },
                    onIncrement = { cfg = cfg.copy(restSeconds = (cfg.restSeconds + 15).coerceAtMost(360)) },
                    accentColor = KinetixTokens.Colors.Hydration,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stepper(
                    label = "Min Reps",
                    valueText = "${cfg.repMin}",
                    onDecrement = { cfg = cfg.copy(repMin = (cfg.repMin - 1).coerceAtLeast(1)) },
                    onIncrement = {
                        val next = (cfg.repMin + 1).coerceAtMost(cfg.repMax)
                        cfg = cfg.copy(repMin = next)
                    },
                    modifier = Modifier.weight(1f)
                )
                Stepper(
                    label = "Max Reps",
                    valueText = "${cfg.repMax}",
                    onDecrement = {
                        val next = (cfg.repMax - 1).coerceAtLeast(cfg.repMin)
                        cfg = cfg.copy(repMax = next)
                    },
                    onIncrement = { cfg = cfg.copy(repMax = (cfg.repMax + 1).coerceAtMost(40)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Column {
                Text(
                    text = "Weight Step Increment",
                    style = MaterialTheme.typography.labelLarge,
                    color = KinetixTokens.Colors.TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                SegmentedControl(
                    options = stepOptions,
                    selected = cfg.weightStepKg,
                    labelFor = { "${it} kg" },
                    onSelect = { cfg = cfg.copy(weightStepKg = it) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KinetixTokens.Radius.NestedShape)
                    .background(KinetixTokens.Colors.NestedSurface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-start rest timer",
                        style = MaterialTheme.typography.bodyLarge,
                        color = KinetixTokens.Colors.TextPrimary
                    )
                    Text(
                        text = "Automatically begin ${cfg.restSeconds}s countdown after completing a set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KinetixTokens.Colors.TextSecondary
                    )
                }
                Switch(
                    checked = cfg.autoStartRest,
                    onCheckedChange = { cfg = cfg.copy(autoStartRest = it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = KinetixTokens.Colors.TextPrimary,
                        checkedTrackColor = KinetixTokens.Colors.Workout
                    )
                )
            }

            OutlinedTextField(
                value = cfg.note,
                onValueChange = { cfg = cfg.copy(note = it.take(80)) },
                label = { Text("Session cue or note (optional)") },
                placeholder = { Text("e.g. 2s pause on chest, tempo 3-1-1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = onSkipToday,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip Exercise Today", color = KinetixTokens.Colors.StatusLowFg)
            }
        }
    }
}

@Composable
private fun EditSplitDayDialog(
    day: SplitDayEntity,
    exercises: List<SplitExerciseEntity>,
    onSaveDayMeta: (String, String) -> Unit,
    onAddExercise: () -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(day.id) { mutableStateOf(day.name) }
    var muscles by remember(day.id) { mutableStateOf(day.targetMusclesCsv) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KinetixTokens.Colors.CardSurface,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Exercises (${exercises.size})", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onAddExercise) {
                        Text("+ Add", color = KinetixTokens.Colors.Workout)
                    }
                }
                exercises.forEach { ex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KinetixTokens.Colors.NestedSurface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ex.exerciseName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = KinetixTokens.Colors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveExercise(ex.id) }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Remove ${ex.exerciseName}",
                                tint = KinetixTokens.Colors.StatusLowFg,
                                modifier = Modifier.size(16.dp)
                            )
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = KinetixTokens.Colors.Workout,
                    contentColor = KinetixTokens.Colors.OnWorkout
                )
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
