# 🎵 Musy — Android Music Player App

**Musy** is a modern Android music streaming app built using **MVVM architecture**, **Kotlin Coroutines**, and **Retrofit**. It features **media-style notifications** for background playback—just like Spotify—with full playback controls including next, previous, pause/resume, and seek bar support.

---

## 📱 Features

- 🎧 Search and stream songs from an API
- 🎵 MVVM architecture with ViewModel and LiveData
- ⚙️ Background playback using a `ForegroundService`
- 🔊 Media-style notifications with:
  - Album art
  - Current song title
  - Play/Pause
  - Next/Previous
  - Rewind 10s
  - Seek bar support
- 🧠 Uses Retrofit for API calls and Kotlin Coroutines for async tasks

---

## 🧱 Architecture

This project follows the **MVVM (Model-View-ViewModel)** architecture pattern. Here's how the layers are structured:

### 🔄 Flow Overview

1. `MainActivity` observes data from `MusicViewModel`.
2. `MusicViewModel` calls `MusicRepository` to fetch songs via `Retrofit`.
3. `PlayerManager` controls playback, wrapped by `MusicService`.
4. `MusicService` listens to playback controls (from UI or Notification).
5. Foreground `Notification` updates UI and allows media control.

This architecture ensures separation of concerns, scalability, and testability.

