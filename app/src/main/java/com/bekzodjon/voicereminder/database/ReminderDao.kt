package com.bekzodjon.voicereminder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bekzodjon.voicereminder.data.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ReminderDao {
    @Insert
    abstract suspend fun insertRaw(r: ReminderEntity): Long

    @Query("SELECT COALESCE(MAX(seqNumber), 0) + 1 FROM reminders")
    abstract suspend fun nextSeq(): Int

    /** Inserts with the next automatic sequence number, atomically. */
    @Transaction
    open suspend fun insertWithSeq(r: ReminderEntity): Long = insertRaw(r.copy(seqNumber = nextSeq()))

    @Update
    abstract suspend fun update(r: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("SELECT * FROM reminders WHERE id = :id")
    abstract suspend fun get(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders ORDER BY CASE WHEN scheduledAt IS NULL THEN 1 ELSE 0 END, scheduledAt, seqNumber")
    abstract fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status != 'COMPLETED'")
    abstract suspend fun openReminders(): List<ReminderEntity>

    /** Atomic compare-and-set status change; returns 1 if it happened, 0 if the status was different (duplicate action). */
    @Query("UPDATE reminders SET status = :newStatus WHERE id = :id AND status = :oldStatus")
    abstract suspend fun transition(id: Long, oldStatus: String, newStatus: String): Int

    /** CONFIRM DONE: only succeeds from PENDING_CONFIRMATION. For recurring reminders [newStatus] is ACTIVE with the next occurrence. */
    @Query("UPDATE reminders SET status = :newStatus, completedAt = :completedAt, scheduledAt = :scheduledAt WHERE id = :id AND status = 'PENDING_CONFIRMATION'")
    abstract suspend fun confirm(id: Long, newStatus: String, completedAt: Long, scheduledAt: Long?): Int

    @Query("UPDATE reminders SET scheduledAt = :at, status = 'ACTIVE', completedAt = NULL WHERE id = :id")
    abstract suspend fun setSchedule(id: Long, at: Long?)

    @Query("UPDATE reminders SET lastNotifiedAt = :t WHERE id = :id")
    abstract suspend fun markNotified(id: Long, t: Long)

    @Query("UPDATE reminders SET lastNotifiedAt = NULL WHERE id = :id")
    abstract suspend fun clearNotified(id: Long)
}
