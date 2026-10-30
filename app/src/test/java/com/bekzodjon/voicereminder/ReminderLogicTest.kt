package com.bekzodjon.voicereminder

import com.bekzodjon.voicereminder.domain.Recurrence
import com.bekzodjon.voicereminder.domain.RecurrenceCalculator
import com.bekzodjon.voicereminder.domain.ReminderStateMachine
import com.bekzodjon.voicereminder.domain.ReminderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class ReminderLogicTest {
    @Test fun doneOnlyMovesToPendingConfirmation() {
        assertEquals(ReminderStatus.PENDING_CONFIRMATION, ReminderStateMachine.onDone(ReminderStatus.ACTIVE))
        assertNull(ReminderStateMachine.onDone(ReminderStatus.COMPLETED))
        assertNull(ReminderStateMachine.onDone(ReminderStatus.PENDING_CONFIRMATION))
    }

    @Test fun cannotConfirmWithoutDone() {
        assertNull(ReminderStateMachine.onConfirm(ReminderStatus.ACTIVE))
        assertEquals(ReminderStatus.COMPLETED, ReminderStateMachine.onConfirm(ReminderStatus.PENDING_CONFIRMATION))
    }

    @Test fun notYetReturnsToActive() {
        assertEquals(ReminderStatus.ACTIVE, ReminderStateMachine.onNotYet(ReminderStatus.PENDING_CONFIRMATION))
        assertNull(ReminderStateMachine.onNotYet(ReminderStatus.ACTIVE))
    }

    @Test fun swipeNeverChangesStatus() {
        ReminderStatus.values().forEach { assertEquals(it, ReminderStateMachine.onSwipeDismiss(it)) }
    }

    @Test fun dailyNext() {
        val base = LocalDateTime.of(2026, 10, 1, 7, 0)
        assertEquals(LocalDateTime.of(2026, 10, 31, 7, 0),
            RecurrenceCalculator.nextAfter(base, Recurrence.DAILY, 1, LocalDateTime.of(2026, 10, 30, 8, 0)))
    }

    @Test fun weeklyNext() {
        val base = LocalDateTime.of(2026, 10, 26, 10, 0)
        assertEquals(LocalDateTime.of(2026, 11, 2, 10, 0),
            RecurrenceCalculator.nextAfter(base, Recurrence.WEEKLY, 1, LocalDateTime.of(2026, 10, 30, 8, 0)))
    }

    @Test fun monthlyKeepsDayAfterShortMonth() {
        val base = LocalDateTime.of(2026, 1, 31, 9, 0)
        assertEquals(LocalDateTime.of(2026, 2, 28, 9, 0),
            RecurrenceCalculator.nextAfter(base, Recurrence.MONTHLY, 1, LocalDateTime.of(2026, 2, 1, 0, 0)))
        assertEquals(LocalDateTime.of(2026, 3, 31, 9, 0),
            RecurrenceCalculator.nextAfter(base, Recurrence.MONTHLY, 1, LocalDateTime.of(2026, 3, 1, 0, 0)))
    }

    @Test fun everyTwoDays() {
        val base = LocalDateTime.of(2026, 10, 28, 9, 0)
        assertEquals(LocalDateTime.of(2026, 10, 30, 9, 0),
            RecurrenceCalculator.nextAfter(base, Recurrence.DAILY, 2, LocalDateTime.of(2026, 10, 29, 0, 0)))
    }

    @Test fun nonRecurringHasNoNext() {
        assertNull(RecurrenceCalculator.nextAfter(LocalDateTime.now(), Recurrence.NONE, 1, LocalDateTime.now()))
    }
}
