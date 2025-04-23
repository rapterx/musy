package com.example.musy.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.example.musy.service.MusicService
import com.example.musy.data.model.Song
import com.example.musy.data.repository.MusicRepository
import com.example.musy.service.MusicPlayerManager
import kotlinx.coroutines.*

class MusicViewModel(private val context: Context) : ViewModel() {
    private val repository = MusicRepository()
    val songs: LiveData<List<Song>> = repository.songs

    private val _currentIndex = MutableLiveData(0)
    val currentIndex: LiveData<Int> get() = _currentIndex

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private val _duration = MutableLiveData(0)
    val duration: LiveData<Int> get() = _duration

    private val _position = MutableLiveData(0)
    val position: LiveData<Int> get() = _position

    private val recentlyPlayed = mutableListOf<Int>()
    private val recentLimit = 5

    val playerManager =  MusicPlayerManager()

    init {
        songs.observeForever { list ->
            if (list.isNotEmpty() && _currentIndex.value == 0) {
                playSongAt(0)
            }
        }
    }

    fun searchSongs(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.fetchSongs(query)
        }
    }

    fun togglePlayPause() {
        val song = currentSong() ?: return
        val action = if (_isPlaying.value == true) MusicService.ACTION_PAUSE else MusicService.ACTION_PLAY
        playerManager.togglePlayPause()

        if (_isPlaying.value == true) {
            _isPlaying.value = false
        } else {
            _isPlaying.value = true
        }

        sendServiceCommand(action, song)

    }

    fun playSongAt(index: Int) {
        val list = songs.value ?: return
        if (index !in list.indices) return
        _currentIndex.value = index
        _isPlaying.value = true
        startPlayback(list[index])
    }

    fun playNext() {
        val list = songs.value ?: return
        val current = _currentIndex.value ?: 0
        val nextIndex = if (current < list.lastIndex) current + 1 else 0
        playSongAt(nextIndex)
    }

    fun playPrev() {
        val list = songs.value ?: return
        val current = _currentIndex.value ?: 0
        val prevIndex = if (current > 0) current - 1 else list.lastIndex
        playSongAt(prevIndex)
    }

    fun rewind10() {
        sendServiceCommand(MusicService.ACTION_REWIND, currentSong())
    }

    fun seekTo(positionMs: Int) {
        val intent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_SEEK
            putExtra(MusicService.EXTRA_SEEK_POSITION, positionMs)
        }
        ContextCompat.startForegroundService(context, intent)
        _position.value = positionMs
    }

    private fun startPlayback(song: Song) {
        sendServiceCommand(MusicService.ACTION_PLAY, song)
        _duration.value = 30000 // 30 seconds preview by default, will be updated via broadcast
        _position.value = 0
    }

    private fun currentSong(): Song? {
        return songs.value?.getOrNull(_currentIndex.value ?: 0)
    }

    private fun sendServiceCommand(action: String, song: Song?) {
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
            song?.let {
                putExtra(MusicService.EXTRA_SONG_TITLE, it.title)
                putExtra(MusicService.EXTRA_ALBUM_ART, it.albumArt)
                putExtra(MusicService.EXTRA_SONG_URL, it.previewUrl)
            }
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun updatePlaybackStateFromService(isPlaying: Boolean, position: Int) {
        _isPlaying.value = isPlaying
        _position.value = position
    }
}
