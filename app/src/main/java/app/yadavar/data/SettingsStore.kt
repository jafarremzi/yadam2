package app.yadavar.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** theme: 0 سیستم، 1 تیره (پیش‌فرض)، 2 روشن */
data class AppSettings(
    val theme: Int = 1,
    val use24h: Boolean = true,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
)

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(parse(prefs))
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun commit(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        _state.value = parse(prefs)
    }

    fun setTheme(v: Int) = commit { putInt("theme", v) }
    fun setUse24h(v: Boolean) = commit { putBoolean("use24h", v) }
    fun setSound(v: Boolean) = commit { putBoolean("sound", v) }
    fun setVibrate(v: Boolean) = commit { putBoolean("vibrate", v) }

    companion object {
        private const val FILE = "settings"
        private fun parse(p: SharedPreferences) = AppSettings(
            theme = p.getInt("theme", 1),
            use24h = p.getBoolean("use24h", true),
            sound = p.getBoolean("sound", true),
            vibrate = p.getBoolean("vibrate", true),
        )

        fun load(context: Context): AppSettings =
            parse(context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE))
    }
}
