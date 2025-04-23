package com.example.musy.service

import android.media.MediaPlayer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MusicPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var onSongComplete: (() -> Unit)? = null

    /**
     * Suspend function to play music with completion callback.
     */
    suspend fun playSuspend(url: String, onCompletion: () -> Unit = {}) {
        release() // Ensure no leaks

        onSongComplete = onCompletion

        suspendCancellableCoroutine<Unit> { cont ->
            val mp = MediaPlayer().apply {
                setDataSource(url)

                setOnPreparedListener {
                    it.start()
                    if (cont.isActive) cont.resume(Unit)
                }

                setOnCompletionListener {
                    onSongComplete?.invoke()
                }

                setOnErrorListener { _, what, extra ->
                    if (cont.isActive) {
                        cont.resumeWithException(Exception("MediaPlayer error: $what, $extra"))
                    }
                    true
                }

                prepareAsync()
            }

            cont.invokeOnCancellation { mp.release() }
            mediaPlayer = mp
        }
    }

    /**
     * Toggle playback state.
     */
    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.start()
            }
        }
    }

    /**
     * Rewind 10 seconds safely.
     */
    fun rewind10Seconds() {
        mediaPlayer?.let {
            val newPosition = (it.currentPosition - 10_000).coerceAtLeast(0)
            it.seekTo(newPosition)
        }
    }

    /**
     * Getters for duration and current position.
     */
    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    /**
     * Seek to a position in milliseconds.
     */
    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
    }

    /**
     * Pause playback.
     */
    fun pause() {
        mediaPlayer?.pause()
    }

    /**
     * Resume playback.
     */
    fun resume() {
        mediaPlayer?.start()
    }

    /**
     * Release the media player resources.
     */
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
