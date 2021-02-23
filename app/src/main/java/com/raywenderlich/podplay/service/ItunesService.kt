package com.raywenderlich.podplay.service

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesService {

    @GET("/search?media=podcast")
    fun searchPodcastByTerm(@Query("term") term: String): Call<PodcastResponse>

    //Returns an instance as singleton. This ensures that the interface is only instantiated once during the app’s
    //lifetime.
    companion object {
        val instance: ItunesService by lazy {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://itunes.apple.com")
                .addConverterFactory(GsonConverterFactory.create()).build()
            retrofit.create<ItunesService>(ItunesService::class.java)
        }
    }
}