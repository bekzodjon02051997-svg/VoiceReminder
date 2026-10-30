package com.bekzodjon.voicereminder.domain

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Pure state machine for the two-stage completion flow (DONE -> CONFIRM DONE). */
object ReminderStateMachine {
    /** Stage 1: DONE only asks for confirmation, it never completes. */
    fun onDone(s: ReminderStatus): ReminderStatus? =
        if (s == ReminderStatus.ACTIVE) ReminderStatus.PENDING_CONFIRMATION else null

    /** Stage 2: CONFIRM DONE is the only way to complete. */
    fun onConfirm(s: ReminderStatus): ReminderStatus? =
        if (s == ReminderStatus.PENDING_CONFIRMATION) ReminderStatus.COMPLETED else null

    fun onNotYet(s: ReminderStatus): ReminderStatus? =
        if (s == ReminderStatus.PENDING_CONFIRMATION) ReminderStatus.ACTIVE else null

    /** Swiping a notification away never changes the status. */
    fun onSwipeDismiss(s: ReminderStatus): ReminderStatus = s
}

object RecurrenceCalculator {
    /** First occurrence strictly after [after], stepping from [base] by [interval] units. Null if not recurring. */
    fun nextAfter(base: LocalDateTime, recurrence: Recurrence, interval: Int, after: LocalDateTime): LocalDateTime? {
        if (recurrence == Recurrence.NONE) return null
        val step = if (interval < 1) 1L else interval.toLong()
        fun at(n: Long): LocalDateTime = when (recurrence) {
            Recurrence.DAILY -> base.plusDays(n * step)
            Recurrence.WEEKLY -> base.plusWeeks(n * step)
            else -> base.plusMonths(n * step)
        }
        val unit = when (recurrence) {
            Recurrence.DAILY -> ChronoUnit.DAYS
            Recurrence.WEEKLY -> ChronoUnit.WEEKS
            else -> ChronoUnit.MONTHS
        }
        var n = maxOf(1L, unit.between(base, after) / step - 1)
        if (at(n) <= after) {
            while (at(n) <= after) n++
        } else {
            while (n > 1 && at(n - 1) > after) n--
        }
        return at(n)
    }
}
