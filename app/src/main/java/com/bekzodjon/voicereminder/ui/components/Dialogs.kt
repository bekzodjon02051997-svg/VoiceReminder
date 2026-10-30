package com.bekzodjon.voicereminder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bekzodjon.voicereminder.R
import com.bekzodjon.voicereminder.data.ReminderEntity
import com.bekzodjon.voicereminder.data.recurrenceEnum
import com.bekzodjon.voicereminder.data.scheduledLocal
import com.bekzodjon.voicereminder.domain.Recurrence
import com.bekzodjon.voicereminder.utils.TimeUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Free-text entry (also the fallback when speech recognition is unavailable or fails). */
@Composable
fun TextInputDialog(initial: String, onSubmit: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_typed)) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text(stringResource(R.string.type_hint)) },
                modifier = Modifier.fillMaxWidth(), minLines = 3
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onSubmit(text.trim()) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun ConfirmDeleteDialog(r: ReminderEntity, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_confirm)) },
        text = { Text("#${r.seqNumber} ${r.title}") },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

data class EditResult(
    val title: String,
    val description: String?,
    val scheduledAt: Long?,
    val recurrence: Recurrence,
    val interval: Int
)

/** Create / edit dialog. With [rescheduleOnly] only date and time are shown. */
@Composable
fun ReminderEditDialog(
    initial: ReminderEntity?,
    rescheduleOnly: Boolean,
    defaultTime: LocalTime,
    onSave: (EditResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialDt = initial?.scheduledLocal
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var date by remember { mutableStateOf<LocalDate?>(initialDt?.toLocalDate()) }
    var time by remember { mutableStateOf<LocalTime?>(initialDt?.toLocalTime()) }
    var recurrence by remember { mutableStateOf(initial?.recurrenceEnum ?: Recurrence.NONE) }
    var intervalText by remember { mutableStateOf((initial?.recurrenceInterval ?: 1).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.add_reminder else if (rescheduleOnly) R.string.reschedule else R.string.edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!rescheduleOnly) {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text(stringResource(R.string.title_label)) }, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc, onValueChange = { desc = it },
                        label = { Text(stringResource(R.string.desc_label)) }, modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        pickDate(context, date ?: LocalDate.now()) { date = it }
                    }) { Text(date?.let { TimeUtils.formatDate(context, it) } ?: stringResource(R.string.pick_date)) }
                    OutlinedButton(onClick = {
                        pickTime(context, time ?: defaultTime) { time = it }
                    }) { Text(time?.let { "%02d:%02d".format(it.hour, it.minute) } ?: stringResource(R.string.pick_time)) }
                }
                if (date != null || time != null) {
                    TextButton(onClick = { date = null; time = null }) { Text(stringResource(R.string.clear_schedule)) }
                }
                if (!rescheduleOnly) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(recurrence == Recurrence.NONE, { recurrence = Recurrence.NONE }, { Text(stringResource(R.string.repeat_none)) })
                        FilterChip(recurrence == Recurrence.DAILY, { recurrence = Recurrence.DAILY }, { Text(stringResource(R.string.repeat_daily)) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(recurrence == Recurrence.WEEKLY, { recurrence = Recurrence.WEEKLY }, { Text(stringResource(R.string.repeat_weekly)) })
                        FilterChip(recurrence == Recurrence.MONTHLY, { recurrence = Recurrence.MONTHLY }, { Text(stringResource(R.string.repeat_monthly)) })
                    }
                    if (recurrence != Recurrence.NONE) {
                        OutlinedTextField(
                            value = intervalText, onValueChange = { intervalText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text(stringResource(R.string.repeat_interval)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = rescheduleOnly || title.isNotBlank(),
                onClick = {
                    val rec = if (rescheduleOnly) (initial?.recurrenceEnum ?: Recurrence.NONE) else recurrence
                    val interval = if (rescheduleOnly) (initial?.recurrenceInterval ?: 1)
                    else (intervalText.toIntOrNull() ?: 1).coerceAtLeast(1)
                    val hasSchedule = date != null || time != null || rec != Recurrence.NONE
                    val dt: LocalDateTime? = if (!hasSchedule) null else {
                        val t = time ?: defaultTime
                        val d = date ?: if (LocalDateTime.of(LocalDate.now(), t).isAfter(LocalDateTime.now())) LocalDate.now() else LocalDate.now().plusDays(1)
                        LocalDateTime.of(d, t)
                    }
                    onSave(
                        EditResult(
                            title = if (rescheduleOnly) (initial?.title ?: "") else title.trim(),
                            description = if (rescheduleOnly) initial?.description else desc.trim().ifBlank { null },
                            scheduledAt = dt?.let { TimeUtils.toMillis(it) },
                            recurrence = rec,
                            interval = interval
                        )
                    )
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
