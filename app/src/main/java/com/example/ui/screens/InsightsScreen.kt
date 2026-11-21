package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.example.domain.model.VolumeStatus
import com.example.ui.components.KinetixAmbientBackdrop
import com.example.ui.components.KinetixCard
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.TodayUiState
import com.example.ui.viewmodel.WorkoutUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InsightsScreen(
    todayState: TodayUiState,
    workoutState: WorkoutUiState
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d · HH:mm") }

    KinetixAmbientBackdrop(
        primaryOrb = ElectricVolt,
        secondaryOrb = EmeraldPulse,
        tertiaryOrb = GlacialCyan
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "insights_header") {
                Column {
                    Text("ANALYTICS", style = MaterialTheme.typography.labelMedium, color = ElectricVolt)
                    Text("Volume & History", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
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

            item(key = "steps_history_chart") {
                KinetixCard(accentGlow = EmeraldPulse) {
                    Text("Recent Daily Steps", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    val records = todayState.recentSteps.reversed()
                    if (records.isEmpty()) {
                        Text("Walk with Kinetix enabled to populate daily step bars.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    } else {
                        val maxSteps = maxOf(todayState.stepGoal.toLong(), records.maxOf { it.stepsToday }, 1L)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        ) {
                            val count = records.size
                            val slotWidth = size.width / count.coerceAtLeast(1)
                            val barWidth = (slotWidth * 0.6f).coerceAtMost(28.dp.toPx())
                            records.forEachIndexed { idx, rec ->
                                val ratio = (rec.stepsToday.toFloat() / maxSteps.toFloat()).coerceIn(0.05f, 1f)
                                val barHeight = size.height * ratio
                                val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                                drawRoundRect(
                                    color = if (rec.stepsToday >= todayState.stepGoal) ElectricVolt else EmeraldPulse,
                                    topLeft = Offset(x, size.height - barHeight),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
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
    }
}
