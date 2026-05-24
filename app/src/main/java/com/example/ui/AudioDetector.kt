package com.example.ui

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.log10
import kotlin.math.sqrt

object AudioDetector {
    private const val SAMPLE_RATE = 44100
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    @SuppressLint("MissingPermission")
    fun startListening(): Flow<Double> = flow {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            return@flow
        }

        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (e: Exception) {
            return@flow
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            return@flow
        }

        val buffer = ShortArray(bufferSize)
        
        try {
            audioRecord.startRecording()
        } catch (e: Exception) {
            try { audioRecord.release() } catch (e2: Exception) {}
            return@flow
        }

        try {
            while (true) {
                val readSize = audioRecord.read(buffer, 0, buffer.size)
                if (readSize > 0) {
                    var sum = 0.0
                    for (i in 0 until readSize) {
                        sum += buffer[i] * buffer[i]
                    }
                    val amplitude = sqrt(sum / readSize)
                    val db = if (amplitude > 0) 20 * log10(amplitude) else 0.0
                    emit(db)
                }
                delay(50)
            }
        } finally {
            try {
                audioRecord.stop()
            } catch (e: Exception) {}
            try {
                audioRecord.release()
            } catch (e: Exception) {}
        }
    }.flowOn(Dispatchers.IO)
}
