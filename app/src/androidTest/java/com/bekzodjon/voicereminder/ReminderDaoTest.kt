package com.bekzodjon.voicereminder

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bekzodjon.voicereminder.data.ReminderEntity
import com.bekzodjon.voicereminder.database.AppDatabase
import com.bekzodjon.voicereminder.domain.ReminderStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun tearDown() { db.close() }

    private fun sample(title: String) = ReminderEntity(seqNumber = 0, title = title, createdAt = 1L)

    @Test fun sequenceNumbersAreAutomatic() = runBlocking {
        val dao = db.reminderDao()
        val a = dao.insertWithSeq(sample("a"))
        val b = dao.insertWithSeq(sample("b"))
        assertEquals(1, dao.get(a)!!.seqNumber)
        assertEquals(2, dao.get(b)!!.seqNumber)
    }

    @Test fun twoStageCompletionIsEnforcedInSql() = runBlocking {
        val dao = db.reminderDao()
        val id = dao.insertWithSeq(sample("x"))
        // CONFIRM without DONE must not complete.
        assertEquals(0, dao.confirm(id, ReminderStatus.COMPLETED.name, 5L, null))
        assertEquals(1, dao.transition(id, ReminderStatus.ACTIVE.name, ReminderStatus.PENDING_CONFIRMATION.name))
        // A duplicate DONE is a no-op.
        assertEquals(0, dao.transition(id, ReminderStatus.ACTIVE.name, ReminderStatus.PENDING_CONFIRMATION.name))
        assertEquals(1, dao.confirm(id, ReminderStatus.COMPLETED.name, 5L, null))
        assertEquals(ReminderStatus.COMPLETED.name, dao.get(id)!!.status)
    }
}
