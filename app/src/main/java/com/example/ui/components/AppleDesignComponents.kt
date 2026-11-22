package com.example.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KinetixTokens
import com.example.ui.theme.rememberReduceAnimations
import kotlin.math.roundToInt

/**
 * Shared press-scale micro-interaction (0.97f on press) powered purely by GPU transform.
 */
fun Modifier.applePressScale(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val reduceMotion = rememberReduceAnimations()
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed && !reduceMotion) KinetixTokens.Motion.PRESS_SCALE else 1f,
        animationSpec = KinetixTokens.Motion.pressSpring(reduceMotion),
        label = "applePressScale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Optional GPU RenderEffect blur strictly for floating bottom nav & sheet headers on API 31+.
 */
fun Modifier.appleChromeBlur(radiusDp: Dp = 20.dp): Modifier = composed {
    val density = LocalDensity.current
    val radiusPx = with(density) { radiusDp.toPx() }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.graphicsLayer {
            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                radiusPx,
                radiusPx,
                android.graphics.Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        }
    } else {
        this
    }
}

/**
 * Apple Fitness-inspired elevated card:
 * #1C1C1E surface, continuous-feel 22dp corners, 0.5dp @ 8% white hairline, zero glow or gradient blobs.
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    containerColor: Color = KinetixTokens.Colors.CardSurface,
    borderColor: Color = KinetixTokens.Colors.Hairline,
    contentPadding: Dp = KinetixTokens.Spacing.CardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .applePressScale(interactionSource = interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                } else {
                    Modifier
                }
            ),
        shape = KinetixTokens.Radius.CardShape,
        color = containerColor,
        border = BorderStroke(0.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Apple Fitness single circular RingProgress with spring load animation.
 */
@Composable
fun RingProgress(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 8.dp,
    trackColor: Color = color.copy(alpha = 0.18f),
    startAngle: Float = -90f,
    maxSweep: Float = 360f,
    centerContent: @Composable (() -> Unit)? = null
) {
    val reduceMotion = rememberReduceAnimations()
    var hasLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasLoaded = true }
    val targetProgress = if (hasLoaded || reduceMotion) progress.coerceIn(0f, 1.2f) else 0f
    val animProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = KinetixTokens.Motion.ringSpring(reduceMotion),
        label = "ringProgress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = maxSweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            val clamped = animProgress.coerceIn(0f, 1f)
            val sweep = clamped * maxSweep
            if (sweep > 0.5f) {
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        centerContent?.invoke()
    }
}

/**
 * iOS-style SegmentedControl with spring-animated sliding thumb and haptic feedback.
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String = "segment"
) {
    if (options.isEmpty()) return
    val view = LocalView.current
    val reduceMotion = rememberReduceAnimations()
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = KinetixTokens.Colors.NestedSurface,
        border = BorderStroke(0.5.dp, KinetixTokens.Colors.Hairline)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
        ) {
            val count = options.size.coerceAtLeast(1)
            val segWidth = maxWidth / count
            val segWidthPx = with(LocalDensity.current) { segWidth.toPx() }
            val thumbOffsetPx by animateFloatAsState(
                targetValue = selectedIndex * segWidthPx,
                animationSpec = KinetixTokens.Motion.standardSpring(reduceMotion),
                label = "segmentThumb"
            )

            Box(
                modifier = Modifier
                    .width(segWidth)
                    .height(38.dp)
                    .offset { IntOffset(thumbOffsetPx.roundToInt(), 0) }
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFF3A3A3C))
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                options.forEach { option ->
                    val isSelected = option == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onSelect(option)
                                }
                            }
                            .testTag("${tagPrefix}_${labelFor(option)}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelFor(option),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isSelected) KinetixTokens.Colors.TextPrimary else KinetixTokens.Colors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS Settings-style inset grouped container card with hairline row dividers.
 */
@Composable
fun AppleGroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!header.isNullOrBlank()) {
            Text(
                text = header,
                style = MaterialTheme.typography.labelMedium,
                color = KinetixTokens.Colors.TextSecondary,
                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = KinetixTokens.Radius.CardShape,
            color = KinetixTokens.Colors.CardSurface,
            border = BorderStroke(0.5.dp, KinetixTokens.Colors.Hairline)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
        if (!footer.isNullOrBlank()) {
            Text(
                text = footer,
                style = MaterialTheme.typography.labelMedium,
                color = KinetixTokens.Colors.TextSecondary,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, end = 12.dp)
            )
        }
    }
}

@Composable
fun AppleRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = KinetixTokens.Colors.Hairline
    )
}

/**
 * iOS-style ListRow for grouped lists, navigation rows, and single-choice selectors.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingTint: Color = KinetixTokens.Colors.TextPrimary,
    leadingBg: Color = KinetixTokens.Colors.NestedSurface,
    trailingText: String? = null,
    trailingColor: Color = KinetixTokens.Colors.TextSecondary,
    showChevron: Boolean = false,
    selectedCheckmark: Boolean? = null,
    checkmarkTint: Color = KinetixTokens.Colors.Workout,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .applePressScale(interactionSource = interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(leadingBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = leadingTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = KinetixTokens.Colors.TextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KinetixTokens.Colors.TextSecondary
                )
            }
        }
        if (trailingContent != null) {
            Spacer(Modifier.width(8.dp))
            trailingContent()
        } else {
            if (!trailingText.isNullOrBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = trailingColor
                )
            }
            if (selectedCheckmark == true) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = checkmarkTint,
                    modifier = Modifier.size(20.dp)
                )
            } else if (showChevron) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = KinetixTokens.Colors.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * PrimaryButton with spring press scale and optional haptic confirmation.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = KinetixTokens.Colors.Workout,
    contentColor: Color = KinetixTokens.Colors.OnWorkout,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    strongHaptic: Boolean = false
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = {
            view.performHapticFeedback(
                if (strongHaptic) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP
            )
            onClick()
        },
        enabled = enabled,
        interactionSource = interactionSource,
        shape = KinetixTokens.Radius.ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = KinetixTokens.Colors.NestedSurface,
            disabledContentColor = KinetixTokens.Colors.TextTertiary
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        modifier = modifier
            .height(48.dp)
            .applePressScale(enabled = enabled, interactionSource = interactionSource)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Apple-style ModalBottomSheet supporting medium/large detent toggle and blurred/translucent header bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    largeDetent: Boolean = false,
    headerAction: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = largeDetent)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = KinetixTokens.Radius.SheetShape,
        containerColor = KinetixTokens.Colors.CardSurface,
        contentColor = KinetixTokens.Colors.TextPrimary,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = KinetixTokens.Colors.TextTertiary,
                width = 38.dp,
                height = 4.dp
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Translucent blurred header surface
            Surface(
                color = KinetixTokens.Colors.SheetHeaderTint,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = KinetixTokens.Colors.TextPrimary
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = KinetixTokens.Colors.TextSecondary
                            )
                        }
                    }
                    headerAction?.invoke(this)
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = KinetixTokens.Colors.Hairline)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                content = content
            )
        }
    }
}

/**
 * Apple-style Stepper with tactile haptics, large numeric readout, and 0.97 press bounce.
 */
@Composable
fun Stepper(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    decrementEnabled: Boolean = true,
    incrementEnabled: Boolean = true,
    accentColor: Color = KinetixTokens.Colors.TextPrimary,
    decrementTestTag: String? = null,
    incrementTestTag: String? = null
) {
    val view = LocalView.current
    Surface(
        modifier = modifier,
        shape = KinetixTokens.Radius.NestedShape,
        color = KinetixTokens.Colors.NestedSurface,
        border = BorderStroke(0.5.dp, KinetixTokens.Colors.Hairline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = KinetixTokens.Colors.TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = valueText,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
                maxLines = 1
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepperButton(
                    icon = Icons.Rounded.Remove,
                    contentDescription = "Decrease $label",
                    enabled = decrementEnabled,
                    modifier = if (decrementTestTag != null) Modifier.testTag(decrementTestTag) else Modifier,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onDecrement()
                    }
                )
                StepperButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "Increase $label",
                    enabled = incrementEnabled,
                    modifier = if (incrementTestTag != null) Modifier.testTag(incrementTestTag) else Modifier,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onIncrement()
                    }
                )
            }
        }
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF3A3A3C),
        modifier = modifier
            .size(48.dp)
            .applePressScale(enabled = enabled, interactionSource = interactionSource)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) KinetixTokens.Colors.TextPrimary else KinetixTokens.Colors.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Custom iOS-style tactile slider with circular white thumb, large live value readout,
 * and discrete haptic ticks — distinct from progress bars.
 */
@Composable
fun AppleCustomSlider(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    stepSize: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = KinetixTokens.Colors.Hydration
) {
    val view = LocalView.current
    val min = valueRange.start
    val max = valueRange.endInclusive.coerceAtLeast(min + 0.001f)
    val span = max - min
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var lastHapticTick by remember(title) { mutableIntStateOf(((value - min) / stepSize).roundToInt()) }

    val fraction = ((value - min) / span).coerceIn(0f, 1f)

    fun updateFromFraction(rawFraction: Float) {
        val clamped = rawFraction.coerceIn(0f, 1f)
        val rawVal = min + clamped * span
        val stepped = if (stepSize > 0f) {
            min + (((rawVal - min) / stepSize).roundToInt() * stepSize)
        } else {
            rawVal
        }
        val finalVal = stepped.coerceIn(min, max)
        val tick = if (stepSize > 0f) ((finalVal - min) / stepSize).roundToInt() else (clamped * 20).roundToInt()
        if (tick != lastHapticTick) {
            lastHapticTick = tick
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        onValueChange(finalVal)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = KinetixTokens.Colors.TextPrimary
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(min, max, stepSize) {
                    detectTapGestures { offset ->
                        updateFromFraction(offset.x / trackWidthPx)
                    }
                }
                .pointerInput(min, max, stepSize) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        updateFromFraction(change.position.x / trackWidthPx)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val thumbRadius = 13.dp
            val thumbPx = with(LocalDensity.current) { thumbRadius.toPx() }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackH = 4.dp.toPx()
                val usableW = (size.width - thumbPx * 2f).coerceAtLeast(1f)
                val centerY = size.height / 2f
                val thumbX = thumbPx + usableW * fraction

                // Track background
                drawLine(
                    color = Color(0xFF3A3A3C),
                    start = Offset(thumbPx, centerY),
                    end = Offset(size.width - thumbPx, centerY),
                    strokeWidth = trackH,
                    cap = StrokeCap.Round
                )
                // Active track
                drawLine(
                    color = accentColor,
                    start = Offset(thumbPx, centerY),
                    end = Offset(thumbX, centerY),
                    strokeWidth = trackH,
                    cap = StrokeCap.Round
                )
                // Subtle shadow ring behind white iOS thumb
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = thumbPx + 1.5.dp.toPx(),
                    center = Offset(thumbX, centerY)
                )
                // Crisp circular white iOS thumb
                drawCircle(
                    color = Color.White,
                    radius = thumbPx,
                    center = Offset(thumbX, centerY)
                )
            }
        }
    }
}

/**
 * Compact Apple-style EmptyState: icon + one concise line + optional single action button.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    accentColor: Color = KinetixTokens.Colors.TextSecondary,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KinetixTokens.Colors.NestedSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = KinetixTokens.Colors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            Surface(
                onClick = onAction,
                shape = KinetixTokens.Radius.PillShape,
                color = accentColor.copy(alpha = 0.16f),
                modifier = Modifier.height(36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor
                    )
                }
            }
        }
    }
}

/**
 * High-contrast pill StatChip for muscle groups and volume statuses.
 */
@Composable
fun StatChip(
    text: String,
    foregroundColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = KinetixTokens.Radius.PillShape,
        color = backgroundColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = foregroundColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * Collapsible iOS Large Title header synced cleanly to LazyListState scroll position.
 */
@Composable
fun AppleLargeTitleHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = KinetixTokens.Colors.TextSecondary
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = KinetixTokens.Colors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = trailing
            )
        }
    }
}

/**
 * Compact blurred centered iOS title bar that fades in when the list scrolls past the large title.
 */
@Composable
fun AppleCollapsedNavBar(
    title: String,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val showCollapsed by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 70
        }
    }
    AnimatedVisibility(
        visible = showCollapsed,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150)),
        modifier = modifier
    ) {
        Surface(
            color = KinetixTokens.Colors.BlurredChrome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = KinetixTokens.Colors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    if (trailing != null) {
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically,
                            content = trailing
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = KinetixTokens.Colors.Hairline)
            }
        }
    }
}

/**
 * GPU-only opacity pulse shimmer skeleton for network fetches (no blank loading screen).
 */
@Composable
fun ShimmerListPlaceholder(
    rowCount: Int = 3,
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberReduceAnimations()
    val alpha = if (reduceMotion) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val anim by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(750, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmerAlpha"
        )
        anim
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(rowCount) {
            Card(contentPadding = 14.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(16.dp)
                        .graphicsLayer { this.alpha = alpha }
                        .clip(RoundedCornerShape(6.dp))
                        .background(KinetixTokens.Colors.NestedSurface)
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(12.dp)
                        .graphicsLayer { this.alpha = alpha }
                        .clip(RoundedCornerShape(6.dp))
                        .background(KinetixTokens.Colors.NestedSurface)
                )
            }
        }
    }
}

/**
 * Subtle floating pill toast ("Saved") for instant auto-save confirmation.
 */
@Composable
fun AppleSavedToast(
    visible: Boolean,
    message: String = "Saved",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + slideInVertically { -it / 2 },
        exit = fadeOut(tween(180)) + slideOutVertically { -it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = KinetixTokens.Radius.PillShape,
            color = KinetixTokens.Colors.NestedSurface,
            border = BorderStroke(0.5.dp, KinetixTokens.Colors.Hairline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = KinetixTokens.Colors.StatusInRangeFg,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelLarge,
                    color = KinetixTokens.Colors.TextPrimary
                )
            }
        }
    }
}

/**
 * Proper Title-Case helper for display names (e.g. "rajat" -> "Rajat").
 */
fun formatPossessiveProfileTitle(rawName: String): String {
    val clean = rawName.trim().ifBlank { "Athlete" }
    val capitalized = clean.split(Regex("\\s+")).joinToString(" ") { word ->
        word.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
    }
    return if (capitalized.endsWith("s", ignoreCase = true)) {
        "$capitalized' Profile"
    } else {
        "$capitalized's Profile"
    }
}
