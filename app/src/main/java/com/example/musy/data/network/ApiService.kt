package com.example.musy.data.network

import com.example.musy.data.model.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface ApiService {
    @Headers("x-rapidapi-key: 5df0588496msh3b24b7657c0968bp1f7493jsna3c7f750f31d",
    "x-rapidapi-host: deezerdevs-deezer.p.rapidapi.com")
    @GET("search")
    fun searchSongs(@Query("q") query: String): Call<ApiResponse>
}