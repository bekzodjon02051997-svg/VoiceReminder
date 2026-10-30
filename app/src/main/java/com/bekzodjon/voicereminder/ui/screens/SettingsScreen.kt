package com.bekzodjon.voicereminder.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bekzodjon.voicereminder.R
import com.bekzodjon.voicereminder.data.Prefs
import com.bekzodjon.voicereminder.notifications.NotificationHelper
import com.bekzodjon.voicereminder.ui.components.pickTime

@Composable
private fun SwitchRow(title: String, desc: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (desc != null) Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsScreen(prefs: Prefs, canScheduleExact: Boolean) {
    val context = LocalContext.current
    var snooze by remember { mutableIntStateOf(prefs.snoozeMinutes) }
    var time by remember { mutableStateOf(prefs.defaultTime) }
    var reshow by remember { mutableStateOf(prefs.reshowOnDismiss) }
    var notifyNow by remember { mutableStateOf(prefs.notifyUnscheduledNow) }
    var autoVoice by remember { mutableStateOf(prefs.autoStartVoice) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.headlineSmall)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_snooze), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 30, 60).forEach { m ->
                    FilterChip(
                        selected = snooze == m,
                        onClick = { snooze = m; prefs.snoozeMinutes = m },
                        label = { Text(stringResource(R.string.settings_minutes, m)) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_default_time), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = {
                pickTime(context, time) { t -> time = t; prefs.defaultHour = t.hour; prefs.defaultMinute = t.minute }
            }) { Text("%02d:%02d".format(time.hour, time.minute)) }
        }

        SwitchRow(stringResource(R.string.settings_reshow), stringResource(R.string.settings_reshow_desc), reshow) { reshow = it; prefs.reshowOnDismiss = it }
        SwitchRow(stringResource(R.string.settings_notify_unscheduled), stringResource(R.string.settings_notify_unscheduled_desc), notifyNow) { notifyNow = it; prefs.notifyUnscheduledNow = it }
        SwitchRow(stringResource(R.string.settings_auto_voice), null, autoVoice) { autoVoice = it; prefs.autoStartVoice = it }

        OutlinedButton(onClick = {
            val i = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_ID)
            context.startActivity(i)
        }) { Text(stringResource(R.string.settings_sound)) }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_exact_alarm), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (canScheduleExact) R.string.settings_exact_alarm_on else R.string.settings_exact_alarm_off),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }) { Text(stringResource(R.string.open)) }
            }
        }

        Text(stringResource(R.string.voice_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
