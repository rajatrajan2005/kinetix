package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.ExerciseEntity
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Muscle-Targeted Exercise Picker",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Text(
                text = "Long-press or tap + to add to today's workout without altering your saved split.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search offline DB (e.g. incline, triceps, row)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exercise_picker_search")
            )

            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(muscleGroups, key = { it }) { muscle ->
                    FilterChip(
                        selected = selectedMuscle == muscle,
                        onClick = { onSelectMuscle(muscle) },
                        label = { Text(muscle) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VoltContainer,
                            selectedLabelColor = ElectricVolt
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
                            onClick = { onSelectHead(head) },
                            label = { Text(head) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanContainer,
                                selectedLabelColor = GlacialCyan
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(exercises, key = { it.id }) { exercise ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onAddExercise(exercise) },
                                onLongClick = { onAddExercise(exercise) }
                            ),
                        shape = RoundedCornerShape(14.dp),
                        color = CarbonSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "Long press to add",
                                tint = TextMuted
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${exercise.muscleGroup} · ${exercise.muscleHead} · ${exercise.equipment}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            IconButton(
                                onClick = { onAddExercise(exercise) },
                                modifier = Modifier.semantics { contentDescription = "Add ${exercise.name}" }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = ElectricVolt)
                            }
                        }
                    }
                }
            }
        }
    }
}
