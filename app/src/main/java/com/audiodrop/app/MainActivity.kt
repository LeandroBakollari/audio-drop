package com.audiodrop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.audiodrop.app.ui.AudioDropApp
import com.audiodrop.app.ui.theme.AudioDropTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioDropTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AudioDropApp()
                }
            }
        }
    }
}
