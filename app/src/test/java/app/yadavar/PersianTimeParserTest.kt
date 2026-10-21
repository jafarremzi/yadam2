package app.yadavar

import app.yadavar.time.Fmt
import app.yadavar.time.JalaliCalendar
import app.yadavar.time.JalaliDate
import app.yadavar.time.ParseStatus
import app.yadavar.time.PersianTimeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PersianTimeParserTest {
    // سه‌شنبه ۲۰ اکتبر ۲۰۲۶، ساعت ۱۴:۳۰
    private val now = LocalDateTime.of(2026, 10, 20, 14, 30, 15)
    private fun p(s: String) = PersianTimeParser.parse(s, now)
    private fun at(d: Int, h: Int, m: Int, month: Int = 10) = LocalDateTime.of(2026, month, d, h, m)

    @Test fun relativeMinutes() {
        val r = p("پنج دقیقه دیگر به من یادآوری کن چای بخورم")
        assertEquals(at(20, 14, 35), r.suggested); assertEquals(ParseStatus.OK, r.status)
        assertEquals("چای بخورم", r.title)
    }
    @Test fun relativeMinutesExposed() {
        assertEquals(1L, p("یک دقیقه دیگر بهم پیام بده").relativeMinutes)
        assertEquals(null, p("فردا ساعت ده").relativeMinutes)
    }
    @Test fun halfHour() { assertEquals(at(20, 15, 0), p("نیم ساعت دیگر").suggested) }
    @Test fun oneHourWithCommand() {
        val r = p("یک ساعت دیگر بهم یادآوری کن که دارویم را بررسی کنم")
        assertEquals(at(20, 15, 30), r.suggested); assertEquals("دارویم را بررسی کنم", r.title)
    }
    @Test fun twoHoursAndOneAndHalf() {
        assertEquals(at(20, 16, 30), p("دو ساعت دیگر").suggested)
        assertEquals(at(20, 16, 0), p("یک ساعت و نیم دیگر").suggested)
    }
    @Test fun colloquial() { assertEquals(at(20, 15, 30), p("یه ساعت دیگه").suggested) }
    @Test fun tomorrowAfternoon() {
        val r = p("فردا ساعت سه بعدازظهر جلسه دارم")
        assertEquals(at(21, 15, 0), r.suggested); assertEquals(ParseStatus.OK, r.status)
        assertEquals("جلسه دارم", r.title)
        assertEquals(at(21, 15, 0), p("فردا ساعت سه بعد از ظهر جلسه دارم").suggested)
    }
    @Test fun tomorrowAmbiguous() {
        val r = p("فردا ساعت سه")
        assertEquals(ParseStatus.NEEDS_CONFIRMATION, r.status)
        assertEquals(at(21, 15, 0), r.suggested); assertEquals(at(21, 3, 0), r.alternative)
    }
    @Test fun tonight() {
        val r = p("امشب ساعت نه")
        assertEquals(at(20, 21, 0), r.suggested); assertEquals(ParseStatus.OK, r.status)
    }
    @Test fun dayAfterTomorrow() {
        assertEquals(at(22, 10, 0), p("پس‌فردا ساعت ده صبح").suggested)
    }
    @Test fun weekdayWithTime() {
        val r = p("شنبه ساعت ده صبح به من یادآوری کن که با مشتری تماس بگیرم")
        assertEquals(at(24, 10, 0), r.suggested); assertEquals("با مشتری تماس بگیرم", r.title)
    }
    @Test fun sameWeekdayRollsToNextWeek() {
        val r = p("سه‌شنبه ساعت ۹ صبح")
        assertEquals(at(27, 9, 0), r.suggested); assertEquals(ParseStatus.NEEDS_CONFIRMATION, r.status)
    }
    @Test fun tomorrowMorningDefault() {
        val r = p("فردا صبح")
        assertEquals(at(21, 8, 0), r.suggested); assertEquals(ParseStatus.NEEDS_CONFIRMATION, r.status)
    }
    @Test fun persianDigitsAnd24h() {
        val r = p("فردا ساعت ۱۵:۳۰")
        assertEquals(at(21, 15, 30), r.suggested); assertEquals(ParseStatus.OK, r.status)
    }
    @Test fun jalaliDate() {
        assertEquals(at(29, 10, 0), p("۷ آبان ساعت ۱۰ صبح").suggested)
    }
    @Test fun jalaliNumericDate() {
        assertEquals(at(29, 10, 0), p("1405/8/7 ساعت 10 صبح").suggested)
    }
    @Test fun pastTimeIsRejected() {
        assertEquals(ParseStatus.IN_PAST, p("امروز ساعت 10 صبح").status)
    }
    @Test fun timeWithoutDayPicksNextFuture() {
        val r = p("ساعت ۵ یادآوری کن")
        assertEquals(at(20, 17, 0), r.suggested); assertEquals(ParseStatus.NEEDS_CONFIRMATION, r.status)
    }
    @Test fun invalidHour() { assertEquals(ParseStatus.INVALID, p("امروز ساعت 25").status) }
    @Test fun plainNoteHasNoTime() {
        val r = p("جلسه پروژه را یادداشت کن")
        assertEquals(ParseStatus.NO_TIME, r.status); assertEquals("جلسه پروژه", r.title)
        assertNull(r.suggested)
    }
    @Test fun numberWordInTitleIsKept() {
        val r = p("سه کتاب بخرم")
        assertEquals(ParseStatus.NO_TIME, r.status); assertEquals("سه کتاب بخرم", r.title)
    }
    @Test fun midnight() { assertEquals(at(22, 0, 0), p("فردا ساعت 12 شب").suggested) }
    @Test fun quarterToThree() { assertEquals(at(21, 14, 45), p("فردا ساعت 3 کم ربع بعدازظهر").suggested) }
    @Test fun dateOnlyNeedsConfirmation() {
        val r = p("فردا با علی تماس بگیر")
        assertEquals(ParseStatus.NEEDS_CONFIRMATION, r.status); assertEquals(at(21, 9, 0), r.suggested)
        assertEquals("با علی تماس بگیر", r.title)
    }
    @Test fun todayWithoutTimeRoundsToNextHour() {
        val r = p("امروز باید خرید کنم")
        assertEquals(at(20, 15, 0), r.suggested); assertEquals(ParseStatus.NEEDS_CONFIRMATION, r.status)
    }

    // ---- تقویم شمسی ----
    @Test fun jalaliKnownDates() {
        assertEquals(JalaliDate(1403, 1, 1), JalaliCalendar.toJalali(LocalDate.of(2024, 3, 20)))
        assertEquals(JalaliDate(1404, 1, 1), JalaliCalendar.toJalali(LocalDate.of(2025, 3, 21)))
        assertEquals(JalaliDate(1405, 1, 1), JalaliCalendar.toJalali(LocalDate.of(2026, 3, 21)))
        assertEquals(JalaliDate(1405, 7, 28), JalaliCalendar.toJalali(LocalDate.of(2026, 10, 20)))
        assertEquals(LocalDate.of(2025, 3, 20), JalaliCalendar.toGregorian(1403, 12, 30))
    }
    @Test fun jalaliRoundTripAndValidation() {
        var d = LocalDate.of(2020, 1, 1)
        while (d.isBefore(LocalDate.of(2032, 1, 1))) {
            val j = JalaliCalendar.toJalali(d)
            assertEquals(d, JalaliCalendar.toGregorian(j.year, j.month, j.day))
            d = d.plusDays(1)
        }
        assertNull(JalaliCalendar.toGregorianOrNull(1404, 12, 30)) // سال کبیسه نیست
    }
    @Test fun formatting() {
        assertEquals("سه‌شنبه ۲۸ مهر ۱۴۰۵", Fmt.date(LocalDate.of(2026, 10, 20)))
        assertEquals("۱۵:۳۰", Fmt.time(at(20, 15, 30), true))
        assertEquals("۳:۳۰ بعدازظهر", Fmt.time(at(20, 15, 30), false))
        assertEquals("۲ ساعت و ۳۰ دقیقه دیگر", Fmt.remaining(now.withSecond(0), at(20, 17, 0)))
    }
}
