package app.yadavar.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class ParseStatus { NO_TIME, OK, NEEDS_CONFIRMATION, IN_PAST, INVALID }

data class ParseResult(
    /** عنوان کار پس از حذف عبارت‌های زمانی و فرمانی (ارقام لاتین؛ نمایش با Fmt.digits). */
    val title: String,
    val status: ParseStatus,
    val suggested: LocalDateTime?,
    /** گزینه جایگزین وقتی صبح/بعدازظهر مشخص نیست. */
    val alternative: LocalDateTime?,
    val message: String?,
    /** آیا بهتر است پیش‌فرض، یادآوری زمان‌دار باشد؟ */
    val suggestReminder: Boolean,
    /** اگر زمان نسبی بوده («۵ دقیقه دیگر»): مدت بر حسب دقیقه؛ برای محاسبه‌ی دوباره هنگام تأیید. */
    val relativeMinutes: Long? = null,
)

object TextNormalizer {
    private val SPACES = Regex("""\s+""")
    private val PUNCT = Regex("""[،؛؟?!.,"«»()\[\]]""")

    private val REPLACEMENTS = listOf(
        Regex("""بعد ?از ?ظهر""") to "بعدازظهر",
        Regex("""بعدظهر""") to "بعدازظهر",
        Regex("""پس ?فردا""") to "پسفردا",
        Regex("""(?<![\p{L}\p{N}])دیگه(?![\p{L}\p{N}])""") to "دیگر",
        Regex("""(?<![\p{L}\p{N}])یه(?![\p{L}\p{N}])""") to "یک",
        Regex("""(?<![\p{L}\p{N}])ابان(?![\p{L}\p{N}])""") to "آبان",
        Regex("""(?<![\p{L}\p{N}])اذر(?![\p{L}\p{N}])""") to "آذر",
    )

    fun normalize(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            when (ch) {
                in '\u06F0'..'\u06F9' -> sb.append('0' + (ch - '\u06F0'))
                in '\u0660'..'\u0669' -> sb.append('0' + (ch - '\u0660'))
                '\u064A', '\u0649' -> sb.append('\u06CC')
                '\u0643' -> sb.append('\u06A9')
                '\u200C', '\u200F', '\u200E', '\u00A0' -> sb.append(' ')
                '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650', '\u0651', '\u0652', '\u0640' -> {}
                else -> sb.append(ch)
            }
        }
        var t = PUNCT.replace(sb.toString(), " ")
        t = SPACES.replace(t, " ").trim()
        for ((re, to) in REPLACEMENTS) t = re.replace(t, to)
        return t
    }
}

/**
 * استخراج تاریخ و ساعت از متن فارسی. ماژول مستقل از اندروید است تا با تست واحد بررسی شود.
 * اصل: هرگز زمان مبهم را بی‌صدا حدس نمی‌زند؛ در این حالت وضعیت NEEDS_CONFIRMATION برمی‌گردد.
 */
object PersianTimeParser {

    private enum class Period { MORNING, NOON, AFTERNOON, EVENING, NIGHT }
    private enum class DayWord { TODAY, TOMORROW, DAY_AFTER, TONIGHT }

    private const val B = """(?<![\p{L}\p{N}])"""
    private const val E = """(?![\p{L}\p{N}])"""

    private val MONTHS = Fmt.MONTHS
    private val MONTH_ALT = MONTHS.joinToString("|")

    private val UNITS = mapOf(
        "صفر" to 0, "یک" to 1, "دو" to 2, "سه" to 3, "چهار" to 4, "پنج" to 5,
        "شش" to 6, "شیش" to 6, "هفت" to 7, "هشت" to 8, "نه" to 9, "ده" to 10,
        "یازده" to 11, "دوازده" to 12, "سیزده" to 13, "چهارده" to 14,
        "پانزده" to 15, "پونزده" to 15, "شانزده" to 16, "شونزده" to 16,
        "هفده" to 17, "هیجده" to 18, "هجده" to 18, "نوزده" to 19,
    )
    private val TENS = mapOf("بیست" to 20, "سی" to 30, "چهل" to 40, "پنجاه" to 50, "شصت" to 60)
    private val NUMBER_CONTEXT_NEXT = setOf(
        "ساعت", "دقیقه", "روز", "هفته", "ربع", "صبح", "ظهر", "بعدازظهر", "عصر", "شب", "ماه",
    ) + MONTHS.toSet()

    private fun w(p: String) = Regex("$B(?:$p)$E")
    private fun wd(p: String, d: DayOfWeek) = Regex("""(?:${B}این\s+)?$B(?:$p)$E(?:\s+(?:آینده|بعدی)$E)?""") to d

    private val WEEKDAYS = listOf(
        wd("یک ?شنبه", DayOfWeek.SUNDAY), wd("دو ?شنبه", DayOfWeek.MONDAY),
        wd("سه ?شنبه", DayOfWeek.TUESDAY), wd("چهار ?شنبه", DayOfWeek.WEDNESDAY),
        wd("پنج ?شنبه", DayOfWeek.THURSDAY), wd("جمعه", DayOfWeek.FRIDAY),
        wd("شنبه", DayOfWeek.SATURDAY),
    )
    private val DAY_WORDS = listOf(
        w("امشب") to DayWord.TONIGHT, w("پسفردا") to DayWord.DAY_AFTER,
        w("فردا") to DayWord.TOMORROW, w("امروز") to DayWord.TODAY,
    )
    private val PERIODS = listOf(
        w("بعدازظهر") to Period.AFTERNOON, w("عصر") to Period.EVENING,
        w("ظهر") to Period.NOON, w("صبح") to Period.MORNING,
        w("نیمه ?شب|شب") to Period.NIGHT,
    )

    private val REMIND_INTENT = Regex("""یاد ?آوری|یادم (?:بنداز|باشه|باشد|بیار)|خبرم کن""")
    private val NOTE_INTENT = Regex("""یادداشت""")
    private val REL = Regex(
        """(?:(?:\d{1,5}\s*(?:ساعت|دقیقه|روز|هفته)(?:\s*و\s*نیم)?|نیم\s*ساعت|(?:\d{1,5}\s*)?ربع(?:\s*ساعت)?)(?:\s*و\s*)?)+\s*(?:دیگر|بعد)$E""",
    )
    private val REL_HALF = Regex("""(\d{1,5})\s*(ساعت|روز|هفته)\s*و\s*نیم""")
    private val REL_HALF_HOUR = Regex("""نیم\s*ساعت""")
    private val REL_QUARTER = Regex("""(?:(\d{1,5})\s*)?ربع(?:\s*ساعت)?""")
    private val REL_UNIT = Regex("""(\d{1,5})\s*(ساعت|دقیقه|روز|هفته)""")

    private val NUM_DATE = Regex("""(?<![\d/])(\d{4})/(\d{1,2})/(\d{1,2})(?![\d/])""")
    private val NAMED_DATE = Regex("""(?<![\d/:])(\d{1,2})\s*(?:ام\s*)?($MONTH_ALT)(?:\s*ماه)?(?:\s*(\d{4}))?$E""")
    private val CLOCK = Regex("""${B}ساعت\s*(\d{1,2})(?::(\d{2}))?(?:\s*و\s*(نیم|ربع|\d{1,2}\s*دقیقه))?(\s*کم\s*ربع)?""")
    private val BARE_CLOCK = Regex("""(?<![\d:/])(\d{1,2}):(\d{2})(?![\d:])""")
    private val NUM_PERIOD = Regex("""(?<![\p{L}\p{N}:/])(\d{1,2})\s*PERIOD""")

    private val COMMANDS = Regex(
        """(?:به ?من|بهم|برام|برایم)?\s*(?:یاد ?آوری\s*(?:کن|کنید|بده|بکن)|یادم\s*(?:بنداز|باشه|باشد|بیار)|یادداشت\s*(?:کن|کنید|بکن)|خبرم\s*کن)(?:\s*که)?""",
    )
    private val SPACES = Regex("""\s+""")

    private fun String.cut(m: MatchResult): String =
        substring(0, m.range.first) + " " + substring(m.range.last + 1)

    private fun unitMinutes(u: String): Long = when (u) {
        "ساعت" -> 60L
        "دقیقه" -> 1L
        "روز" -> 1440L
        else -> 10080L
    }

    private fun convertNumberWords(text: String): String {
        val tk = text.split(' ')
        val out = ArrayList<String>()
        var i = 0
        while (i < tk.size) {
            var value = -1
            var len = 0
            val t = tk[i]
            val tens = TENS[t]
            if (tens != null) {
                val unit = if (tk.getOrNull(i + 1) == "و") UNITS[tk.getOrNull(i + 2) ?: ""] else null
                if (unit != null && unit in 1..9) { value = tens + unit; len = 3 } else { value = tens; len = 1 }
            } else {
                val u = UNITS[t]
                if (u != null) { value = u; len = 1 }
            }
            if (len > 0) {
                val next = tk.getOrNull(i + len)
                val prev = out.lastOrNull()
                if ((next != null && next in NUMBER_CONTEXT_NEXT) || prev == "ساعت") {
                    out.add(value.toString())
                    i += len
                    continue
                }
            }
            out.add(t)
            i++
        }
        return out.joinToString(" ")
    }

    private fun cleanTitle(s: String, fallback: String): String {
        var t = " " + s.replace("PERIOD", " ") + " "
        t = COMMANDS.replace(t, " ")
        t = SPACES.replace(t, " ").trim()
        var changed = true
        while (changed) {
            changed = false
            for (lead in listOf("که ", "و ")) if (t.startsWith(lead)) { t = t.removePrefix(lead).trim(); changed = true }
            if (t.endsWith(" را")) { t = t.removeSuffix(" را").trim(); changed = true }
        }
        return if (t.isBlank()) fallback else t
    }

    fun parse(raw: String, now: LocalDateTime): ParseResult {
        val nowMin = now.withSecond(0).withNano(0)
        val today = nowMin.toLocalDate()
        val original = TextNormalizer.normalize(raw)
        var work = original
        val noteIntent = NOTE_INTENT.containsMatchIn(work)
        val reminderIntent = REMIND_INTENT.containsMatchIn(work)

        fun invalid(msg: String) = ParseResult(cleanTitle(work, original), ParseStatus.INVALID, null, null, msg, !noteIntent)

        var weekday: DayOfWeek? = null
        for ((re, dow) in WEEKDAYS) {
            val m = re.find(work) ?: continue
            weekday = dow
            work = work.cut(m)
            break
        }
        work = convertNumberWords(work)

        // ---- زمان نسبی: «پنج دقیقه دیگر» ----
        val rel = REL.find(work)
        if (rel != null) {
            var minutes = 0L
            var s = rel.value
            s = REL_HALF.replace(s) { mr ->
                val u = unitMinutes(mr.groupValues[2])
                minutes += mr.groupValues[1].toLong() * u + u / 2
                " "
            }
            s = REL_HALF_HOUR.replace(s) { minutes += 30; " " }
            s = REL_QUARTER.replace(s) { mr ->
                minutes += 15L * (mr.groupValues[1].toLongOrNull() ?: 1L)
                " "
            }
            REL_UNIT.findAll(s).forEach { mr -> minutes += mr.groupValues[1].toLong() * unitMinutes(mr.groupValues[2]) }
            work = work.cut(rel)
            if (minutes <= 0L || minutes > 366L * 1440L) return invalid("مدت زمان نامعتبر است.")
            return ParseResult(
                cleanTitle(work, original), ParseStatus.OK, nowMin.plusMinutes(minutes), null, null, !noteIntent,
                relativeMinutes = minutes,
            )
        }

        // ---- تاریخ مشخص ----
        var date: LocalDate? = null
        val nd = NUM_DATE.find(work)
        if (nd != null) {
            val y = nd.groupValues[1].toInt()
            val mo = nd.groupValues[2].toInt()
            val d = nd.groupValues[3].toInt()
            date = if (y < 1700) JalaliCalendar.toGregorianOrNull(y, mo, d)
            else runCatching { LocalDate.of(y, mo, d) }.getOrNull()
            if (date == null) return invalid("تاریخ نامعتبر است.")
            work = work.cut(nd)
        } else {
            val nm = NAMED_DATE.find(work)
            if (nm != null) {
                val d = nm.groupValues[1].toInt()
                val mo = MONTHS.indexOf(nm.groupValues[2]) + 1
                val yStr = nm.groupValues[3]
                if (yStr.isNotEmpty()) {
                    date = JalaliCalendar.toGregorianOrNull(yStr.toInt(), mo, d)
                } else {
                    val cj = JalaliCalendar.toJalali(today).year
                    var cand = JalaliCalendar.toGregorianOrNull(cj, mo, d)
                    if (cand != null && cand.isBefore(today)) cand = JalaliCalendar.toGregorianOrNull(cj + 1, mo, d)
                    date = cand
                }
                if (date == null) return invalid("تاریخ نامعتبر است.")
                work = work.cut(nm)
            }
        }

        // ---- امروز/فردا/پس‌فردا/امشب ----
        var dayWord: DayWord? = null
        for ((re, dw) in DAY_WORDS) {
            val m = re.find(work) ?: continue
            dayWord = dw
            work = work.cut(m)
            break
        }

        // ---- بخش روز ----
        var period: Period? = null
        for ((re, p) in PERIODS) {
            val m = re.find(work) ?: continue
            period = p
            work = work.substring(0, m.range.first) + " PERIOD " + work.substring(m.range.last + 1)
            break
        }
        if (dayWord == DayWord.TONIGHT) {
            if (period == null) period = Period.NIGHT
            dayWord = DayWord.TODAY
        }

        // ---- ساعت ----
        var hour: Int? = null
        var minute = 0
        var kam = false
        val c1 = CLOCK.find(work)
        if (c1 != null) {
            hour = c1.groupValues[1].toInt()
            if (c1.groupValues[2].isNotEmpty()) minute = c1.groupValues[2].toInt()
            when (val extra = c1.groupValues[3]) {
                "" -> {}
                "نیم" -> minute = 30
                "ربع" -> minute = 15
                else -> minute = extra.filter { it.isDigit() }.toInt()
            }
            kam = c1.groupValues[4].isNotEmpty()
            work = work.cut(c1)
        } else {
            val c2 = BARE_CLOCK.find(work)
            if (c2 != null) {
                hour = c2.groupValues[1].toInt()
                minute = c2.groupValues[2].toInt()
                work = work.cut(c2)
            } else {
                val c3 = NUM_PERIOD.find(work)
                if (c3 != null) {
                    hour = c3.groupValues[1].toInt()
                    work = work.substring(0, c3.range.first) + " PERIOD " + work.substring(c3.range.last + 1)
                }
            }
        }
        if (hour != null && (hour > 23 || minute > 59)) return invalid("ساعت نامعتبر است.")

        val per = period
        val explicitTime = hour != null || per != null
        val hasDate = date != null || dayWord != null || weekday != null
        if (!explicitTime && !hasDate) {
            return ParseResult(
                cleanTitle(work, original), ParseStatus.NO_TIME, null, null,
                if (reminderIntent) "زمان یادآوری مشخص نشد؛ لطفاً تنظیم کنید." else null,
                reminderIntent && !noteIntent,
            )
        }

        val suggestReminder = !noteIntent && (hour != null || per != null || reminderIntent)
        val title = cleanTitle(work, original)
        val explicitDay = date != null || dayWord != null
        val weekdayOnly = !explicitDay && weekday != null
        val dayless = !explicitDay && weekday == null
        var baseDate: LocalDate? = date ?: when (dayWord) {
            DayWord.TODAY -> today
            DayWord.TOMORROW -> today.plusDays(1)
            DayWord.DAY_AFTER -> today.plusDays(2)
            else -> null
        }
        if (baseDate == null && weekday != null) {
            baseDate = today.plusDays(((weekday.value - today.dayOfWeek.value + 7) % 7).toLong())
        }
        var d0: LocalDate = baseDate ?: today

        if (hour == null) {
            val defH = when (per) {
                Period.MORNING -> 8
                Period.NOON -> 12
                Period.AFTERNOON -> 15
                Period.EVENING -> 17
                Period.NIGHT -> 21
                null -> 9
            }
            var dt = LocalDateTime.of(d0, LocalTime.of(defH, 0))
            if (!dt.isAfter(nowMin)) {
                when {
                    dayless -> dt = dt.plusDays(1)
                    weekdayOnly -> dt = dt.plusDays(7)
                    d0 == today && per == null -> dt = nowMin.withMinute(0).plusHours(1)
                }
            }
            return if (dt.isAfter(nowMin)) {
                ParseResult(title, ParseStatus.NEEDS_CONFIRMATION, dt, null,
                    "ساعت دقیق مشخص نشد؛ زمان پیشنهادی را بررسی کنید.", suggestReminder)
            } else {
                ParseResult(title, ParseStatus.IN_PAST, dt, null, "این زمان گذشته است.", suggestReminder)
            }
        }

        val definite = hour == 0 || hour > 12
        var h: Int = hour
        val m = if (kam) 45 else minute
        if (kam) h = if (h == 0) 23 else if (h == 1 && !definite) 12 else h - 1

        val options = ArrayList<Pair<Int, Int>>() // (ساعت، جابه‌جایی روز)
        var ambiguous = false
        if (definite) {
            options.add(h to 0)
        } else if (per != null) {
            options.add(
                when (per) {
                    Period.MORNING -> (if (h == 12) 0 else h) to 0
                    Period.NOON -> (if (h == 12) 12 else if (h <= 5) h + 12 else h) to 0
                    Period.AFTERNOON, Period.EVENING -> (if (h == 12) 12 else h + 12) to 0
                    Period.NIGHT -> if (h == 12) 0 to 1 else if (h <= 4) h to 1 else (h + 12) to 0
                },
            )
        } else {
            ambiguous = true
            if (h == 12) { options.add(12 to 0); options.add(0 to 1) }
            else { options.add(h to 0); options.add((h + 12) to 0) }
        }

        fun build(d: LocalDate) = options.map { LocalDateTime.of(d.plusDays(it.second.toLong()), LocalTime.of(it.first, m)) }
        var dts = build(d0)
        var rolled = false
        if ((dayless || weekdayOnly) && dts.none { it.isAfter(nowMin) }) {
            d0 = d0.plusDays(if (weekdayOnly) 7L else 1L)
            dts = build(d0)
            rolled = true
        }

        var suggested: LocalDateTime
        var alternative: LocalDateTime? = null
        if (!ambiguous) {
            suggested = dts[0]
        } else if (d0 == today) {
            val fut = dts.filter { it.isAfter(nowMin) }
            if (fut.isEmpty()) {
                suggested = dts[0]
            } else {
                suggested = fut[0]
                alternative = fut.getOrNull(1)
            }
        } else if (h == 12 || h !in 1..6) {
            suggested = dts[0]; alternative = dts[1]
        } else {
            suggested = dts[1]; alternative = dts[0]
        }

        return when {
            !suggested.isAfter(nowMin) ->
                ParseResult(title, ParseStatus.IN_PAST, suggested, null, "این زمان گذشته است.", suggestReminder)
            ambiguous ->
                ParseResult(title, ParseStatus.NEEDS_CONFIRMATION, suggested, alternative,
                    "مشخص نیست صبح است یا بعدازظهر؛ لطفاً زمان را تأیید کنید.", suggestReminder)
            rolled ->
                ParseResult(title, ParseStatus.NEEDS_CONFIRMATION, suggested, null,
                    if (weekdayOnly) "همین روز هفته گذشته است؛ هفته بعد پیشنهاد شد."
                    else "این ساعت امروز گذشته است؛ فردا پیشنهاد شد.", suggestReminder)
            else -> ParseResult(title, ParseStatus.OK, suggested, null, null, suggestReminder)
        }
    }
}
