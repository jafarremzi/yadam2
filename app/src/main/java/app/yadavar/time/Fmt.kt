package app.yadavar.time

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** قالب‌بندی تاریخ و ساعت برای نمایش فارسی. */
object Fmt {
    val MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    fun digits(s: String): String = buildString {
        for (c in s) append(if (c in '0'..'9') ('\u06F0' + (c - '0')) else c)
    }

    fun weekday(d: DayOfWeek): String = when (d) {
        DayOfWeek.SATURDAY -> "شنبه"
        DayOfWeek.SUNDAY -> "یکشنبه"
        DayOfWeek.MONDAY -> "دوشنبه"
        DayOfWeek.TUESDAY -> "سه‌شنبه"
        DayOfWeek.WEDNESDAY -> "چهارشنبه"
        DayOfWeek.THURSDAY -> "پنجشنبه"
        DayOfWeek.FRIDAY -> "جمعه"
    }

    fun date(d: LocalDate): String {
        val j = JalaliCalendar.toJalali(d)
        return digits("${weekday(d.dayOfWeek)} ${j.day} ${MONTHS[j.month - 1]} ${j.year}")
    }

    fun time(dt: LocalDateTime, use24h: Boolean): String {
        val h = dt.hour
        val mm = dt.minute.toString().padStart(2, '0')
        if (use24h) return digits("${h.toString().padStart(2, '0')}:$mm")
        val h12 = if (h % 12 == 0) 12 else h % 12
        val part = when {
            h < 12 -> "صبح"
            h < 17 -> "بعدازظهر"
            h < 20 -> "عصر"
            else -> "شب"
        }
        return digits("$h12:$mm") + " " + part
    }

    fun dateTime(dt: LocalDateTime, use24h: Boolean): String =
        date(dt.toLocalDate()) + "، " + time(dt, use24h)

    fun timer(seconds: Int): String = digits("${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}")

    fun remaining(from: LocalDateTime, to: LocalDateTime): String {
        val total = Duration.between(from, to).toMinutes()
        if (total < 0) return "گذشته"
        if (total < 1) return "کمتر از یک دقیقه دیگر"
        val days = total / 1440
        val hours = (total % 1440) / 60
        val mins = total % 60
        val parts = ArrayList<String>()
        if (days > 0) parts.add("$days روز")
        if (hours > 0) parts.add("$hours ساعت")
        if (mins > 0) parts.add("$mins دقیقه")
        return digits(parts.joinToString(" و ")) + " دیگر"
    }
}
