package com.example.musy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaSessionCompat
import com.example.musy.R

class MusicService : Service() {

    private lateinit var mediaSession: MediaSessionCompat
    private val playerManager = MusicPlayerManager.getInstance()

    private var currentTitle: String = "Unknown Title"
    private var currentArt: Int = R.drawable.ic_launcher_foreground // default art

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREV = "ACTION_PREV"
        const val ACTION_UPDATE_TRACK = "ACTION_UPDATE_TRACK"
    }


    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "MusySession")
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Musy")
            .setContentText("Playing music")
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // Make sure this icon exists!!
            .build()

        startForeground(1, notification)  // 1 is your notification ID
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Musy Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                playerManager.resume()
                sendPlaybackState(true)
                updateNotification(isPlaying = true)
            }
            ACTION_PAUSE -> {
                playerManager.pause()
                sendPlaybackState(false)
                updateNotification(isPlaying = false)
            }
            ACTION_NEXT -> {
                sendBroadcast(Intent("MUSIC_NEXT"))
                updateNotification(isPlaying = true)
            }
            ACTION_PREV -> {
                sendBroadcast(Intent("MUSIC_PREV"))
                updateNotification(isPlaying = true)
            }
            ACTION_UPDATE_TRACK -> {
                val newTitle = intent.getStringExtra("songTitle") ?: "Unknown Title"
                val newAlbumArt = intent.getIntExtra("albumArt", R.drawable.ic_launcher_foreground)
                updateCurrentTrack(newTitle, newAlbumArt)

                // Rebuild notification immediately
                startForeground(1, buildNotification(isPlaying = playerManager.isPlaying()))
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun sendPlaybackState(isPlaying: Boolean) {
        val intent = Intent("PLAYBACK_STATE_CHANGED").apply {
            putExtra("isPlaying", isPlaying)
        }
        sendBroadcast(intent)
    }


    private fun updateNotification(isPlaying: Boolean) {
        val notification = buildNotification(isPlaying)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
        mediaSession.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(isPlaying: Boolean): Notification {
        val albumArt = BitmapFactory.decodeResource(resources, currentArt)

        val playPauseIntent = createServicePendingIntent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY)
        val nextIntent = createServicePendingIntent(ACTION_NEXT)
        val prevIntent = createServicePendingIntent(ACTION_PREV)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText("Musy Player")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(albumArt)
            .addAction(R.drawable.baseline_skip_previous_24, "Previous", prevIntent) // ADD THIS
            .addAction(
                if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(R.drawable.baseline_skip_next_24, "Next", nextIntent) // ADD THIS
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2) // <-- Show Prev, Play/Pause, Next
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .build()
    }


    private fun createServicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(), // unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // helper to update song info from ViewModel or elsewhere
    fun updateCurrentTrack(title: String, artResId: Int) {
        currentTitle = title
        currentArt = artResId
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
