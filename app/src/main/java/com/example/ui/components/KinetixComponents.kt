package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.media.MediaPlaybackUiState
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricVolt
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.GlacialCyan
import com.example.ui.theme.GlassBottomTint
import com.example.ui.theme.GlassRimHighlight
import com.example.ui.theme.GlassRimShadow
import com.example.ui.theme.GlassTopSheen
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import kotlin.math.min

/**
 * Lightweight 120-fps GPU radial ambient glow backdrop over true AMOLED black,
 * giving frosted glass cards genuine depth and color refraction without blur jank.
 */
@Composable
fun KinetixAmbientBackdrop(
    primaryOrb: Color = ElectricVolt,
    secondaryOrb: Color = GlacialCyan,
    tertiaryOrb: Color = WarmAmber,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryOrb.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(w * 0.88f, h * 0.08f),
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = Offset(w * 0.88f, h * 0.08f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryOrb.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(w * 0.10f, h * 0.42f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.10f, h * 0.42f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tertiaryOrb.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(w * 0.82f, h * 0.82f),
                    radius = w * 0.60f
                ),
                radius = w * 0.60f,
                center = Offset(w * 0.82f, h * 0.82f)
            )
        }
        content()
    }
}

/**
 * Translucent glassmorphic card with specular top-left rim light and subtle inner frosted gradient.
 */
@Composable
fun KinetixCard(
    modifier: Modifier = Modifier,
    containerColor: Color = ObsidianSurface,
    borderColor: Color? = null,
    accentGlow: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val rimBrush = remember(borderColor, accentGlow) {
        if (borderColor != null) {
            Brush.linearGradient(listOf(borderColor, borderColor.copy(alpha = 0.35f)))
        } else {
            val endTint = accentGlow?.copy(alpha = 0.32f) ?: GlassRimShadow
            Brush.linearGradient(
                colors = listOf(GlassRimHighlight, endTint, GlassRimShadow)
            )
        }
    }
    val glassFillBrush = remember(containerColor, accentGlow) {
        Brush.verticalGradient(
            colors = listOf(
                (accentGlow ?: Color.White).copy(alpha = 0.09f),
                GlassTopSheen,
                GlassBottomTint
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, rimBrush)
    ) {
        Column(
            modifier = Modifier
                .background(glassFillBrush)
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun CalorieRingGauge(
    consumedKcal: Int,
    targetKcal: Int,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    accentColor: Color = WarmAmber
) {
    val rawProgress = if (targetKcal > 0) consumedKcal.toFloat() / targetKcal.toFloat() else 0f
    val animatedSweep by animateFloatAsState(
        targetValue = (rawProgress.coerceIn(0f, 1.15f) * 270f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "calorieSweep"
    )
    val animatedKcal by animateIntAsState(
        targetValue = consumedKcal,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "kcalValue"
    )
    val remaining = (targetKcal - consumedKcal).coerceAtLeast(0)
    val arcColor = if (consumedKcal > targetKcal) Color(0xFFF87171) else accentColor

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(arcColor.copy(alpha = 0.16f), Color.Transparent)
                ),
                radius = this.size.minDimension * 0.52f
            )
            val strokePx = 11.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = ElevatedCarbon.copy(alpha = 0.75f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = arcColor.copy(alpha = 0.25f),
                startAngle = 135f,
                sweepAngle = min(270f, animatedSweep),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx + 6.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = arcColor,
                startAngle = 135f,
                sweepAngle = min(270f, animatedSweep),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$animatedKcal",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "/ $targetKcal kcal",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$remaining left",
                style = MaterialTheme.typography.labelSmall,
                color = arcColor
            )
        }
    }
}

@Composable
fun MacroProgressRow(
    label: String,
    consumedG: Double,
    targetG: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (targetG > 0) (consumedG / targetG.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "macro_$label"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(
                text = "${consumedG.toInt()} / ${targetG}g",
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape),
            color = color,
            trackColor = ElevatedCarbon.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun MiniMediaPlayerBar(
    mediaState: MediaPlaybackUiState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onOpenListenerSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val rimBrush = remember(mediaState.isPlaying) {
        Brush.linearGradient(
            listOf(
                GlassRimHighlight,
                if (mediaState.isPlaying) ElectricVolt.copy(alpha = 0.45f) else GlassRimShadow
            )
        )
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("workout_mini_player"),
        shape = shape,
        color = CarbonSurface,
        border = BorderStroke(1.dp, rimBrush)
    ) {
        val innerGlass = Brush.verticalGradient(
            listOf(
                if (mediaState.isPlaying) ElectricVolt.copy(alpha = 0.08f) else GlassTopSheen,
                GlassBottomTint
            )
        )
        if (!mediaState.hasListenerAccess) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(innerGlass)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = ElectricVolt,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Universal Music Control",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Control Poweramp, Echo & any player",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onOpenListenerSettings,
                    modifier = Modifier.testTag("enable_media_listener_button")
                ) {
                    Text("Connect")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .background(innerGlass)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val artBitmap = remember(mediaState.albumArt) {
                        mediaState.albumArt?.asImageBitmap()
                    }
                    if (artBitmap != null) {
                        Image(
                            bitmap = artBitmap,
                            contentDescription = "Album art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElevatedCarbon),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "No album art",
                                tint = ElectricVolt
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaState.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = mediaState.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onPrevious,
                        enabled = mediaState.hasActiveSession,
                        modifier = Modifier.semantics { contentDescription = "Previous track" }
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = TextPrimary)
                    }
                    val playTint by animateColorAsState(
                        targetValue = if (mediaState.isPlaying) ElectricVolt else TextPrimary,
                        label = "playTint"
                    )
                    IconButton(
                        onClick = onPlayPause,
                        enabled = mediaState.hasActiveSession,
                        modifier = Modifier
                            .testTag("media_play_pause_button")
                            .semantics { contentDescription = if (mediaState.isPlaying) "Pause" else "Play" }
                    ) {
                        Icon(
                            imageVector = if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = playTint
                        )
                    }
                    IconButton(
                        onClick = onNext,
                        enabled = mediaState.hasActiveSession,
                        modifier = Modifier.semantics { contentDescription = "Next track" }
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, tint = TextPrimary)
                    }
                }

                if (mediaState.hasActiveSession && mediaState.durationMs > 0L) {
                    val progress = (mediaState.positionMs.toFloat() / mediaState.durationMs.toFloat())
                        .coerceIn(0f, 1f)
                    Slider(
                        value = progress,
                        onValueChange = { fraction ->
                            onSeekTo((fraction * mediaState.durationMs).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricVolt,
                            activeTrackColor = ElectricVolt,
                            inactiveTrackColor = ElevatedCarbon
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                    )
                } else if (!mediaState.hasActiveSession) {
                    Text(
                        text = "Start playback in your music app to control it here",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
