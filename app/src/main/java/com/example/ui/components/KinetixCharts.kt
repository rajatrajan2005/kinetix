package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.example.domain.model.MuscleWeeklyVolume
import com.example.domain.model.VolumeStatus
import com.example.ui.theme.KinetixTokens
import com.example.ui.theme.rememberReduceAnimations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Apple Fitness-inspired 3 concentric Activity Rings with spring entrance on load
 * and large numeric readouts beside them:
 * - Outer Ring (#FA114F): Calories
 * - Middle Ring (#A6FF00): Protein
 * - Inner Ring (#1EEAEF): Water & Steps
 */
@Composable
fun AppleFitnessRingsHero(
    moveProgress: Float,
    moveValueLarge: String,
    moveTargetLabel: String,
    exerciseProgress: Float,
    exerciseValueLarge: String,
    exerciseTargetLabel: String,
    standProgress: Float,
    standValueLarge: String,
    standTargetLabel: String,
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberReduceAnimations()
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { loaded = true }

    val animMove by animateFloatAsState(
        targetValue = if (loaded || reduceMotion) moveProgress.coerceIn(0f, 1.25f) else 0f,
        animationSpec = KinetixTokens.Motion.ringSpring(reduceMotion),
        label = "ringMove"
    )
    val animExercise by animateFloatAsState(
        targetValue = if (loaded || reduceMotion) exerciseProgress.coerceIn(0f, 1.25f) else 0f,
        animationSpec = KinetixTokens.Motion.ringSpring(reduceMotion),
        label = "ringExercise"
    )
    val animStand by animateFloatAsState(
        targetValue = if (loaded || reduceMotion) standProgress.coerceIn(0f, 1.25f) else 0f,
        animationSpec = KinetixTokens.Motion.ringSpring(reduceMotion),
        label = "ringStand"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("apple_fitness_rings_hero"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(152.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                val gap = 4.dp.toPx()
                val baseR = size.minDimension / 2f
                val radii = listOf(
                    baseR - stroke / 2f,
                    baseR - stroke * 1.5f - gap,
                    baseR - stroke * 2.5f - gap * 2f
                )
                val specs = listOf(
                    Triple(radii[0], KinetixTokens.Colors.Calories, animMove),
                    Triple(radii[1], KinetixTokens.Colors.Workout, animExercise),
                    Triple(radii[2], KinetixTokens.Colors.Hydration, animStand)
                )
                val center = Offset(size.width / 2f, size.height / 2f)

                specs.forEach { (radius, color, progress) ->
                    val topLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = Size(radius * 2f, radius * 2f)
                    drawArc(
                        color = color.copy(alpha = 0.16f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    val sweep = (progress.coerceAtMost(1f) * 360f)
                        .coerceAtLeast(if (progress > 0f) 4f else 0f)
                    if (sweep > 0f) {
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

        Spacer(Modifier.width(18.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HeroRingMetric(
                label = "Calories",
                largeValue = moveValueLarge,
                targetLabel = moveTargetLabel,
                color = KinetixTokens.Colors.Calories
            )
            HeroRingMetric(
                label = "Protein",
                largeValue = exerciseValueLarge,
                targetLabel = exerciseTargetLabel,
                color = KinetixTokens.Colors.Protein
            )
            HeroRingMetric(
                label = "Water & Steps",
                largeValue = standValueLarge,
                targetLabel = standTargetLabel,
                color = KinetixTokens.Colors.Hydration
            )
        }
    }
}

@Composable
private fun HeroRingMetric(
    label: String,
    largeValue: String,
    targetLabel: String,
    color: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = KinetixTokens.Colors.TextSecondary
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = largeValue,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = color,
                maxLines = 1
            )
            Text(
                text = targetLabel,
                style = MaterialTheme.typography.labelMedium,
                color = KinetixTokens.Colors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
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
 * Clean Apple-style interactive Donut chart for Macros & Volume breakdown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractivePieChart(
    slices: List<PieSliceData>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    chartSize: Dp = 176.dp
) {
    val reduceMotion = rememberReduceAnimations()
    val validSlices = remember(slices) { slices.filter { it.value > 0f } }
    if (validSlices.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.DonutLarge,
            message = emptyMessage
        )
        return
    }

    var selectedIdx by remember(validSlices) { mutableIntStateOf(0) }
    val safeSelectedIdx = selectedIdx.coerceIn(0, validSlices.lastIndex)
    val total = remember(validSlices) { validSlices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f) }
    val selectedSlice = validSlices[safeSelectedIdx]
    val selectedPct = ((selectedSlice.value / total) * 100f).roundToInt()

    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { loaded = true }
    val animReveal by animateFloatAsState(
        targetValue = if (loaded || reduceMotion) 1f else 0f,
        animationSpec = KinetixTokens.Motion.ringSpring(reduceMotion),
        label = "donutReveal"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(chartSize),
            contentAlignment = Alignment.Center
        ) {
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
                val baseStroke = 24.dp.toPx()
                val selectedExtra = 5.dp.toPx()
                val radius = (size.minDimension / 2f) - baseStroke / 2f - 6.dp.toPx()
                val topLeft = Offset(size.width / 2f - radius, size.height / 2f - radius)
                val arcSize = Size(radius * 2f, radius * 2f)

                var startAngle = -90f
                validSlices.forEachIndexed { idx, slice ->
                    val sweep = (slice.value / total) * 360f * animReveal
                    val gap = if (validSlices.size > 1) 3.5f else 0f
                    val drawSweep = (sweep - gap).coerceAtLeast(1f)
                    val isSel = idx == safeSelectedIdx
                    drawArc(
                        color = if (isSel) slice.color else slice.color.copy(alpha = 0.65f),
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
                    startAngle += (slice.value / total) * 360f * animReveal
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
                    style = MaterialTheme.typography.labelLarge,
                    color = KinetixTokens.Colors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = selectedSlice.valueText,
                    style = MaterialTheme.typography.labelMedium,
                    color = KinetixTokens.Colors.TextSecondary,
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
                    shape = KinetixTokens.Radius.PillShape,
                    color = if (selected) slice.color.copy(alpha = 0.18f) else KinetixTokens.Colors.NestedSurface,
                    border = BorderStroke(
                        0.5.dp,
                        if (selected) slice.color.copy(alpha = 0.7f) else KinetixTokens.Colors.Hairline
                    ),
                    modifier = Modifier.clickable { selectedIdx = idx }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) KinetixTokens.Colors.TextPrimary else KinetixTokens.Colors.TextSecondary
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
 * Animated Steps Trend Bar Chart with dashed goal reference and tap inspection.
 */
@Composable
fun InteractiveBarChart(
    entries: List<BarEntry>,
    goalValue: Float,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
            message = emptyMessage,
            accentColor = KinetixTokens.Colors.Hydration
        )
        return
    }

    val reduceMotion = rememberReduceAnimations()
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { loaded = true }
    val animHeightScale by animateFloatAsState(
        targetValue = if (loaded || reduceMotion) 1f else 0f,
        animationSpec = KinetixTokens.Motion.ringSpring(reduceMotion),
        label = "barReveal"
    )

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
                text = "${selected.label} · ${selected.valueText}",
                style = MaterialTheme.typography.titleMedium,
                color = KinetixTokens.Colors.TextPrimary
            )
            Text(
                text = "Goal ${goalValue.toInt()}",
                style = MaterialTheme.typography.labelMedium,
                color = KinetixTokens.Colors.Hydration
            )
        }
        Spacer(Modifier.height(10.dp))
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
                color = KinetixTokens.Colors.Hydration.copy(alpha = 0.4f),
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            val slotWidth = size.width / entries.size.coerceAtLeast(1)
            val barWidth = (slotWidth * 0.56f).coerceAtMost(26.dp.toPx())
            entries.forEachIndexed { idx, item ->
                val ratio = (item.value / maxValue).coerceIn(0.06f, 1f) * animHeightScale
                val barHeight = size.height * ratio
                val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                val isGoalMet = item.value >= goalValue
                val isSelected = idx == safeIndex
                val baseColor = if (isGoalMet) KinetixTokens.Colors.Workout else KinetixTokens.Colors.Hydration
                val barColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.5f)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            entries.forEachIndexed { idx, item ->
                val isSel = idx == safeSelectedIndex(safeIndex, entries.size)
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSel) KinetixTokens.Colors.TextPrimary else KinetixTokens.Colors.TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

private fun safeSelectedIndex(idx: Int, size: Int): Int = idx.coerceIn(0, (size - 1).coerceAtLeast(0))

/**
 * Horizontal muscle hard-sets chart with the 10–20 optimal target band highlighted
 * and high-contrast StatChips ("Low", "In range", "High").
 */
@Composable
fun WeeklyHardSetsChart(
    volumes: List<MuscleWeeklyVolume>,
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberReduceAnimations()
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { loaded = true }
    val scaleMax = 24f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        volumes.forEach { vol ->
            val (label, fg, bg) = when (vol.status) {
                VolumeStatus.UNDER -> Triple(
                    "Low",
                    KinetixTokens.Colors.StatusLowFg,
                    KinetixTokens.Colors.StatusLowBg
                )
                VolumeStatus.OPTIMAL -> Triple(
                    "In range",
                    KinetixTokens.Colors.StatusInRangeFg,
                    KinetixTokens.Colors.StatusInRangeBg
                )
                VolumeStatus.EXCESS -> Triple(
                    "High",
                    KinetixTokens.Colors.StatusHighFg,
                    KinetixTokens.Colors.StatusHighBg
                )
            }
            val fillRatio by animateFloatAsState(
                targetValue = if (loaded || reduceMotion) (vol.hardSets / scaleMax).coerceIn(0f, 1f) else 0f,
                animationSpec = KinetixTokens.Motion.ringSpring(reduceMotion),
                label = "vol_${vol.muscleGroup}"
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = vol.muscleGroup,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = KinetixTokens.Colors.TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${vol.hardSets} sets",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KinetixTokens.Colors.TextSecondary
                        )
                        StatChip(text = label, foregroundColor = fg, backgroundColor = bg)
                    }
                }
                Spacer(Modifier.height(6.dp))
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(KinetixTokens.Colors.NestedSurface)
                ) {
                    // 10–20 optimal target band highlighted cleanly inside the track
                    val bandStart = maxWidth * (10f / scaleMax)
                    val bandWidth = maxWidth * (10f / scaleMax)
                    Box(
                        modifier = Modifier
                            .padding(start = bandStart)
                            .width(bandWidth)
                            .height(8.dp)
                            .background(KinetixTokens.Colors.StatusInRangeFg.copy(alpha = 0.22f))
                    )
                    if (vol.hardSets > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fillRatio.coerceAtLeast(0.03f))
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(fg)
                        )
                    }
                }
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
