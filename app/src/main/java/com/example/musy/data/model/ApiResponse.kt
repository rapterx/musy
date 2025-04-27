package com.example.musy.data.model

data class ApiResponse(
    val data: List<SongDto>
)

data class SongDto(
    val id: String,
    val title: String,
    val artist: Artist,
    val album: Album,
    val preview: String,
    val duration: Int
) {
    fun toSong(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist.name,
            albumArt = album.cover,
            previewUrl = preview,
            duration = duration*1000
        )
    }
}

data class Artist(val name: String)
data class Album(val cover: String)
