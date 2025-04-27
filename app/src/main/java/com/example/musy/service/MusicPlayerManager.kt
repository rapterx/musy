package com.example.musy.service

import android.media.MediaPlayer

class MusicPlayerManager private constructor() {

    companion object {
        // Singleton instance
        @Volatile
        private var INSTANCE: MusicPlayerManager? = null

        // Thread-safe access to the singleton instance
        fun getInstance(): MusicPlayerManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: MusicPlayerManager().also { INSTANCE = it }
            }
    }

    private var mediaPlayer: MediaPlayer? = null


    fun play(url: String) {
        release() // Ensure that any existing player is released

        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener {
                it.start() // Start the song once prepared
            }
            prepareAsync() // Prepare asynchronously
        }
    }


    fun rewind10Seconds() {
        mediaPlayer?.let {
            val newPosition = (it.currentPosition - 10_000).coerceAtLeast(0)
            it.seekTo(newPosition)
        }
    }
    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun getDuration(): Int = mediaPlayer?.duration ?: 0


    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0


    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }


    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
