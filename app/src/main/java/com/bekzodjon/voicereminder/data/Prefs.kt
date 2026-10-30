package com.bekzodjon.voicereminder.data

import android.content.Context
import java.time.LocalTime

/** User settings stored in SharedPreferences. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var snoozeMinutes: Int
        get() = sp.getInt("snooze", 10)
        set(v) = sp.edit().putInt("snooze", v).apply()

    var defaultHour: Int
        get() = sp.getInt("default_hour", 9)
        set(v) = sp.edit().putInt("default_hour", v).apply()

    var defaultMinute: Int
        get() = sp.getInt("default_minute", 0)
        set(v) = sp.edit().putInt("default_minute", v).apply()

    val defaultTime: LocalTime get() = LocalTime.of(defaultHour, defaultMinute)

    var reshowOnDismiss: Boolean
        get() = sp.getBoolean("reshow", true)
        set(v) = sp.edit().putBoolean("reshow", v).apply()

    var notifyUnscheduledNow: Boolean
        get() = sp.getBoolean("notify_unscheduled", false)
        set(v) = sp.edit().putBoolean("notify_unscheduled", v).apply()

    var autoStartVoice: Boolean
        get() = sp.getBoolean("auto_voice", false)
        set(v) = sp.edit().putBoolean("auto_voice", v).apply()
}
