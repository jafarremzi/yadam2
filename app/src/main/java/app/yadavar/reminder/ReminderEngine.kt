package app.yadavar.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.yadavar.data.AppDatabase
import app.yadavar.data.NoteEntity
import app.yadavar.data.Repeat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ReminderEngine {
    suspend fun onFire(context: Context, id: Long) {
        val dao = AppDatabase.get(context).noteDao()
        val note = dao.getById(id) ?: return
        if (note.done || note.remindAt == null) return
        NotificationHelper.show(context, note, missed = false)
        finishOrAdvance(context, note)
    }

    /** بعد از نمایش اعلان: یادآوری ساده «انجام‌شده‌ی اعلان» می‌شود، تکرارشونده به نوبت بعد می‌رود. */
    private suspend fun finishOrAdvance(context: Context, note: NoteEntity) {
        val dao = AppDatabase.get(context).noteDao()
        val at = note.remindAt ?: return
        if (note.repeat == Repeat.NONE) {
            dao.update(note.copy(fired = true))
        } else {
            val next = ReminderMath.nextOccurrence(at, note.repeat, System.currentTimeMillis())
            dao.update(note.copy(remindAt = next, fired = false))
            ReminderScheduler(context).schedule(note.id, next)
        }
    }

    /** بعد از ریبوت، به‌روزرسانی برنامه، تغییر ساعت/منطقه زمانی یا باز شدن برنامه. */
    suspend fun rescheduleAll(context: Context, showMissed: Boolean) {
        val dao = AppDatabase.get(context).noteDao()
        val scheduler = ReminderScheduler(context)
        val now = System.currentTimeMillis()
        for (n in dao.getActiveReminders()) {
            val at = n.remindAt ?: continue
            if (at > now) {
                scheduler.schedule(n.id, at)
            } else if (!n.fired) {
                if (showMissed) NotificationHelper.show(context, n, missed = true)
                finishOrAdvance(context, n)
            }
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (id < 0) return
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { ReminderEngine.onFire(app, id) } finally { pending.finish() }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { ReminderEngine.rescheduleAll(app, showMissed = true) } finally { pending.finish() }
        }
    }
}
