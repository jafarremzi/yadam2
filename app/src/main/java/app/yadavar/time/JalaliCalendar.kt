package app.yadavar.time

import java.time.LocalDate

data class JalaliDate(val year: Int, val month: Int, val day: Int)

/** تبدیل تاریخ میلادی و شمسی (الگوریتم jalaali-js). */
object JalaliCalendar {
    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )
    private const val JDN_EPOCH_OFFSET = 2440588L

    private class Cal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): Cal {
        val bl = BREAKS.size
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        require(jy >= jp && jy < BREAKS[bl - 1]) { "سال شمسی خارج از محدوده" }
        var jump = 0
        for (i in 1 until bl) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + (jump % 33) / 4
            jp = jm
        }
        var n = jy - jp
        leapJ += (n / 33) * 8 + ((n % 33) + 3) / 4
        if ((jump % 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = (gy / 4) - (((gy / 100) + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return Cal(leap, gy, march)
    }

    fun toGregorian(jy: Int, jm: Int, jd: Int): LocalDate {
        val r = jalCal(jy)
        val jdn1f = LocalDate.of(r.gy, 3, r.march).toEpochDay() + JDN_EPOCH_OFFSET
        val jdn = jdn1f + (jm - 1) * 31 - (jm / 7) * (jm - 7) + jd - 1
        return LocalDate.ofEpochDay(jdn - JDN_EPOCH_OFFSET)
    }

    fun toJalali(date: LocalDate): JalaliDate {
        val jdn = date.toEpochDay() + JDN_EPOCH_OFFSET
        val gy = date.year
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = LocalDate.of(gy, 3, r.march).toEpochDay() + JDN_EPOCH_OFFSET
        var k = (jdn - jdn1f).toInt()
        if (k >= 0) {
            if (k <= 185) {
                return JalaliDate(jy, 1 + k / 31, k % 31 + 1)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        return JalaliDate(jy, 7 + k / 30, k % 30 + 1)
    }

    /** null اگر تاریخ شمسی معتبر نباشد (مثلاً ۳۰ اسفند در سال کبیسه‌نشده). */
    fun toGregorianOrNull(jy: Int, jm: Int, jd: Int): LocalDate? {
        if (jm !in 1..12 || jd !in 1..31) return null
        return try {
            val g = toGregorian(jy, jm, jd)
            if (toJalali(g) == JalaliDate(jy, jm, jd)) g else null
        } catch (e: Exception) {
            null
        }
    }
}
