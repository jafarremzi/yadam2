package app.yadavar.reminder

import app.yadavar.data.Repeat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderMath {
    /** نوبت بعدی یادآوری تکرارشونده؛ همیشه بعد از nowMillis. */
    fun nextOccurrence(fromMillis: Long, repeat: Int, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val step = if (repeat == Repeat.WEEKLY) 7L else 1L
        var dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(fromMillis), zone)
        var guard = 0
        do {
            dt = dt.plusDays(step)
            guard++
        } while (dt.atZone(zone).toInstant().toEpochMilli() <= nowMillis && guard < 5000)
        return dt.atZone(zone).toInstant().toEpochMilli()
    }
}
