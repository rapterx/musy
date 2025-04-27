package com.example.musy.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.musy.R
import com.example.musy.databinding.ActivityMainBinding
import com.example.musy.service.MusicService
import com.example.musy.ui.viewmodel.MusicViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MusicViewModel
    private lateinit var songAdapter: SongAdapter

    private val musicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "MUSIC_NEXT" -> {
                    viewModel.playNext()
                    binding.viewPager.setCurrentItem(viewModel.currentIndex.value ?: 0, true)
                }
                "MUSIC_PREV" -> {
                    viewModel.playPrev()
                    binding.viewPager.setCurrentItem(viewModel.currentIndex.value ?: 0, true)
                }
                "PLAYBACK_STATE_CHANGED" -> {
                    val isPlaying = intent.getBooleanExtra("isPlaying", false)
                    viewModel.setIsPlaying(isPlaying)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction("MUSIC_NEXT")
            addAction("MUSIC_PREV")
            addAction("PLAYBACK_STATE_CHANGED")
        }
        registerReceiver(musicReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(musicReceiver)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start MusicService once
        val serviceIntent = Intent(this, MusicService::class.java)
        startForegroundService(serviceIntent)

        // Initialize ViewModel
        viewModel = MusicViewModel(applicationContext)

        // Fetch all songs initially
        viewModel.searchSongs("all")

        // Set up song ViewPager
        viewModel.songs.observe(this) { songs ->
            songAdapter = SongAdapter(songs)
            binding.viewPager.adapter = songAdapter
        }

        // ViewPager page change -> play selected song
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.playSongAt(position)
            }
        })

        // ViewModel song index change -> update ViewPager current item
        viewModel.currentIndex.observe(this) { index ->
            if (binding.viewPager.currentItem != index) {
                binding.viewPager.setCurrentItem(index, false)
            }

            val currentSong = viewModel.songs.value?.get(index)
            currentSong?.let {
                val intent = Intent(this, MusicService::class.java).apply {
                    action = "ACTION_UPDATE_TRACK"
                    putExtra("songTitle", it.title)
                    putExtra("albumArt", R.drawable.ic_launcher_foreground) // Replace with actual art if you have
                }
                startService(intent)
            }
        }

        // Update Play/Pause Button icon
        viewModel.isPlaying.observe(this) { isPlaying ->
            binding.playPauseBtn.setImageResource(
                if (isPlaying) R.drawable.baseline_pause_24
                else R.drawable.baseline_play_arrow_24
            )
        }

        // SeekBar updates
        viewModel.duration.observe(this) { dur ->
            binding.seekBar.max = dur
        }

        viewModel.position.observe(this) { pos ->
            if (!binding.seekBar.isPressed) {
                binding.seekBar.progress = pos
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Play/Pause button
        binding.playPauseBtn.setOnClickListener {
            viewModel.togglePlayPause()
        }

        // Rewind button
        binding.rewindBtn.setOnClickListener {
            viewModel.rewind10()
        }

        // Next button
        binding.nextBtn.setOnClickListener {
            viewModel.playNext()
            binding.viewPager.setCurrentItem(viewModel.currentIndex.value ?: 0, true)
        }

        // Previous button
        binding.prevBtn.setOnClickListener {
            viewModel.playPrev()
            binding.viewPager.setCurrentItem(viewModel.currentIndex.value ?: 0, true)
        }

        binding.shuffleButton.setOnClickListener {
            viewModel.playRandomSong()
        }
    }
}
