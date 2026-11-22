package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.media.AudioOutputKind
import com.example.media.MediaPlaybackUiState
import com.example.ui.theme.KinetixTokens
import com.example.ui.theme.rememberReduceAnimations

/**
 * Pure true-black (#000000) OLED canvas container — zero ambient blobs or blur jank.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun KinetixAmbientBackdrop(
    primaryOrb: Color = KinetixTokens.Colors.Workout,
    secondaryOrb: Color = KinetixTokens.Colors.Hydration,
    tertiaryOrb: Color = KinetixTokens.Colors.Calories,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KinetixTokens.Colors.Background),
        content = content
    )
}

/**
 * Clean elevated Apple Fitness surface wrapper delegating to Card.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun KinetixCard(
    modifier: Modifier = Modifier,
    containerColor: Color = KinetixTokens.Colors.CardSurface,
    borderColor: Color? = null,
    accentGlow: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        containerColor = containerColor,
        borderColor = borderColor ?: KinetixTokens.Colors.Hairline,
        onClick = onClick,
        content = content
    )
}

/**
 * Apple Fitness-inspired Nutrition Ring displaying kcal remaining in large type inside the ring.
 */
@Composable
fun CalorieRingGauge(
    consumedKcal: Int,
    targetKcal: Int,
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    accentColor: Color = KinetixTokens.Colors.Calories
) {
    val reduceMotion = rememberReduceAnimations()
    val rawProgress = if (targetKcal > 0) consumedKcal.toFloat() / targetKcal.toFloat() else 0f
    val remaining = (targetKcal - consumedKcal).coerceAtLeast(0)
    val over = (consumedKcal - targetKcal).coerceAtLeast(0)
    val animatedDisplay by animateIntAsState(
        targetValue = if (over > 0) over else remaining,
        animationSpec = KinetixTokens.Motion.standardSpring(reduceMotion),
        label = "kcalRemaining"
    )
    val ringColor = if (over > 0) KinetixTokens.Colors.StatusLowFg else accentColor

    RingProgress(
        progress = rawProgress,
        color = ringColor,
        modifier = modifier,
        size = size,
        strokeWidth = 11.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$animatedDisplay",
                style = MaterialTheme.typography.headlineMedium,
                color = KinetixTokens.Colors.TextPrimary
            )
            Text(
                text = if (over > 0) "kcal over" else "kcal left",
                style = MaterialTheme.typography.labelMedium,
                color = ringColor
            )
        }
    }
}

/**
 * Slim Apple-style horizontal macro bar with "x / y g" readout and spring animation.
 */
@Composable
fun MacroProgressRow(
    label: String,
    consumedG: Double,
    targetG: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberReduceAnimations()
    val progress = if (targetG > 0) (consumedG / targetG.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = KinetixTokens.Motion.ringSpring(reduceMotion),
        label = "macro_$label"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KinetixTokens.Colors.TextSecondary
                )
            }
            Text(
                text = "${consumedG.toInt()} / ${targetG} g",
                style = MaterialTheme.typography.labelLarge,
                color = KinetixTokens.Colors.TextPrimary
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceAtLeast(if (consumedG > 0) 0.03f else 0f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun CompactAudioBadge(mediaState: MediaPlaybackUiState) {
    val icon = when (mediaState.audioOutputKind) {
        AudioOutputKind.BLUETOOTH -> Icons.Rounded.BluetoothAudio
        AudioOutputKind.WIRED -> Icons.Rounded.Headphones
        AudioOutputKind.SPEAKER -> Icons.Rounded.Speaker
    }
    val tint = if (mediaState.isHeadsetConnected) KinetixTokens.Colors.Hydration else KinetixTokens.Colors.TextSecondary

    Row(
        modifier = Modifier.testTag("audio_output_badge"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Audio output: ${mediaState.audioOutputLabel}",
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = mediaState.audioOutputLabel,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Slim Apple-style Mini-Player strip (artwork, title, play/pause + next),
 * collapsing to a compact "Connect music" row when Notification Listener isn't granted.
 */
@Suppress("UNUSED_PARAMETER")
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
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("workout_mini_player"),
        shape = RoundedCornerShape(16.dp),
        color = KinetixTokens.Colors.CardSurface,
        border = BorderStroke(0.5.dp, KinetixTokens.Colors.Hairline)
    ) {
        if (!mediaState.hasListenerAccess) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .applePressScale(interactionSource = interactionSource)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onOpenListenerSettings
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("enable_media_listener_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(KinetixTokens.Colors.NestedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = KinetixTokens.Colors.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Connect music",
                        style = MaterialTheme.typography.titleMedium,
                        color = KinetixTokens.Colors.TextPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompactAudioBadge(mediaState)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Connect music player",
                        tint = KinetixTokens.Colors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(9.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(KinetixTokens.Colors.NestedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "No album art",
                            tint = KinetixTokens.Colors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (mediaState.hasActiveSession) mediaState.title else "No active track",
                        style = MaterialTheme.typography.labelLarge,
                        color = KinetixTokens.Colors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = if (mediaState.hasActiveSession) {
                        "${mediaState.artist} · ${mediaState.audioOutputLabel}"
                    } else {
                        mediaState.audioOutputLabel
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (mediaState.isHeadsetConnected) {
                            KinetixTokens.Colors.Hydration
                        } else {
                            KinetixTokens.Colors.TextSecondary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("audio_output_badge")
                    )
                }

                val playTint by animateColorAsState(
                    targetValue = if (mediaState.isPlaying) KinetixTokens.Colors.Workout else KinetixTokens.Colors.TextPrimary,
                    label = "playTint"
                )
                IconButton(
                    onClick = onPlayPause,
                    enabled = mediaState.hasActiveSession,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("media_play_pause_button")
                        .semantics { contentDescription = if (mediaState.isPlaying) "Pause" else "Play" }
                ) {
                    Icon(
                        imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = playTint
                    )
                }
                IconButton(
                    onClick = onNext,
                    enabled = mediaState.hasActiveSession,
                    modifier = Modifier
                        .size(44.dp)
                        .semantics { contentDescription = "Next track" }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = null,
                        tint = KinetixTokens.Colors.TextPrimary
                    )
                }
            }
        }
    }
}
