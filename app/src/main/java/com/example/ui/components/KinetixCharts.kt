package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.ExerciseRingGreen
import com.example.ui.theme.GlassRimHighlight
import com.example.ui.theme.MoveRingRed
import com.example.ui.theme.StandRingCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Apple Fitness-inspired 3 concentric glowing Activity Rings:
 * - Outer Ring (#FA114F): Move / Calorie Intake vs Target
 * - Middle Ring (#A6FF00): Exercise / Protein Target
 * - Inner Ring (#00F5EA): Stand / Daily Steps + Hydration combined
 */
@Composable
fun AppleFitnessRingsHero(
    moveProgress: Float,
    moveSummary: String,
    exerciseProgress: Float,
    exerciseSummary: String,
    standProgress: Float,
    standSummary: String,
    modifier: Modifier = Modifier
) {
    val animMove by animateFloatAsState(
        targetValue = moveProgress.coerceIn(0f, 1.5f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ringMove"
    )
    val animExercise by animateFloatAsState(
        targetValue = exerciseProgress.coerceIn(0f, 1.5f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ringExercise"
    )
    val animStand by animateFloatAsState(
        targetValue = standProgress.coerceIn(0f, 1.5f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ringStand"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("apple_fitness_rings_hero"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(144.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 13.dp.toPx()
                val gap = 4.dp.toPx()
                val radii = listOf(
                    (size.minDimension / 2f) - stroke / 2f,
                    (size.minDimension / 2f) - stroke * 1.5f - gap,
                    (size.minDimension / 2f) - stroke * 2.5f - gap * 2f
                )
                val specs = listOf(
                    Triple(radii[0], MoveRingRed, animMove),
                    Triple(radii[1], ExerciseRingGreen, animExercise),
                    Triple(radii[2], StandRingCyan, animStand)
                )
                val center = Offset(size.width / 2f, size.height / 2f)

                specs.forEach { (radius, color, progress) ->
                    val topLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = Size(radius * 2f, radius * 2f)
                    // Track
                    drawArc(
                        color = color.copy(alpha = 0.18f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    // Soft ambient glow
                    val sweep = (progress.coerceAtMost(1f) * 360f).coerceAtLeast(if (progress > 0f) 4f else 0f)
                    if (sweep > 0f) {
                        drawArc(
                            color = color.copy(alpha = 0.25f),
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke + 5.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = color,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RingLegendMetric("FUEL · MOVE", moveSummary, MoveRingRed, moveProgress)
            RingLegendMetric("PROTEIN · TRAIN", exerciseSummary, ExerciseRingGreen, exerciseProgress)
            RingLegendMetric("STEPS & WATER", standSummary, StandRingCyan, standProgress)
        }
    }
}

@Composable
private fun RingLegendMetric(
    label: String,
    valueText: String,
    color: Color,
    progress: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(
                "${(progress * 100f).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Immutable
data class PieSliceData(
    val label: String,
    val value: Float,
    val valueText: String,
    val color: Color
)

/**
 * Interactive Donut / Pie Chart with touch slice selection, animated radial expansion,
 * center summary readout, and interactive legend chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractivePieChart(
    slices: List<PieSliceData>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    chartSize: Dp = 186.dp
) {
    val validSlices = remember(slices) { slices.filter { it.value > 0f } }
    if (validSlices.isEmpty()) {
        Text(emptyMessage, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        return
    }

    var selectedIdx by remember(validSlices) { mutableIntStateOf(0) }
    val safeSelectedIdx = selectedIdx.coerceIn(0, validSlices.lastIndex)
    val total = remember(validSlices) { validSlices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f) }
    val selectedSlice = validSlices[safeSelectedIdx]
    val selectedPct = ((selectedSlice.value / total) * 100f).roundToInt()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(chartSize),
            contentAlignment = Alignment.Center
        ) {
            val animExpansion by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "pieSelection_$safeSelectedIdx"
            )
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(validSlices, total) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = hypot(dx, dy)
                            val outerR = min(cx, cy)
                            if (dist in (outerR * 0.25f)..(outerR * 1.05f)) {
                                // Start angle is -90f (top) clockwise
                                val angleDeg = ((atan2(dy, dx) * 180.0 / PI) + 90.0 + 360.0) % 360.0
                                var accum = 0.0
                                validSlices.forEachIndexed { idx, s ->
                                    val sweep = (s.value / total) * 360.0
                                    if (angleDeg >= accum && angleDeg <= accum + sweep) {
                                        selectedIdx = idx
                                        return@detectTapGestures
                                    }
                                    accum += sweep
                                }
                            }
                        }
                    }
            ) {
                val baseStroke = 26.dp.toPx()
                val selectedExtra = 7.dp.toPx() * animExpansion
                val radius = (size.minDimension / 2f) - baseStroke / 2f - 8.dp.toPx()
                val topLeft = Offset(size.width / 2f - radius, size.height / 2f - radius)
                val arcSize = Size(radius * 2f, radius * 2f)

                var startAngle = -90f
                validSlices.forEachIndexed { idx, slice ->
                    val sweep = (slice.value / total) * 360f
                    val gap = if (validSlices.size > 1) 3f else 0f
                    val drawSweep = (sweep - gap).coerceAtLeast(1f)
                    val isSel = idx == safeSelectedIdx
                    if (isSel) {
                        drawArc(
                            color = slice.color.copy(alpha = 0.28f),
                            startAngle = startAngle,
                            sweepAngle = drawSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = baseStroke + selectedExtra + 8.dp.toPx(), cap = StrokeCap.Butt)
                        )
                    }
                    drawArc(
                        color = if (isSel) slice.color else slice.color.copy(alpha = 0.72f),
                        startAngle = startAngle,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = if (isSel) baseStroke + selectedExtra else baseStroke,
                            cap = StrokeCap.Butt
                        )
                    )
                    startAngle += sweep
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "$selectedPct%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = selectedSlice.color
                )
                Text(
                    text = selectedSlice.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = selectedSlice.valueText,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            validSlices.forEachIndexed { idx, slice ->
                val selected = idx == safeSelectedIdx
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) slice.color.copy(alpha = 0.20f) else CarbonSurface,
                    border = BorderStroke(
                        1.dp,
                        if (selected) slice.color else GlassRimHighlight.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.clickable { selectedIdx = idx }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${slice.label} · ${slice.valueText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) TextPrimary else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Immutable
data class BarEntry(
    val label: String,
    val value: Float,
    val valueText: String
)

/**
 * Interactive Bar Chart with dashed goal reference line and tap-to-inspect bar values.
 */
@Composable
fun InteractiveBarChart(
    entries: List<BarEntry>,
    goalValue: Float,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Text(emptyMessage, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        return
    }

    var selectedIndex by remember(entries) { mutableIntStateOf(entries.lastIndex) }
    val safeIndex = selectedIndex.coerceIn(0, entries.lastIndex)
    val selected = entries[safeIndex]
    val maxValue = remember(entries, goalValue) {
        maxOf(goalValue, entries.maxOf { it.value }, 1f)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selected.label}: ${selected.valueText}",
                style = MaterialTheme.typography.labelMedium,
                color = ElectricVolt
            )
            Text(
                text = "Tap any bar to inspect",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Spacer(Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .pointerInput(entries) {
                    detectTapGestures { offset ->
                        val slotW = size.width / entries.size.coerceAtLeast(1)
                        val tapped = (offset.x / slotW).toInt().coerceIn(0, entries.lastIndex)
                        selectedIndex = tapped
                    }
                }
        ) {
            val goalRatio = (goalValue / maxValue).coerceIn(0f, 1f)
            val goalY = size.height * (1f - goalRatio)
            drawLine(
                color = StandRingCyan.copy(alpha = 0.45f),
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )

            val slotWidth = size.width / entries.size.coerceAtLeast(1)
            val barWidth = (slotWidth * 0.58f).coerceAtMost(28.dp.toPx())
            entries.forEachIndexed { idx, item ->
                val ratio = (item.value / maxValue).coerceIn(0.06f, 1f)
                val barHeight = size.height * ratio
                val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                val isGoalMet = item.value >= goalValue
                val isSelected = idx == safeIndex
                val baseColor = if (isGoalMet) ElectricVolt else StandRingCyan
                val barColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.6f)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            entries.forEachIndexed { idx, item ->
                val isSel = idx == safeIndex
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSel) ElectricVolt else TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Decodes an image stored in app-private filesDir without blocking UI composition.
 */
@Composable
fun LocalPhotoImage(
    filePath: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = filePath) {
        value = if (filePath.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val f = File(filePath)
                    if (!f.exists()) null else BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
                }.getOrNull()
            }
        }
    }

    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        fallback()
    }
}
