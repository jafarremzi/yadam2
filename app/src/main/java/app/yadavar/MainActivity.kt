package app.yadavar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.yadavar.reminder.EXTRA_NOTE_ID
import app.yadavar.ui.MainViewModel
import app.yadavar.ui.YadAvarRoot

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val settings by vm.settings.collectAsState()
            val dark = when (settings.theme) {
                1 -> true
                2 -> false
                else -> isSystemInDarkTheme()
            }
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            YadAvarRoot(vm, dark)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    override fun onStop() {
        super.onStop()
        vm.cancelListening() // میکروفون هرگز پس‌زمینه فعال نمی‌ماند
    }

    private fun handleIntent(i: Intent?) {
        val id = i?.getLongExtra(EXTRA_NOTE_ID, -1L) ?: -1L
        if (id >= 0) {
            vm.openNote(id)
            i?.removeExtra(EXTRA_NOTE_ID)
        }
    }
}
