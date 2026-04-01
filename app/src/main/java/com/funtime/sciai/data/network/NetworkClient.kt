package com.funtime.sciai.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClient {
    // Replace with your current LAN IP
    private const val BASE_URL = "http://192.168.1.104:8000/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)   // Large files: wait up to 5 min for server to process PDF
        .writeTimeout(5, TimeUnit.MINUTES)  // Large files: allow up to 5 min to upload 36MB+ PDFs
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
