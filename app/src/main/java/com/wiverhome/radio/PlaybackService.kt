package com.wiverhome.radio

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** Plays the stream in the background; Media3 shows the notification and lock screen controls. */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private var retries = 0

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        // Next/previous station wraps around the queue.
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(StreamListener(player))

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(SessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    /** The stream URI travels from the app in requestMetadata; restore it here. */
    private class SessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val items = mediaItems.map { item ->
                val uri = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri
                val builder = item.buildUpon().setUri(uri)
                if (uri?.toString()?.contains(".m3u8", ignoreCase = true) == true) {
                    builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                builder.build()
            }
            return Futures.immediateFuture(items.toMutableList())
        }
    }

    private inner class StreamListener(private val player: ExoPlayer) : Player.Listener {
        /** Radio drops out on bad networks: reconnect a few times before showing an error. */
        override fun onPlayerError(error: PlaybackException) {
            if (retries >= MAX_RETRIES || !player.playWhenReady) return
            retries++
            handler.postDelayed({
                player.seekToDefaultPosition()
                player.prepare()
            }, RETRY_DELAY_MS * retries)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) retries = 0
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                retries = 0
                handler.removeCallbacksAndMessages(null)
            }
        }

        /** Shoutcast/Icecast "StreamTitle" becomes the title; the station name moves to the artist line. */
        override fun onMetadata(metadata: Metadata) {
            for (i in 0 until metadata.length()) {
                val entry = metadata.get(i)
                if (entry is IcyInfo) showTrack(entry.title?.trim().orEmpty())
            }
        }

        private fun showTrack(track: String) {
            val item = player.currentMediaItem ?: return
            val station = item.mediaMetadata.station?.toString() ?: return
            val meta = item.mediaMetadata.buildUpon()
                .setTitle(track.ifEmpty { station })
                .setArtist(if (track.isEmpty()) null else station)
                .build()
            if (meta.title == item.mediaMetadata.title) return
            player.replaceMediaItem(player.currentMediaItemIndex, item.buildUpon().setMediaMetadata(meta).build())
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 2000L
    }
}
