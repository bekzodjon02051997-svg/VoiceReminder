package com.bekzodjon.voicereminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bekzodjon.voicereminder.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles alarm firing and all notification buttons. Works with the app closed. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderActions.EXTRA_ID, -1L)
        val action = intent.action
        if (id < 0 || action == null) return
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = Graph.repository(app)
                when (action) {
                    ReminderActions.FIRE -> repo.onFire(id)
                    ReminderActions.DONE -> repo.onDone(id)
                    ReminderActions.CONFIRM -> repo.onConfirm(id)
                    ReminderActions.NOT_YET -> repo.onNotYet(id)
                    ReminderActions.DISMISSED -> repo.onDismissed(id)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
