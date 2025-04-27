package com.example.musy.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.musy.data.model.ApiResponse
import com.example.musy.data.model.Song
import com.example.musy.data.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MusicRepository {
    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> = _songs

    suspend fun fetchSongs(query: String) {
        RetrofitClient.apiService.searchSongs(query).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    _songs.postValue(response.body()?.data?.map { it.toSong() })
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                // Handle error
            }
        })
    }
}
