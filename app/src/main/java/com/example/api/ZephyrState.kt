package com.example.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ZephyrState(
    val temp: Float = 0f,
    val hum: Float = 0f,
    val water: Int = 0,
    val dist: Float = 0f,
    val fan: Boolean = false,
    val pump: Boolean = false,
    val mode: String = "auto",
    val tempTh: Float = 30.0f,
    val humTh: Int = 75,
    val br: Int = 80,
    val waterLow: Boolean = false,
    val sched: Boolean = false,
    val schedOnH: Int = 9,
    val schedOnM: Int = 0,
    val schedOffH: Int = 18,
    val schedOffM: Int = 0,
    val curH: Int = 0,
    val curM: Int = 0,
    val curS: Int = 0,
    val timeSet: Boolean = false,
    val inSched: Boolean = true,
    val pause: String = "",
    val leds: List<String> = emptyList()
)
