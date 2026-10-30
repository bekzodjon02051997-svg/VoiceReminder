package com.bekzodjon.voicereminder.repository

import android.content.Context
import com.bekzodjon.voicereminder.data.Prefs
import com.bekzodjon.voicereminder.data.ReminderEntity
import com.bekzodjon.voicereminder.data.recurrenceEnum
import com.bekzodjon.voicereminder.data.statusEnum
import com.bekzodjon.voicereminder.database.ReminderDao
import com.bekzodjon.voicereminder.domain.Recurrence
import com.bekzodjon.voicereminder.domain.RecurrenceCalculator
import com.bekzodjon.voicereminder.domain.ReminderSource
import com.bekzodjon.voicereminder.domain.ReminderStateMachine
import com.bekzodjon.voicereminder.domain.ReminderStatus
import com.bekzodjon.voicereminder.notifications.NotificationHelper
import com.bekzodjon.voicereminder.parser.ParsedReminder
import com.bekzodjon.voicereminder.parser.UzbekReminderParser
import com.bekzodjon.voicereminder.scheduler.ReminderScheduler
import com.bekzodjon.voicereminder.utils.TimeUtils
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/** Single source of truth: database + alarms + notifications. Every status change is persisted atomically. */
class ReminderRepository(context: Context, private val dao: ReminderDao, val prefs: Prefs) {
    private val scheduler = ReminderScheduler(context)
    private val notifier = NotificationHelper(context)

    val all: Flow<List<ReminderEntity>> = dao.observeAll()

    fun canScheduleExact(): Boolean = scheduler.canScheduleExact()

    /** Parses (possibly many tasks), numbers, saves and schedules them. Returns how many reminders were created. */
    suspend fun addFromText(text: String, source: ReminderSource): Int {
        if (text.isBlank()) return 0
        var parsed = UzbekReminderParser(prefs.defaultTime).parse(text, LocalDateTime.now())
        if (parsed.isEmpty()) parsed = listOf(ParsedReminder(text.trim().replaceFirstChar { it.uppercaseChar() }, null))
        parsed.forEach { p ->
            insertAndSchedule(p.title, null, p.dateTime?.let { TimeUtils.toMillis(it) }, p.recurrence, p.interval, source)
        }
        return parsed.size
    }

    suspend fun addManual(title: String, description: String?, scheduledAt: Long?, recurrence: Recurrence, interval: Int): Long =
        insertAndSchedule(title, description, scheduledAt, recurrence, interval, ReminderSource.MANUAL)

    private suspend fun insertAndSchedule(
        title: String, description: String?, scheduledAt: Long?,
        recurrence: Recurrence, interval: Int, source: ReminderSource
    ): Long {
        val entity = ReminderEntity(
            seqNumber = 0, title = title, description = description?.takeIf { it.isNotBlank() },
            createdAt = System.currentTimeMillis(), scheduledAt = scheduledAt,
            recurrence = recurrence.name, recurrenceInterval = interval, source = source.name
        )
        val id = dao.insertWithSeq(entity)
        val saved = dao.get(id) ?: return id
        val at = saved.scheduledAt
        if (at != null) {
            scheduler.schedule(id, at)
        } else if (prefs.notifyUnscheduledNow) {
            notifier.showReminder(saved, alert = true)
            dao.markNotified(id, System.currentTimeMillis())
        }
        return id
    }

    suspend fun saveEdit(id: Long, title: String, description: String?, scheduledAt: Long?, recurrence: Recurrence, interval: Int) {
        val r = dao.get(id) ?: return
        dao.update(
            r.copy(
                title = title, description = description?.takeIf { it.isNotBlank() }, scheduledAt = scheduledAt,
                recurrence = recurrence.name, recurrenceInterval = interval,
                status = ReminderStatus.ACTIVE.name, completedAt = null
            )
        )
        refresh(id)
    }

    suspend fun reschedule(id: Long, atMillis: Long?) {
        dao.setSchedule(id, atMillis)
        refresh(id)
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        notifier.cancel(id)
        dao.delete(id)
    }

    private suspend fun refresh(id: Long) {
        dao.clearNotified(id)
        scheduler.cancel(id)
        notifier.cancel(id)
        val r = dao.get(id) ?: return
        val at = r.scheduledAt
        if (r.statusEnum == ReminderStatus.ACTIVE && at != null) scheduler.schedule(id, at)
    }

    // ---------- Notification / in-app actions (all idempotent) ----------

    /** Alarm fired. */
    suspend fun onFire(id: Long) {
        val r = dao.get(id) ?: return
        when (r.statusEnum) {
            ReminderStatus.ACTIVE -> {
                val at = r.scheduledAt
                if (at != null && at > System.currentTimeMillis() + 60_000) { scheduler.schedule(id, at); return }
                notifier.showReminder(r, alert = true)
                dao.markNotified(id, System.currentTimeMillis())
            }
            ReminderStatus.PENDING_CONFIRMATION -> notifier.showConfirm(r, alert = true)
            ReminderStatus.COMPLETED -> notifier.cancel(id)
        }
    }

    /** Stage 1. Never completes: ACTIVE -> PENDING_CONFIRMATION and shows the confirmation notification. */
    suspend fun onDone(id: Long) {
        val r = dao.get(id) ?: return
        val to = ReminderStateMachine.onDone(r.statusEnum)
        if (to != null && dao.transition(id, r.status, to.name) == 1) {
            dao.get(id)?.let { notifier.showConfirm(it, alert = true) }
        } else when (r.statusEnum) {
            ReminderStatus.PENDING_CONFIRMATION -> notifier.showConfirm(r, alert = false)
            ReminderStatus.COMPLETED -> notifier.cancel(id)
            ReminderStatus.ACTIVE -> Unit
        }
    }

    /** Stage 2. The only path to completion. Recurring reminders move on to their next occurrence instead. */
    suspend fun onConfirm(id: Long) {
        val r = dao.get(id) ?: return
        if (ReminderStateMachine.onConfirm(r.statusEnum) == null) {
            if (r.statusEnum == ReminderStatus.COMPLETED) notifier.cancel(id)
            return
        }
        val now = System.currentTimeMillis()
        val base = r.scheduledAt?.let { TimeUtils.toLocal(it) }
        val rec = r.recurrenceEnum
        val next = if (base != null && rec != Recurrence.NONE)
            RecurrenceCalculator.nextAfter(base, rec, r.recurrenceInterval, TimeUtils.toLocal(now)) else null
        val nextMs = next?.let { TimeUtils.toMillis(it) }
        val to = if (nextMs != null) ReminderStatus.ACTIVE else ReminderStatus.COMPLETED
        if (dao.confirm(id, to.name, now, nextMs ?: r.scheduledAt) == 1) {
            notifier.cancel(id)
            if (nextMs != null) scheduler.schedule(id, nextMs) else scheduler.cancel(id)
        }
    }

    /** NOT YET: back to ACTIVE and remind again after the snooze duration. */
    suspend fun onNotYet(id: Long) {
        val r = dao.get(id) ?: return
        val to = ReminderStateMachine.onNotYet(r.statusEnum) ?: return
        if (dao.transition(id, r.status, to.name) == 1) {
            dao.clearNotified(id) // a stray "dismissed" callback must not re-show it during the snooze
            notifier.cancel(id)
            scheduler.schedule(id, System.currentTimeMillis() + prefs.snoozeMinutes * 60_000L)
        }
    }

    /** Notification was swiped away: status is untouched; the notification comes back (silently). */
    suspend fun onDismissed(id: Long) {
        if (!prefs.reshowOnDismiss) return
        val r = dao.get(id) ?: return
        when (ReminderStateMachine.onSwipeDismiss(r.statusEnum)) {
            ReminderStatus.ACTIVE -> if (r.lastNotifiedAt != null) notifier.showReminder(r, alert = false)
            ReminderStatus.PENDING_CONFIRMATION -> notifier.showConfirm(r, alert = false)
            ReminderStatus.COMPLETED -> Unit
        }
    }

    /** After reboot / app update / time or zone change: re-arm every open reminder. */
    suspend fun restoreAll() {
        for (r in dao.openReminders()) {
            val at = r.scheduledAt
            when (r.statusEnum) {
                ReminderStatus.ACTIVE -> if (at != null) scheduler.schedule(r.id, at)
                ReminderStatus.PENDING_CONFIRMATION -> notifier.showConfirm(r, alert = false)
                ReminderStatus.COMPLETED -> Unit
            }
        }
    }
}
