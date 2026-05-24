package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.ZephyrClient
import com.example.api.ZephyrState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

sealed class ZephyrUiState {
    object Loading : ZephyrUiState()
    data class Success(val state: ZephyrState) : ZephyrUiState()
    data class Error(val message: String) : ZephyrUiState()
}

class ZephyrViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ZephyrUiState>(ZephyrUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _ipAddress = MutableStateFlow(ZephyrClient.currentBaseUrl)
    val ipAddress = _ipAddress.asStateFlow()

    private val _isMicEnabled = MutableStateFlow(false)
    val isMicEnabled = _isMicEnabled.asStateFlow()

    private val _appTheme = MutableStateFlow("auto")
    val appTheme = _appTheme.asStateFlow()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    val result = ZephyrClient.api.getState()
                    _uiState.update { ZephyrUiState.Success(result) }
                } catch (e: Exception) {
                    if (_uiState.value !is ZephyrUiState.Success) {
                        _uiState.update { ZephyrUiState.Error(e.message ?: "Failed to connect to device") }
                    }
                }
                delay(2000)
            }
        }
    }

    fun updateIpAddress(ip: String) {
        val cleaned = ip.trim()
        ZephyrClient.updateBaseUrl(cleaned)
        _ipAddress.update { ZephyrClient.currentBaseUrl }
        _uiState.update { ZephyrUiState.Loading } 
    }

    fun setTheme(theme: String) {
        _appTheme.update { theme }
    }

    fun setMicEnabled(enabled: Boolean) {
        _isMicEnabled.update { enabled }
    }

    fun sendVoiceCmd(cmd: String) = executeAction { ZephyrClient.api.voice(cmd) }

    fun updateFirmware(context: android.content.Context, uri: android.net.Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    onResult("Could not read file")
                    return@launch
                }
                
                val mediaType = "application/octet-stream".toMediaTypeOrNull()
                val requestFile = bytes.toRequestBody(mediaType)
                val body = okhttp3.MultipartBody.Part.createFormData("image", "firmware.bin", requestFile)
                val auth = okhttp3.Credentials.basic("admin", "zephyrota")
                
                val response = ZephyrClient.api.updateFirmware(auth, body)
                if (response.isSuccessful) {
                    onResult("Update successful! Device is restarting...")
                } else {
                    onResult("Update failed: ${response.code()}")
                }
            } catch (e: Exception) {
                onResult("Update error: ${e.message}")
            }
        }
    }

    private fun sendSoundCmd(c: Int) = executeAction { ZephyrClient.api.soundCmd(c) }

    fun setTempThresh(v: Float) = executeAction { ZephyrClient.api.setTempThresh(v) }
    fun setHumThresh(v: Int) = executeAction { ZephyrClient.api.setHumThresh(v) }
    fun setTankH(v: Float) = executeAction { ZephyrClient.api.setTankH(v) }
    fun setMode(mode: String) = executeAction { ZephyrClient.api.setMode(mode) }
    fun setFan(on: Boolean) = executeAction { ZephyrClient.api.setFan(if (on) 1 else 0) }
    fun setPump(on: Boolean) = executeAction { ZephyrClient.api.setPump(if (on) 1 else 0) }
    fun setSched(en: Boolean) = executeAction { ZephyrClient.api.setSched(en = if (en) 1 else 0) }
    fun setBrightness(v: Int) = executeAction { ZephyrClient.api.setBrightness(v) }
    fun setLed(anim: String, r: Int, g: Int, b: Int, func: String = "") = executeAction { ZephyrClient.api.setLed(anim, r, g, b, func) }

    private fun executeAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                val result = ZephyrClient.api.getState()
                _uiState.update { ZephyrUiState.Success(result) }
            } catch (e: Exception) {
            }
        }
    }
}
