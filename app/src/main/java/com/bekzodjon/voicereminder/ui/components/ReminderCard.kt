package com.bekzodjon.voicereminder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bekzodjon.voicereminder.R
import com.bekzodjon.voicereminder.data.ReminderEntity
import com.bekzodjon.voicereminder.data.recurrenceEnum
import com.bekzodjon.voicereminder.data.statusEnum
import com.bekzodjon.voicereminder.domain.Recurrence
import com.bekzodjon.voicereminder.domain.ReminderStatus
import com.bekzodjon.voicereminder.utils.TimeUtils

data class ReminderCallbacks(
    val onDone: (Long) -> Unit,
    val onConfirm: (Long) -> Unit,
    val onNotYet: (Long) -> Unit,
    val onEdit: (ReminderEntity) -> Unit,
    val onReschedule: (ReminderEntity) -> Unit,
    val onDelete: (ReminderEntity) -> Unit
)

@Composable
fun recurrenceLabel(r: ReminderEntity): String? {
    val n = r.recurrenceInterval
    return when (r.recurrenceEnum) {
        Recurrence.NONE -> null
        Recurrence.DAILY -> if (n <= 1) stringResource(R.string.repeat_daily) else stringResource(R.string.repeat_every_days, n)
        Recurrence.WEEKLY -> if (n <= 1) stringResource(R.string.repeat_weekly) else stringResource(R.string.repeat_every_weeks, n)
        Recurrence.MONTHLY -> if (n <= 1) stringResource(R.string.repeat_monthly) else stringResource(R.string.repeat_every_months, n)
    }
}

@Composable
fun ReminderCard(r: ReminderEntity, cb: ReminderCallbacks, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val status = r.statusEnum
    val completed = status == ReminderStatus.COMPLETED
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Text(
                        "${r.seqNumber}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        r.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (completed) TextDecoration.LineThrough else null
                    )
                    Text(
                        TimeUtils.formatSchedule(context, r.scheduledAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            r.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val statusText = when (status) {
                ReminderStatus.ACTIVE -> stringResource(R.string.status_active)
                ReminderStatus.PENDING_CONFIRMATION -> stringResource(R.string.status_pending)
                ReminderStatus.COMPLETED -> r.completedAt?.let {
                    stringResource(R.string.completed_on, TimeUtils.formatSchedule(context, it))
                } ?: stringResource(R.string.status_completed)
            }
            val repeat = recurrenceLabel(r)
            Text(
                listOfNotNull(statusText, repeat).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (status) {
                    ReminderStatus.ACTIVE -> Button(onClick = { cb.onDone(r.id) }) { Text(stringResource(R.string.done)) }
                    ReminderStatus.PENDING_CONFIRMATION -> {
                        Button(onClick = { cb.onConfirm(r.id) }) { Text(stringResource(R.string.confirm_done)) }
                        OutlinedButton(onClick = { cb.onNotYet(r.id) }) { Text(stringResource(R.string.not_yet)) }
                    }
                    ReminderStatus.COMPLETED -> Unit
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!completed) {
                    IconButton(onClick = { cb.onEdit(r) }) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
                    IconButton(onClick = { cb.onReschedule(r) }) { Icon(Icons.Default.Schedule, stringResource(R.string.reschedule)) }
                }
                IconButton(onClick = { cb.onDelete(r) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
            }
        }
    }
}
