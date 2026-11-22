package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.ExerciseEntity
import com.example.ui.components.BottomSheet
import com.example.ui.components.Card
import com.example.ui.components.EmptyState
import com.example.ui.components.StatChip
import com.example.ui.theme.KinetixTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExercisePickerSheet(
    muscleGroups: List<String>,
    muscleHeads: List<String>,
    selectedMuscle: String,
    selectedHead: String,
    query: String,
    exercises: List<ExerciseEntity>,
    onSelectMuscle: (String) -> Unit,
    onSelectHead: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onAddExercise: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    var largeDetent by remember { mutableStateOf(false) }

    BottomSheet(
        title = "Add Exercise",
        subtitle = "Filter by muscle or head · Tap + to add",
        largeDetent = largeDetent,
        headerAction = {
            TextButton(onClick = { largeDetent = !largeDetent }) {
                Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (largeDetent) "Medium" else "Expand")
            }
        },
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("Search offline DB (e.g. incline, row, calves)") },
            singleLine = true,
            shape = KinetixTokens.Radius.NestedShape,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("exercise_picker_search")
        )

        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(muscleGroups, key = { it }) { muscle ->
                FilterChip(
                    selected = selectedMuscle == muscle,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onSelectMuscle(muscle)
                    },
                    label = { Text(muscle) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KinetixTokens.Colors.WorkoutMutedBg,
                        selectedLabelColor = KinetixTokens.Colors.Workout
                    )
                )
            }
        }

        if (muscleHeads.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(muscleHeads, key = { it }) { head ->
                    FilterChip(
                        selected = selectedHead == head,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSelectHead(head)
                        },
                        label = { Text(head) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KinetixTokens.Colors.HydrationMutedBg,
                            selectedLabelColor = KinetixTokens.Colors.Hydration
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        if (exercises.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Search,
                message = "No matching exercises in offline library."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (largeDetent) 480.dp else 330.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(exercises, key = { it.id }) { exercise ->
                    Card(
                        containerColor = KinetixTokens.Colors.NestedSurface,
                        contentPadding = 12.dp,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onAddExercise(exercise)
                            },
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onAddExercise(exercise)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragIndicator,
                                contentDescription = "Long press to add",
                                tint = KinetixTokens.Colors.TextTertiary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = KinetixTokens.Colors.TextPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatChip(
                                        text = exercise.muscleGroup,
                                        foregroundColor = KinetixTokens.Colors.TextPrimary,
                                        backgroundColor = KinetixTokens.Colors.CardSurface
                                    )
                                    Text(
                                        text = "${exercise.muscleHead} · ${exercise.equipment}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KinetixTokens.Colors.TextSecondary
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onAddExercise(exercise)
                                },
                                modifier = Modifier.semantics { contentDescription = "Add ${exercise.name}" }
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = KinetixTokens.Colors.Workout
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
