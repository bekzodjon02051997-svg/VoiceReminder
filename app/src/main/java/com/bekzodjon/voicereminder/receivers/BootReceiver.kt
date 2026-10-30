package com.bekzodjon.voicereminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bekzodjon.voicereminder.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-arms alarms after reboot, app update, clock / timezone change or exact-alarm permission change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Graph.repository(app).restoreAll()
            } finally {
                pending.finish()
            }
        }
    }
}
