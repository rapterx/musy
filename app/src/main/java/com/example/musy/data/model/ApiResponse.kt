package com.example.musy.data.model

data class ApiResponse(
    val data: List<SongDto>
)

data class SongDto(
    val id: String,
    val title: String,
    val artist: Artist,
    val album: Album,
    val preview: String
) {
    fun toSong(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist.name,
            albumArt = album.cover,
            previewUrl = preview
        )
    }
}

data class Artist(val name: String)
data class Album(val cover: String)
