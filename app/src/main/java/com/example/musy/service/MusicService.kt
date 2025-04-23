package com.example.musy.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaSessionCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.musy.R
import kotlinx.coroutines.*

class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "musy_playback_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREV = "ACTION_PREV"
        const val ACTION_REWIND = "ACTION_REWIND"
        const val ACTION_SEEK = "ACTION_SEEK"

        const val EXTRA_SONG_TITLE = "EXTRA_SONG_TITLE"
        const val EXTRA_ALBUM_ART = "EXTRA_ALBUM_ART"
        const val EXTRA_SONG_URL = "EXTRA_SONG_URL"
        const val EXTRA_SEEK_POSITION = "EXTRA_SEEK_POSITION"
    }

    private lateinit var mediaSession: MediaSessionCompat
    private val playerManager = MusicPlayerManager()
    private var currentTitle: String = ""
    private var currentArt: Int = R.drawable.ic_launcher_foreground
    private var currentUrl: String = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "MusySession")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val songUrl = intent?.getStringExtra(EXTRA_SONG_URL)

        when (action) {
            ACTION_PLAY -> {
                if (!songUrl.isNullOrEmpty()) {
                    currentTitle = intent.getStringExtra(EXTRA_SONG_TITLE) ?: ""
                    currentArt = intent.getIntExtra(
                        EXTRA_ALBUM_ART,
                        R.drawable.ic_launcher_foreground
                    )
                    currentUrl = songUrl
                    CoroutineScope(Dispatchers.Main).launch {
                        playerManager.playSuspend(songUrl) {
                            sendLocalBroadcast(ACTION_NEXT)  // autoplay next when song finishes
                        }
                        startForeground(NOTIFICATION_ID, buildNotification(true))
                    }
                }
            }

            ACTION_PAUSE -> {
                playerManager.pause()
                startForeground(NOTIFICATION_ID, buildNotification(false))
            }

            ACTION_REWIND -> {
                playerManager.rewind10Seconds()
            }

            ACTION_SEEK -> {
                val position = intent.getIntExtra(EXTRA_SEEK_POSITION, 0)
                playerManager.seekTo(position)
            }

            ACTION_NEXT, ACTION_PREV -> {
                sendLocalBroadcast(action) // no playback here
            }
        }

        return START_STICKY
    }

    private fun sendLocalBroadcast(action: String) {
        val intent = Intent(action)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val playPauseIntent = Intent(this, MusicService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
            putExtra(EXTRA_SONG_TITLE, currentTitle)
            putExtra(EXTRA_ALBUM_ART, currentArt)
            putExtra(EXTRA_SONG_URL, currentUrl)
        }

        val playPausePendingIntent = PendingIntent.getService(
            this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val nextPendingIntent = PendingIntent.getService(
            this, 1, Intent(this, MusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_IMMUTABLE
        )

        val prevPendingIntent = PendingIntent.getService(
            this, 2, Intent(this, MusicService::class.java).setAction(ACTION_PREV),
            PendingIntent.FLAG_IMMUTABLE
        )

        val rewindPendingIntent = PendingIntent.getService(
            this, 3, Intent(this, MusicService::class.java).setAction(ACTION_REWIND),
            PendingIntent.FLAG_IMMUTABLE
        )

        val albumArt = BitmapFactory.decodeResource(resources, currentArt)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText("Musy Player")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(albumArt)
            .addAction(R.drawable.baseline_skip_previous_24, "Prev", prevPendingIntent)
            .addAction(R.drawable.baseline_fast_rewind_24, "Rewind", rewindPendingIntent)
            .addAction(
                if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            .addAction(R.drawable.baseline_skip_next_24, "Next", nextPendingIntent)
            .setStyle(MediaStyle().setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0, 2, 3))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Musy Playback", NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        playerManager.release()
        super.onDestroy()
    }
}
