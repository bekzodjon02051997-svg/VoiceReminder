package com.bekzodjon.voicereminder.receivers

import android.content.Context
import android.content.Intent
import android.net.Uri

object ReminderActions {
    const val FIRE = "com.bekzodjon.voicereminder.action.FIRE"
    const val DONE = "com.bekzodjon.voicereminder.action.DONE"
    const val CONFIRM = "com.bekzodjon.voicereminder.action.CONFIRM"
    const val NOT_YET = "com.bekzodjon.voicereminder.action.NOT_YET"
    const val DISMISSED = "com.bekzodjon.voicereminder.action.DISMISSED"
    const val EXTRA_ID = "reminder_id"

    /** Explicit intent; the unique data URI keeps every (reminder, action) PendingIntent distinct. */
    fun intent(context: Context, action: String, id: Long): Intent =
        Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("reminder://$id/${action.substringAfterLast('.')}"))
            .putExtra(EXTRA_ID, id)
}
