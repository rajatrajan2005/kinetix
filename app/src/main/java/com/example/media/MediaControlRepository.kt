package com.example.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class MediaPlaybackUiState(
    val hasListenerAccess: Boolean = false,
    val hasActiveSession: Boolean = false,
    val title: String = "No media playing",
    val artist: String = "Tap play in any music app",
    val sourcePackage: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val albumArt: Bitmap? = null
)

/**
 * Controls any active media session (Poweramp, Echo, Spotify, etc.) through
 * MediaSessionManager + MediaController with zero player-specific SDKs.
 */
class MediaControlRepository(private val context: Context) {

    private val componentName = ComponentName(context, KinetixMediaListenerService::class.java)
    private val sessionManager =
        context.applicationContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private val _state = MutableStateFlow(MediaPlaybackUiState(hasListenerAccess = isAccessGranted()))
    val state: StateFlow<MediaPlaybackUiState> = _state.asStateFlow()

    private var activeController: MediaController? = null
    private var isListening = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishCurrentControllerState()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishCurrentControllerState()
        }

        override fun onSessionDestroyed() {
            attachToBestSession(emptyList())
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            attachToBestSession(controllers.orEmpty())
        }

    fun isAccessGranted(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun buildNotificationListenerSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun refreshBinding() {
        val granted = isAccessGranted()
        if (!granted) {
            detach()
            _state.value = MediaPlaybackUiState(hasListenerAccess = false)
            return
        }
        runCatching {
            if (!isListening) {
                sessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
                isListening = true
            }
            val sessions = sessionManager.getActiveSessions(componentName)
            attachToBestSession(sessions)
        }.onFailure {
            _state.value = MediaPlaybackUiState(hasListenerAccess = false)
        }
    }

    private fun attachToBestSession(controllers: List<MediaController>) {
        activeController?.unregisterCallback(controllerCallback)
        val chosen = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull()

        activeController = chosen
        chosen?.registerCallback(controllerCallback)
        publishCurrentControllerState()
    }

    private fun publishCurrentControllerState() {
        val controller = activeController
        if (controller == null) {
            _state.value = MediaPlaybackUiState(
                hasListenerAccess = isAccessGranted(),
                hasActiveSession = false
            )
            return
        }
        val meta = controller.metadata
        val pb = controller.playbackState
        val isPlaying = pb?.state == PlaybackState.STATE_PLAYING
        val estimatedPos = estimatePosition(pb)
        val duration = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
        val art = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        _state.value = MediaPlaybackUiState(
            hasListenerAccess = true,
            hasActiveSession = true,
            title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
                ?: "Active Audio",
            artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
                ?: controller.packageName,
            sourcePackage = controller.packageName,
            isPlaying = isPlaying,
            positionMs = estimatedPos.coerceIn(0L, if (duration > 0L) duration else Long.MAX_VALUE),
            durationMs = duration,
            albumArt = art
        )
    }

    private fun estimatePosition(pb: PlaybackState?): Long {
        if (pb == null) return 0L
        if (pb.state != PlaybackState.STATE_PLAYING) return pb.position.coerceAtLeast(0L)
        val delta = SystemClock.elapsedRealtime() - pb.lastPositionUpdateTime
        return (pb.position + (delta * pb.playbackSpeed).toLong()).coerceAtLeast(0L)
    }

    fun playPause() {
        val controller = activeController ?: return
        val controls = controller.transportControls
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controls.pause()
        } else {
            controls.play()
        }
    }

    fun skipNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        activeController?.transportControls?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun detach() {
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        if (isListening) {
            runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
            isListening = false
        }
    }
}
