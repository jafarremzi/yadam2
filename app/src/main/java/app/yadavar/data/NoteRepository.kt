package app.yadavar.data

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import app.yadavar.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow

data class SaveOutcome(val scheduled: Boolean, val exact: Boolean)

class NoteRepository(context: Context) {
    private val ctx = context.applicationContext
    private val dao = AppDatabase.get(ctx).noteDao()
    private val scheduler = ReminderScheduler(ctx)

    val notes: Flow<List<NoteEntity>> = dao.observeAll()

    suspend fun get(id: Long): NoteEntity? = dao.getById(id)

    suspend fun save(note: NoteEntity): SaveOutcome {
        val id = if (note.id == 0L) dao.insert(note) else { dao.update(note); note.id }
        val at = note.remindAt
        if (at != null && !note.done && at > System.currentTimeMillis()) {
            return SaveOutcome(scheduled = true, exact = scheduler.schedule(id, at))
        }
        cancelAlarmAndNotification(id)
        return SaveOutcome(scheduled = false, exact = true)
    }

    suspend fun delete(note: NoteEntity) {
        cancelAlarmAndNotification(note.id)
        dao.delete(note)
    }

    suspend fun setDone(note: NoteEntity, done: Boolean) {
        dao.update(note.copy(done = done))
        val at = note.remindAt
        if (done || at == null) cancelAlarmAndNotification(note.id)
        else if (at > System.currentTimeMillis()) scheduler.schedule(note.id, at)
    }

    private fun cancelAlarmAndNotification(id: Long) {
        scheduler.cancel(id)
        NotificationManagerCompat.from(ctx).cancel(id.toInt())
    }
}
