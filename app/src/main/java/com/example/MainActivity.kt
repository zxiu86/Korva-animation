package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.KorvaHomeScreen
import com.example.ui.screens.KorvaStudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioObsidianDark
import com.example.viewmodel.AppScreen
import com.example.viewmodel.KorvaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: KorvaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()

                BackHandler(enabled = currentScreen == AppScreen.STUDIO) {
                    viewModel.navigateToHome()
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = StudioObsidianDark
                ) {
                    Crossfade(
                        targetState = currentScreen,
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.HOME -> KorvaHomeScreen(viewModel = viewModel)
                            AppScreen.STUDIO -> KorvaStudioScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}


