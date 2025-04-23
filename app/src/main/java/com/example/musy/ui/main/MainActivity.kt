package com.example.musy.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.widget.ViewPager2
import com.example.musy.service.MusicService
import com.example.musy.R
import com.example.musy.databinding.ActivityMainBinding
import com.example.musy.ui.viewmodel.MusicViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MusicViewModel
    private lateinit var songAdapter: SongAdapter

    private val mediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicService.ACTION_PLAY -> viewModel.updatePlaybackStateFromService(true, viewModel.position.value ?: 0)
                MusicService.ACTION_PAUSE -> viewModel.updatePlaybackStateFromService(false, viewModel.position.value ?: 0)
                MusicService.ACTION_NEXT -> {
                    viewModel.playNext()
                    binding.viewPager.setCurrentItem(viewModel.currentIndex.value ?: 0, true)
                }
                MusicService.ACTION_PREV -> {
                    viewModel.playPrev()
                    binding.viewPager.setCurrentItem(viewModel.currentIndex.value ?: 0, true)
                }
                MusicService.ACTION_REWIND -> viewModel.rewind10()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init ViewModel and inject context manually
        viewModel = MusicViewModel(applicationContext)

        // Register media receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mediaReceiver,
            IntentFilter().apply {
                addAction(MusicService.ACTION_PLAY)
                addAction(MusicService.ACTION_PAUSE)
                addAction(MusicService.ACTION_NEXT)
                addAction(MusicService.ACTION_PREV)
                addAction(MusicService.ACTION_REWIND)
            }
        )

        // Load songs
        viewModel.searchSongs("all")

        // Observe songs and set ViewPager adapter
        viewModel.songs.observe(this) { songs ->
            songAdapter = SongAdapter(songs)
            binding.viewPager.adapter = songAdapter
        }

        // ViewPager song sync
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.playSongAt(position)
            }
        })

        // Sync ViewPager position when currentIndex changes
        viewModel.currentIndex.observe(this) { index ->
            if (binding.viewPager.currentItem != index) {
                binding.viewPager.setCurrentItem(index, false)
            }
        }

        // Play/pause button icon
        viewModel.isPlaying.observe(this) { isPlaying ->
            binding.playPauseBtn.setImageResource(
                if (isPlaying) R.drawable.baseline_pause_24
                else R.drawable.baseline_play_arrow_24
            )
        }

        // Duration and seek updates
        viewModel.duration.observe(this) { dur -> binding.seekBar.max = dur }
        viewModel.position.observe(this) { pos ->
            if (!binding.seekBar.isPressed) {
                binding.seekBar.progress = pos
            }
        }

        // SeekBar manual seeking
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) viewModel.seekTo(progress)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Button listeners
        binding.playPauseBtn.setOnClickListener { viewModel.togglePlayPause() }
        binding.rewindBtn.setOnClickListener { viewModel.rewind10() }
        binding.nextBtn.setOnClickListener {
            viewModel.playNext()
            binding.viewPager.setCurrentItem(viewModel.currentIndex.value ?: 0, true)
        }
        binding.prevBtn.setOnClickListener {
            viewModel.playPrev()
            binding.viewPager.setCurrentItem(viewModel.currentIndex.value ?: 0, true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mediaReceiver)
    }
}
