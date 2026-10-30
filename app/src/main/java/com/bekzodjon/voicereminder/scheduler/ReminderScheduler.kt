package com.bekzodjon.voicereminder.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import com.bekzodjon.voicereminder.receivers.ReminderActions

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(id: Long, atMillis: Long) {
        val trigger = maxOf(atMillis, System.currentTimeMillis() + 1_000)
        val pi = fireIntent(id, create = true) ?: return
        try {
            if (canScheduleExact()) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun cancel(id: Long) {
        fireIntent(id, create = false)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun fireIntent(id: Long, create: Boolean): PendingIntent? {
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, id.toInt(), ReminderActions.intent(context, ReminderActions.FIRE, id), flags)
    }
}
