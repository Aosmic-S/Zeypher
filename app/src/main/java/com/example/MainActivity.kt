package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.ui.ZephyrScreen
import com.example.ui.theme.MyApplicationTheme

import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
  private val viewModel: com.example.ui.ZephyrViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      try {
        val file = java.io.File(cacheDir, "crash_log.txt")
        file.appendText("Crash in ${thread.name}: ${throwable.message}\n${throwable.stackTraceToString()}\n")
      } catch (e: Exception) {}
      throwable.printStackTrace()
      kotlin.system.exitProcess(1)
    }
    enableEdgeToEdge()
    setContent {
      val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
      val isDarkTheme = when (appTheme) {
          "dark" -> true
          "light" -> false
          else -> isSystemInDarkTheme()
      }
      MyApplicationTheme(darkTheme = isDarkTheme) {
        val showSplash = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
        if (showSplash.value) {
            com.example.ui.IntroScreen(onFinished = { showSplash.value = false })
        } else {
            ZephyrScreen(viewModel = viewModel)
        }
      }
    }
  }
}


