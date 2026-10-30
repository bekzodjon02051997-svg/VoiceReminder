package com.bekzodjon.voicereminder.domain

enum class Recurrence { NONE, DAILY, WEEKLY, MONTHLY }

enum class ReminderStatus { ACTIVE, PENDING_CONFIRMATION, COMPLETED }

enum class ReminderSource { VOICE, MANUAL }
