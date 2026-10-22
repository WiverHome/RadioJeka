package com.wiverhome.radio

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

// Метки для тестов интерфейса (app/src/androidTest).
internal const val TAG_SEARCH = "search"
internal const val TAG_STATION = "station"
internal const val TAG_MINI_PLAYER = "miniPlayer"
internal const val TAG_MINI_TOGGLE = "miniToggle"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: RadioViewModel = viewModel()
            val theme by vm.theme.collectAsStateWithLifecycle()
            val dark = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Status/navigation bar icons follow the chosen theme, not the system one.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            RadioTheme(dark) { RadioScreen(vm) }
        }
    }
}

@Composable
private fun RadioTheme(dark: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        // Before Material You: brand colors from the launcher icon gradient.
        dark -> darkColorScheme(
            primary = Color(0xFFFFB2BE),
            onPrimary = Color(0xFF5F1027),
            primaryContainer = Color(0xFF7E2A3E),
            secondaryContainer = Color(0xFF5C3B42),
        )
        else -> lightColorScheme(
            primary = Color(0xFFC8294F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD9DE),
            secondaryContainer = Color(0xFFFFE3E6),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
