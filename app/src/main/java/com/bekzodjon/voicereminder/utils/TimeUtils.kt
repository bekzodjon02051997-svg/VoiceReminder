package com.bekzodjon.voicereminder.utils

import android.content.Context
import com.bekzodjon.voicereminder.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object TimeUtils {
    private val monthNames = listOf(
        "yanvar", "fevral", "mart", "aprel", "may", "iyun",
        "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr"
    )

    fun toMillis(dt: LocalDateTime): Long = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun toLocal(ms: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())

    fun formatDate(context: Context, d: LocalDate): String {
        val today = LocalDate.now()
        return when (d) {
            today -> context.getString(R.string.today)
            today.plusDays(1) -> context.getString(R.string.tomorrow)
            else -> {
                val base = "${d.dayOfMonth}-${monthNames[d.monthValue - 1]}"
                if (d.year != today.year) "$base ${d.year}" else base
            }
        }
    }

    fun formatSchedule(context: Context, ms: Long?): String {
        if (ms == null) return context.getString(R.string.unscheduled)
        val dt = toLocal(ms)
        return "${formatDate(context, dt.toLocalDate())}, %02d:%02d".format(dt.hour, dt.minute)
    }

    fun longDate(d: LocalDate): String = "${d.dayOfMonth}-${monthNames[d.monthValue - 1]}, ${d.year}"
}
