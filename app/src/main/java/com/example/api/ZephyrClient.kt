package com.example.api

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ZephyrClient {
    private var baseUrl = "http://192.168.4.1/"
    
    private val moshi = Moshi.Builder()
        .build()
        
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()
        
    var api: ZephyrApi = createApi(baseUrl)
        private set
    
    fun updateBaseUrl(ip: String) {
        val url = if (ip.startsWith("http")) ip else "http://$ip/"
        baseUrl = if (url.endsWith("/")) url else "$url/"
        api = createApi(baseUrl)
    }
    
    val currentBaseUrl: String get() = baseUrl
    
    private fun createApi(url: String): ZephyrApi {
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ZephyrApi::class.java)
    }
}
