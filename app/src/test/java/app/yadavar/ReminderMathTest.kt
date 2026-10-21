package app.yadavar

import app.yadavar.data.Repeat
import app.yadavar.reminder.ReminderMath
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderMathTest {
    private val zone = ZoneId.of("Asia/Tehran")
    private fun ms(d: LocalDateTime) = d.atZone(zone).toInstant().toEpochMilli()

    @Test fun dailyAdvancesPastNow() {
        val from = ms(LocalDateTime.of(2026, 10, 1, 9, 0))
        val now = ms(LocalDateTime.of(2026, 10, 20, 14, 0))
        assertEquals(ms(LocalDateTime.of(2026, 10, 21, 9, 0)), ReminderMath.nextOccurrence(from, Repeat.DAILY, now, zone))
    }
    @Test fun weeklyKeepsWeekday() {
        val from = ms(LocalDateTime.of(2026, 10, 1, 9, 0)) // پنجشنبه
        val now = ms(LocalDateTime.of(2026, 10, 20, 14, 0))
        assertEquals(ms(LocalDateTime.of(2026, 10, 22, 9, 0)), ReminderMath.nextOccurrence(from, Repeat.WEEKLY, now, zone))
    }
}
