package app.yadavar.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import app.yadavar.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val ACTION_DONE = "app.yadavar.action.DONE"
const val ACTION_SNOOZE = "app.yadavar.action.SNOOZE"
const val SNOOZE_MINUTES = 5L

/** دکمه‌های اعلان آلارم: «انجام شد» و «۵ دقیقه بعد». */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (id < 0) return
        val app = context.applicationContext
        val action = intent.action
        // با بستن اعلان، زنگ هم همان لحظه قطع می‌شود.
        NotificationManagerCompat.from(app).cancel(id.toInt())
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(app).noteDao()
                val note = dao.getById(id)
                if (note != null) {
                    when (action) {
                        ACTION_DONE -> dao.update(note.copy(done = true))
                        ACTION_SNOOZE -> {
                            val at = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
                            dao.update(note.copy(remindAt = at, fired = false, done = false))
                            ReminderScheduler(app).schedule(id, at)
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
