package com.bekzodjon.voicereminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bekzodjon.voicereminder.R
import com.bekzodjon.voicereminder.data.ReminderEntity
import com.bekzodjon.voicereminder.ui.components.ReminderCallbacks
import com.bekzodjon.voicereminder.ui.components.ReminderCard
import com.bekzodjon.voicereminder.utils.TimeUtils
import java.time.LocalDate

@Composable
fun HomeScreen(
    today: List<ReminderEntity>,
    cb: ReminderCallbacks,
    onMic: () -> Unit,
    onType: () -> Unit,
    onAdd: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(TimeUtils.longDate(LocalDate.now()), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.summary_today, today.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onMic),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic, contentDescription = stringResource(R.string.add_voice),
                        modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.tap_to_speak), style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onType, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Keyboard, null, Modifier.size(18.dp))
                    Text(" " + stringResource(R.string.add_typed))
                }
                OutlinedButton(onClick = onAdd, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Text(" " + stringResource(R.string.add_reminder))
                }
            }
        }
        item { Text(stringResource(R.string.today_reminders), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
        if (today.isEmpty()) {
            item { Text(stringResource(R.string.empty_list), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(today, key = { it.id }) { ReminderCard(it, cb) }
    }
}
