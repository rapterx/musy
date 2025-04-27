package com.example.musy.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.example.musy.service.MusicPlayerManager
import com.example.musy.service.MusicService
import com.example.musy.data.model.Song
import com.example.musy.data.repository.MusicRepository
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

    private var progressJob: Job? = null

    private val playerManager = MusicPlayerManager.getInstance()

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
        val currentlyPlaying = _isPlaying.value ?: false
        if(currentlyPlaying) stopUpdatingPosition()
        else startUpdatingPosition()
        val action = if (currentlyPlaying) MusicService.ACTION_PAUSE else MusicService.ACTION_PLAY

        sendMusicServiceCommand(action)

        _isPlaying.value = !currentlyPlaying
    }
    fun setIsPlaying(playing: Boolean) {
        _isPlaying.value = playing
        if (playing) startUpdatingPosition()
        else stopUpdatingPosition()
    }


    fun playSongAt(index: Int) {
        val list = songs.value ?: return
        if (index !in list.indices) return
        _currentIndex.value = index
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
        playerManager.rewind10Seconds()
    }

    fun seekTo(positionMs: Int) {
        playerManager.seekTo(positionMs)
        _position.postValue(positionMs)
    }

    fun playRandomSong() {
        val songList = songs.value ?: return
        if (songList.isEmpty()) return

        val availableIndexes = songList.indices
            .filter { it !in recentlyPlayed }
            .ifEmpty { songList.indices.toList() }

        val randomIndex = availableIndexes.random()

        recentlyPlayed.add(randomIndex)
        if (recentlyPlayed.size > recentLimit) {
            recentlyPlayed.removeAt(0)
        }

        playSongAt(randomIndex)
    }


    private fun startPlayback(song: Song) {
        playerManager.play(song.previewUrl)
        _duration.value = 30000
        _position.value = 0
        _isPlaying.value = true

        startUpdatingPosition()
        sendMusicServiceCommand(MusicService.ACTION_PLAY)
    }

    private fun currentSong(): Song? {
        return songs.value?.getOrNull(_currentIndex.value ?: 0)
    }

    private fun sendMusicServiceCommand(action: String) {
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun startUpdatingPosition() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && (_isPlaying.value == true)) {
                _position.postValue(playerManager.getCurrentPosition())
                delay(500)
            }
        }
    }



    private fun stopUpdatingPosition() {
        progressJob?.cancel()
    }


    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        playerManager.release()
    }
}
