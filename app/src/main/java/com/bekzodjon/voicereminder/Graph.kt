package com.bekzodjon.voicereminder

import android.content.Context
import com.bekzodjon.voicereminder.data.Prefs
import com.bekzodjon.voicereminder.database.AppDatabase
import com.bekzodjon.voicereminder.repository.ReminderRepository

/** Minimal service locator shared by the UI and the broadcast receivers. */
object Graph {
    @Volatile private var repo: ReminderRepository? = null

    fun repository(context: Context): ReminderRepository = repo ?: synchronized(this) {
        repo ?: build(context.applicationContext).also { repo = it }
    }

    private fun build(app: Context): ReminderRepository =
        ReminderRepository(app, AppDatabase.get(app).reminderDao(), Prefs(app))
}
