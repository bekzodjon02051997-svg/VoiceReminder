package com.bekzodjon.voicereminder.parser

import com.bekzodjon.voicereminder.domain.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class ParsedReminder(
    val title: String,
    val dateTime: LocalDateTime?,
    val recurrence: Recurrence = Recurrence.NONE,
    val interval: Int = 1
)

/**
 * Offline rule-based parser for Uzbek (Latin) reminder sentences.
 * Splits one utterance into several reminders and extracts date, time and recurrence.
 * If no date/time is spoken, dateTime stays null (unscheduled) - nothing is invented.
 */
class UzbekReminderParser(private val defaultTime: LocalTime = LocalTime.of(9, 0)) {

    private enum class Period { MORNING, NOON, EVENING, NIGHT }

    private val ic = RegexOption.IGNORE_CASE
    private val sentinel = '\u0001'

    private val numWords = mapOf(
        "bir" to 1, "ikki" to 2, "uch" to 3, "to'rt" to 4, "besh" to 5, "olti" to 6,
        "yetti" to 7, "sakkiz" to 8, "to'qqiz" to 9, "o'n" to 10, "o'n bir" to 11, "o'n ikki" to 12
    )
    private val num = "(\\d{1,2}|o'n\\s+ikki|o'n\\s+bir|o'n|bir|ikki|uch|to'rt|besh|olti|yetti|sakkiz|to'qqiz)"
    private val weekdays = mapOf(
        "dushanba" to DayOfWeek.MONDAY, "seshanba" to DayOfWeek.TUESDAY, "chorshanba" to DayOfWeek.WEDNESDAY,
        "payshanba" to DayOfWeek.THURSDAY, "juma" to DayOfWeek.FRIDAY, "shanba" to DayOfWeek.SATURDAY,
        "yakshanba" to DayOfWeek.SUNDAY
    )
    private val months = mapOf(
        "yanvar" to 1, "fevral" to 2, "mart" to 3, "aprel" to 4, "may" to 5, "iyun" to 6, "iyul" to 7,
        "avgust" to 8, "sentabr" to 9, "sentyabr" to 9, "oktabr" to 10, "oktyabr" to 10, "noyabr" to 11, "dekabr" to 12
    )
    private val wd = "(dushanba|seshanba|chorshanba|payshanba|juma|shanba|yakshanba)"
    private val mn = "(yanvar|fevral|mart|aprel|may|iyun|iyul|avgust|sentabr|sentyabr|oktabr|oktyabr|noyabr|dekabr)"
    private val tail = "(?![a-z'])"

    private val markerRe = Regex("\\b(kerakman|kerakdir|kerak(?:\\s+bo'ladi)?|lozim|eslatib\\s+qo'y|eslating|eslat|esimga\\s+sol)\\b", ic)
    private val relRe = Regex("\\b(yarim|$num)\\s*(daqiqa|minut|soat|kun|hafta)dan(?:\\s+(?:keyin|so'ng))?$tail", ic)
    private val everyNRe = Regex("\\bhar\\s+(\\d+)\\s*(kun|hafta|oy)(?:da|ga|dan|lik)?$tail", ic)
    private val monthDayRe = Regex("\\bhar\\s+oy(?:ning)?\\s+(\\d{1,2})\\s*-?\\s*(?:kuni|sanasi|chi|inchi)?$tail", ic)
    private val everyWdRe = Regex("\\bhar\\s+$wd(?:da|ga|ni|lari|kuni)?(?:\\s+kuni)?$tail", ic)
    private val dailyRe = Regex("\\b(?:har\\s+kun(?:i|da)?|kundalik)$tail", ic)
    private val weeklyRe = Regex("\\b(?:har\\s+hafta(?:da|ning)?|haftalik)$tail", ic)
    private val monthlyRe = Regex("\\b(?:har\\s+oy(?:da)?|oylik)$tail", ic)
    private val fullDateRe = Regex("\\b(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})\\b")
    private val monthDateRe = Regex("\\b(\\d{1,2})\\s*-?\\s*$mn(?:da|dan|ga)?(?:\\s+(\\d{4}))?$tail", ic)
    private val relDayRe = Regex("\\b(bugun|ertaga|indinga|ertadan\\s+keyin)$tail", ic)
    private val wdRe = Regex("\\b(?:(?:kelasi|keyingi)\\s+)?$wd(?:da|dan|ga|lari)?(?:\\s+kuni)?$tail", ic)
    private val soatRe = Regex("\\bsoat\\s+$num(?:\\s*[:.]\\s*([0-5]\\d))?(\\s+yarim)?(?:da|dan|ga|gacha|larda)?$tail(?:\\s+(?:da|dan|ga|gacha)$tail)?", ic)
    private val clockRe = Regex("(?<![\\d.:])([01]?\\d|2[0-3])[:.]([0-5]\\d)(?![\\d.:])(?:\\s*(?:da|dan|ga)$tail)?", ic)
    private val periodRe = Regex("\\b(ertalab(?:ki|da)?|tongda|bamdod|tushdan\\s+keyin|tushda|tushlikda|kunduzi|peshin|kechqurun(?:da)?|kechki|oqshom|kechasi)$tail", ic)
    private val leadJoinRe = Regex("^(?:va|keyin|ham|yana|so'ngra|so'ng|undan\\s+keyin)\\s+", ic)
    private val fillerRe = Regex("\\b(menga|iltimos|mening)\\b", ic)
    private val trailBorRe = Regex("\\s+bor$", ic)
    private val verbalRe = Regex("(?<=sh|uv)(?:imiz|im|ing|ni|i)$", ic)
    private val newTaskHints = listOf(soatRe, clockRe, relDayRe, wdRe, monthDateRe, fullDateRe, Regex("\\bhar\\s+", ic))

    fun normalize(s: String): String = s
        .replace('\u2019', '\'').replace('\u02BB', '\'').replace('\u02BC', '\'')
        .replace('`', '\'').replace('\u00B4', '\'').replace('\u2018', '\'').replace('\u02B9', '\'')
        .replace(Regex("\\s+"), " ").trim()

    fun parse(input: String, now: LocalDateTime): List<ParsedReminder> {
        val text = normalize(input)
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<ParsedReminder>()
        var inheritedDate: LocalDate? = null
        for (task in splitTasks(text)) {
            val (parsed, explicitDate) = parseTask(task, now, inheritedDate)
            if (explicitDate != null) inheritedDate = explicitDate
            if (parsed != null) result += parsed
        }
        return result
    }

    /** Splits one utterance into task chunks (sentences, "kerak/eslat" endings, schedule-bearing commas). */
    fun splitTasks(text: String): List<String> {
        val tasks = mutableListOf<String>()
        for (segment in text.split(Regex("[!?;\\n]+|\\.(?!\\d)"))) {
            val marked = markerRe.replace(segment) { it.value + sentinel }
            for (piece in marked.split(sentinel)) {
                var current: String? = null
                for (raw in piece.split(',')) {
                    val part = raw.trim().replace(leadJoinRe, "").trim()
                    if (part.isEmpty()) continue
                    val cur = current
                    if (cur == null) current = part
                    else if (newTaskHints.any { it.containsMatchIn(part) }) { tasks += cur; current = part }
                    else current = "$cur, $part"
                }
                current?.let { tasks += it }
            }
        }
        return tasks
    }

    private fun wordNum(s: String): Int =
        s.trim().lowercase().replace(Regex("\\s+"), " ").let { it.toIntOrNull() ?: numWords[it] ?: 0 }

    private fun clampDate(y: Int, m: Int, d: Int): LocalDate = YearMonth.of(y, m).let { it.atDay(minOf(d, it.lengthOfMonth())) }

    private fun resolveHour(h: Int, p: Period?): Int {
        if (h == 0 || h >= 13) return h
        return when (p) {
            Period.MORNING -> h
            Period.NOON -> if (h in 1..5) h + 12 else h
            Period.EVENING -> if (h < 12) h + 12 else h
            Period.NIGHT -> if (h == 12) 0 else if (h in 6..11) h + 12 else h
            null -> if (h in 1..6) h + 12 else h
        }
    }

    private fun periodOf(word: String): Period {
        val w = word.lowercase().replace(Regex("\\s+"), " ")
        return when {
            w.startsWith("ertalab") || w == "tongda" || w == "bamdod" -> Period.MORNING
            w.startsWith("tushdan") -> Period.EVENING
            w == "tushda" || w == "tushlikda" || w == "kunduzi" || w == "peshin" -> Period.NOON
            w == "kechasi" -> Period.NIGHT
            else -> Period.EVENING
        }
    }

    private fun parseTask(source: String, now: LocalDateTime, inherited: LocalDate?): Pair<ParsedReminder?, LocalDate?> {
        var t = source
        var recurrence = Recurrence.NONE
        var interval = 1
        var dateV: LocalDate? = null
        var weekdayV: DayOfWeek? = null
        var monthDayV: Int? = null
        var relativeV: LocalDateTime? = null
        var timeV: LocalTime? = null
        var periodV: Period? = null
        val today = now.toLocalDate()

        val rel = relRe.find(t)
        if (rel != null) {
            val isHalf = rel.groupValues[1].equals("yarim", true)
            val amount = if (isHalf) 30 else wordNum(rel.groupValues[1])
            val unit = rel.groupValues[3].lowercase()
            if (amount > 0) {
                val base = now.withSecond(0).withNano(0)
                relativeV = when (unit) {
                    "daqiqa", "minut" -> base.plusMinutes(amount.toLong())
                    "soat" -> if (isHalf) base.plusMinutes(30) else base.plusHours(amount.toLong())
                    "kun" -> base.plusDays(amount.toLong())
                    else -> base.plusWeeks(amount.toLong())
                }
                t = t.replaceRange(rel.range, " ")
            }
        }
        val everyN = everyNRe.find(t)
        if (everyN != null) {
            interval = maxOf(1, everyN.groupValues[1].toInt())
            recurrence = when (everyN.groupValues[2].lowercase()) { "kun" -> Recurrence.DAILY; "hafta" -> Recurrence.WEEKLY; else -> Recurrence.MONTHLY }
            t = t.replaceRange(everyN.range, " ")
        }
        if (recurrence == Recurrence.NONE) {
            val m = monthDayRe.find(t)
            if (m != null) { recurrence = Recurrence.MONTHLY; monthDayV = m.groupValues[1].toInt().coerceIn(1, 31); t = t.replaceRange(m.range, " ") }
        }
        if (recurrence == Recurrence.NONE) {
            val m = everyWdRe.find(t)
            if (m != null) { recurrence = Recurrence.WEEKLY; weekdayV = weekdays[m.groupValues[1].lowercase()]; t = t.replaceRange(m.range, " ") }
        }
        if (recurrence == Recurrence.NONE) {
            val m = dailyRe.find(t)
            if (m != null) { recurrence = Recurrence.DAILY; t = t.replaceRange(m.range, " ") }
        }
        if (recurrence == Recurrence.NONE) {
            val m = weeklyRe.find(t)
            if (m != null) { recurrence = Recurrence.WEEKLY; t = t.replaceRange(m.range, " ") }
        }
        if (recurrence == Recurrence.NONE) {
            val m = monthlyRe.find(t)
            if (m != null) { recurrence = Recurrence.MONTHLY; t = t.replaceRange(m.range, " ") }
        }

        val full = fullDateRe.find(t)
        if (full != null) {
            val y = full.groupValues[3].toInt(); val mo = full.groupValues[2].toInt(); val d = full.groupValues[1].toInt()
            if (mo in 1..12 && d in 1..31) { dateV = clampDate(y, mo, d); t = t.replaceRange(full.range, " ") }
        }
        if (dateV == null) {
            val m = monthDateRe.find(t)
            if (m != null) {
                val d = m.groupValues[1].toInt(); val mo = months[m.groupValues[2].lowercase()] ?: 1
                if (d in 1..31) {
                    val y = m.groupValues[3].toIntOrNull()
                    var c = clampDate(y ?: today.year, mo, d)
                    if (y == null && c.isBefore(today)) c = clampDate(today.year + 1, mo, d)
                    dateV = c; t = t.replaceRange(m.range, " ")
                }
            }
        }
        if (dateV == null) {
            val m = relDayRe.find(t)
            if (m != null) {
                dateV = when (m.groupValues[1].lowercase().replace(Regex("\\s+"), " ")) {
                    "bugun" -> today; "ertaga" -> today.plusDays(1); else -> today.plusDays(2)
                }
                t = t.replaceRange(m.range, " ")
            }
        }
        if (dateV == null && weekdayV == null && recurrence == Recurrence.NONE) {
            val m = wdRe.find(t)
            if (m != null) { weekdayV = weekdays[m.groupValues[1].lowercase()]; t = t.replaceRange(m.range, " ") }
        }

        val per = periodRe.find(t)
        if (per != null) { periodV = periodOf(per.groupValues[1]); t = t.replaceRange(per.range, " ") }

        val soat = soatRe.find(t)
        if (soat != null) {
            val h = wordNum(soat.groupValues[1])
            var min = soat.groupValues[2].toIntOrNull() ?: 0
            if (soat.groupValues[3].isNotBlank()) min = 30
            if (h in 0..23) { timeV = LocalTime.of(resolveHour(h, periodV), min); t = t.replaceRange(soat.range, " ") }
        }
        if (timeV == null) {
            val m = clockRe.find(t)
            if (m != null) {
                timeV = LocalTime.of(resolveHour(m.groupValues[1].toInt(), periodV), m.groupValues[2].toInt())
                t = t.replaceRange(m.range, " ")
            }
        }

        val date = dateV; val weekday = weekdayV; val monthDay = monthDayV; val relative = relativeV; val time = timeV

        val explicitDate: LocalDate? = date ?: if (recurrence == Recurrence.NONE && weekday != null) {
            val cand = today.with(TemporalAdjusters.nextOrSame(weekday))
            if (time != null && LocalDateTime.of(cand, time).isBefore(now)) cand.plusWeeks(1) else cand
        } else null

        val title = cleanTitle(t)
        val hasSchedule = relative != null || date != null || weekday != null || monthDay != null || time != null || recurrence != Recurrence.NONE
        if (title.isEmpty() && !hasSchedule) return Pair(null, null)
        if (title.isEmpty()) return Pair(null, explicitDate)

        val dateTime: LocalDateTime? = when {
            relative != null -> relative
            !hasSchedule -> null
            else -> {
                val tm = time ?: defaultTime
                val d: LocalDate = when {
                    date != null -> date
                    monthDay != null -> {
                        var c = clampDate(today.year, today.monthValue, monthDay)
                        if (!LocalDateTime.of(c, tm).isAfter(now)) { val nm = today.plusMonths(1); c = clampDate(nm.year, nm.monthValue, monthDay) }
                        c
                    }
                    weekday != null -> {
                        var c = today.with(TemporalAdjusters.nextOrSame(weekday))
                        if (!LocalDateTime.of(c, tm).isAfter(now)) c = c.plusWeeks(1)
                        c
                    }
                    inherited != null && time != null && recurrence == Recurrence.NONE -> inherited
                    else -> if (LocalDateTime.of(today, tm).isAfter(now)) today else today.plusDays(1)
                }
                LocalDateTime.of(d, tm)
            }
        }
        return Pair(ParsedReminder(title, dateTime, recurrence, interval), explicitDate)
    }

    private fun cleanTitle(raw: String): String {
        var s = markerRe.replace(raw, " ")
        s = fillerRe.replace(s, " ")
        s = s.replace(Regex("\\s+"), " ").trim().trim(',', ':', ';', '-', ' ')
        s = trailBorRe.replace(s, "").trim().trim(',', ':', ';', '-', ' ')
        s = leadJoinRe.replace(s, "")
        s = verbalRe.replace(s, "")
        s = s.replace(Regex("\\s+,"), ",").replace(Regex("\\s+"), " ").trim()
        return s.replaceFirstChar { it.uppercaseChar() }
    }
}
