package com.bekzodjon.voicereminder.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bekzodjon.voicereminder.MainActivity
import com.bekzodjon.voicereminder.R
import com.bekzodjon.voicereminder.data.ReminderEntity
import com.bekzodjon.voicereminder.receivers.ReminderActions

class NotificationHelper(private val context: Context) {

    init { createChannel() }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_HIGH)
            ch.description = context.getString(R.string.notif_channel_desc)
            ch.enableVibration(true)
            ch.lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    /** Stage 1 notification: the reminder with a DONE action. [alert] false = silent re-post after a swipe. */
    fun showReminder(r: ReminderEntity, alert: Boolean) {
        val text = r.title + (r.description?.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: "")
        val b = base(r, alert)
            .setContentTitle("#${r.seqNumber} · ${context.getString(R.string.notif_title)}")
            .setContentText(r.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .addAction(0, context.getString(R.string.done), broadcast(ReminderActions.DONE, r.id))
        post(r.id, b)
    }

    /** Stage 2 notification: CONFIRM DONE / NOT YET. Replaces the stage 1 notification (same id). */
    fun showConfirm(r: ReminderEntity, alert: Boolean) {
        val text = context.getString(R.string.notif_confirm_text, r.title)
        val b = base(r, alert)
            .setContentTitle("#${r.seqNumber} · ${context.getString(R.string.notif_confirm_title)}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .addAction(0, context.getString(R.string.confirm_done), broadcast(ReminderActions.CONFIRM, r.id))
            .addAction(0, context.getString(R.string.not_yet), broadcast(ReminderActions.NOT_YET, r.id))
        post(r.id, b)
    }

    fun cancel(id: Long) {
        NotificationManagerCompat.from(context).cancel(id.toInt())
    }

    private fun base(r: ReminderEntity, alert: Boolean): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Color.parseColor("#0F766E"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setSilent(!alert)
            .setContentIntent(openApp(r.id))
            // A swipe only re-posts the notification; it never completes anything.
            .setDeleteIntent(broadcast(ReminderActions.DISMISSED, r.id))

    @SuppressLint("MissingPermission")
    private fun post(id: Long, b: NotificationCompat.Builder) {
        val nm = NotificationManagerCompat.from(context)
        if (nm.areNotificationsEnabled()) nm.notify(id.toInt(), b.build())
    }

    private fun broadcast(action: String, id: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context, id.toInt(), ReminderActions.intent(context, action, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun openApp(id: Long): PendingIntent =
        PendingIntent.getActivity(
            context, id.toInt(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val CHANNEL_ID = "reminders"
    }
}
