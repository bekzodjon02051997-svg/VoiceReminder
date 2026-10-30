package com.bekzodjon.voicereminder.ui

import com.bekzodjon.voicereminder.data.ReminderEntity
import com.bekzodjon.voicereminder.data.scheduledLocal
import com.bekzodjon.voicereminder.data.statusEnum
import com.bekzodjon.voicereminder.domain.ReminderStatus
import java.time.LocalDate

object ReminderFilters {
    private fun open(l: List<ReminderEntity>) = l.filter { it.statusEnum != ReminderStatus.COMPLETED }

    /** Today's reminders plus anything overdue that is still open. */
    fun today(l: List<ReminderEntity>, today: LocalDate) =
        open(l).filter { r -> r.scheduledLocal?.toLocalDate()?.let { !it.isAfter(today) } == true }

    fun upcoming(l: List<ReminderEntity>, today: LocalDate) =
        open(l).filter { r -> r.scheduledLocal?.toLocalDate()?.isAfter(today) == true }

    fun unscheduled(l: List<ReminderEntity>) = open(l).filter { it.scheduledAt == null }

    fun completed(l: List<ReminderEntity>) =
        l.filter { it.statusEnum == ReminderStatus.COMPLETED }.sortedByDescending { it.completedAt ?: 0L }

    fun search(l: List<ReminderEntity>, q: String): List<ReminderEntity> =
        if (q.isBlank()) l else l.filter {
            it.title.contains(q, ignoreCase = true) || (it.description?.contains(q, ignoreCase = true) == true)
        }
}
