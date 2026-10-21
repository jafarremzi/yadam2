package app.yadavar.reminder

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.yadavar.MainActivity
import app.yadavar.R
import app.yadavar.data.NoteEntity
import app.yadavar.data.Repeat
import app.yadavar.data.SettingsStore

object NotificationHelper {
    // تنظیمات صدا/لرزش در اندروید ۸+ روی خود کانال است و پس از ساخت تغییر نمی‌کند؛
    // برای همین چهار کانال می‌سازیم و بر اساس تنظیمات کاربر یکی را انتخاب می‌کنیم.
    // کانال‌های نسخه‌ی قبل (اعلان ساده) حذف می‌شوند؛ کانال‌های جدید با صدای «آلارم» ساخته می‌شوند.
    private fun channelId(sound: Boolean, vibrate: Boolean) = "alarm_${if (sound) "s" else "q"}${if (vibrate) "v" else "n"}"
    private val OLD_CHANNELS = listOf("reminders_sv", "reminders_sn", "reminders_qv", "reminders_qn")

    /** مدت زنگ خوردن آلارم تا بسته‌شدن خودکار اعلان. */
    private const val ALARM_TIMEOUT_MS = 2 * 60 * 1000L

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        for (old in OLD_CHANNELS) nm.deleteNotificationChannel(old)
        val alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        for ((sound, vib) in listOf(true to true, true to false, false to true, false to false)) {
            val name = "یادآوری‌ها (" + (if (sound) "با صدا" else "بی‌صدا") + "، " + (if (vib) "با لرزش" else "بدون لرزش") + ")"
            val ch = NotificationChannel(channelId(sound, vib), name, NotificationManager.IMPORTANCE_HIGH)
            ch.description = "اعلان زمان‌دار یادآوری‌های ثبت‌شده"
            if (sound) ch.setSound(alarmUri, alarmAttrs) else ch.setSound(null, null)
            ch.enableVibration(vib)
            if (vib) ch.vibrationPattern = longArrayOf(0, 300, 200, 300)
            ch.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            nm.createNotificationChannel(ch)
        }
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, note: NoteEntity, missed: Boolean) {
        val s = SettingsStore.load(context)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse("yadavar://open/${note.id}")
            putExtra(EXTRA_NOTE_ID, note.id)
        }
        val pi = PendingIntent.getActivity(
            context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (missed) "یادآوری از دست رفته" else "یادآوری"
        val body = note.displayTitle
        val channel = channelId(s.sound, s.vibrate)
        val public = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("یک یادآوری دارید")
            .build()
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
        if (!missed) {
            builder.setTimeoutAfter(ALARM_TIMEOUT_MS)
            // «انجام شد» و «۵ دقیقه بعد» فقط برای یادآوری بدون تکرار؛ در تکرارشونده‌ها نوبت بعدی دست‌نخورده می‌ماند.
            if (note.repeat == Repeat.NONE) {
                builder.addAction(0, "انجام شد", actionIntent(context, note.id, ACTION_DONE))
                builder.addAction(0, "۵ دقیقه بعد", actionIntent(context, note.id, ACTION_SNOOZE))
            }
        }
        val n = builder.build()
        // زنگ تا لمس/بستن اعلان تکرار می‌شود (مثل آلارم)؛ برای یادآوری‌های از دست رفته فقط یک‌بار.
        if (!missed && s.sound) n.flags = n.flags or Notification.FLAG_INSISTENT
        try {
            NotificationManagerCompat.from(context).notify(note.id.toInt(), n)
        } catch (_: SecurityException) {
            // مجوز اعلان داده نشده است.
        }
    }

    private fun actionIntent(context: Context, id: Long, action: String): PendingIntent {
        val kind = if (action == ACTION_DONE) "done" else "snooze"
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("yadavar://action/$kind/$id"))
            .putExtra(EXTRA_NOTE_ID, id)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
