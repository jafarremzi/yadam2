package app.yadavar.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

const val EXTRA_NOTE_ID = "note_id"
const val ACTION_REMIND = "app.yadavar.action.REMIND"

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * یک PendingIntent یکتا برای هر یادداشت (با data=Uri) ساخته می‌شود؛ زمان‌بندی دوباره
     * همان آلارم را جایگزین می‌کند و آلارم تکراری ایجاد نمی‌شود.
     * خروجی true یعنی آلارم دقیق ثبت شد؛ false یعنی به‌دلیل نبود مجوز، آلارم تقریبی ثبت شد.
     */
    fun schedule(id: Long, atMillis: Long): Boolean {
        val pi = pendingIntent(id)
        if (canScheduleExact()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
                return true
            } catch (_: SecurityException) {
                // مجوز در فاصله بررسی و ثبت لغو شده است؛ به آلارم تقریبی برمی‌گردیم.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        return false
    }

    fun cancel(id: Long) {
        alarmManager.cancel(pendingIntent(id))
    }

    private fun pendingIntent(id: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .setData(Uri.parse("yadavar://reminder/$id"))
            .putExtra(EXTRA_NOTE_ID, id)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
