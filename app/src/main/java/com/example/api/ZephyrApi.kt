package com.example.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ZephyrApi {
    @GET("api")
    suspend fun getState(): ZephyrState

    @GET("setTempThresh")
    suspend fun setTempThresh(@Query("v") v: Float): Response<ResponseBody>

    @GET("setHumThresh")
    suspend fun setHumThresh(@Query("v") v: Int): Response<ResponseBody>

    @GET("setMode")
    suspend fun setMode(@Query("v") mode: String): Response<ResponseBody> 

    @GET("setFan")
    suspend fun setFan(@Query("on") on: Int): Response<ResponseBody>

    @GET("setPump")
    suspend fun setPump(@Query("on") on: Int): Response<ResponseBody>

    @GET("setSched")
    suspend fun setSched(
        @Query("en") en: Int? = null,
        @Query("onH") onH: Int? = null,
        @Query("onM") onM: Int? = null,
        @Query("offH") offH: Int? = null,
        @Query("offM") offM: Int? = null
    ): Response<ResponseBody>

    @GET("setTime")
    suspend fun setTime(@Query("h") h: Int, @Query("m") m: Int, @Query("s") s: Int): Response<ResponseBody>

    @GET("setTankH")
    suspend fun setTankH(@Query("v") v: Float): Response<ResponseBody>

    @GET("setBrightness")
    suspend fun setBrightness(@Query("v") v: Int): Response<ResponseBody>

    @GET("soundCmd")
    suspend fun soundCmd(@Query("c") c: Int): Response<ResponseBody>

    @GET("voice")
    suspend fun voice(@Query("cmd") cmd: String): Response<ResponseBody>

    @GET("setLed")
    suspend fun setLed(@Query("anim") anim: String, @Query("r") r: Int, @Query("g") g: Int, @Query("b") b: Int, @Query("func") func: String = ""): Response<ResponseBody>

    @retrofit2.http.Multipart
    @retrofit2.http.POST("update")
    suspend fun updateFirmware(
        @retrofit2.http.Header("Authorization") auth: String,
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part
    ): Response<ResponseBody>
}
