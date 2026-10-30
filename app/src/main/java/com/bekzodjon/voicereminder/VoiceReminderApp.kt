package com.bekzodjon.voicereminder

import android.app.Application
import com.bekzodjon.voicereminder.notifications.NotificationHelper

class VoiceReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper(this) // creates the notification channel
    }
}
