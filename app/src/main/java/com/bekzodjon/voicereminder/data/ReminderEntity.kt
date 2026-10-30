package com.bekzodjon.voicereminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bekzodjon.voicereminder.domain.Recurrence
import com.bekzodjon.voicereminder.domain.ReminderStatus
import com.bekzodjon.voicereminder.utils.TimeUtils
import java.time.LocalDateTime

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Automatic sequence number shown as "#N". */
    val seqNumber: Int,
    val title: String,
    val description: String? = null,
    val createdAt: Long,
    /** Scheduled date+time as epoch millis. Null = unscheduled task. */
    val scheduledAt: Long? = null,
    val recurrence: String = Recurrence.NONE.name,
    val recurrenceInterval: Int = 1,
    /** ACTIVE, PENDING_CONFIRMATION or COMPLETED. */
    val status: String = ReminderStatus.ACTIVE.name,
    val lastNotifiedAt: Long? = null,
    val completedAt: Long? = null,
    /** VOICE or MANUAL. */
    val source: String = "MANUAL"
)

val ReminderEntity.statusEnum: ReminderStatus get() = ReminderStatus.valueOf(status)
val ReminderEntity.recurrenceEnum: Recurrence get() = Recurrence.valueOf(recurrence)
val ReminderEntity.scheduledLocal: LocalDateTime? get() = scheduledAt?.let { TimeUtils.toLocal(it) }
