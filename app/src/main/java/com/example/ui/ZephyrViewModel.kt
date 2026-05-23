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

    fun setTempThresh(v: Float) = executeAction { ZephyrClient.api.setTempThresh(v) }
    fun setHumThresh(v: Int) = executeAction { ZephyrClient.api.setHumThresh(v) }
    fun setMode(mode: String) = executeAction { ZephyrClient.api.setMode(mode) }
    fun setFan(on: Boolean) = executeAction { ZephyrClient.api.setFan(if (on) 1 else 0) }
    fun setPump(on: Boolean) = executeAction { ZephyrClient.api.setPump(if (on) 1 else 0) }
    fun setSched(en: Boolean) = executeAction { ZephyrClient.api.setSched(en = if (en) 1 else 0) }
    fun setBrightness(v: Int) = executeAction { ZephyrClient.api.setBrightness(v) }

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
