package com.bekzodjon.voicereminder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bekzodjon.voicereminder.R
import com.bekzodjon.voicereminder.data.ReminderEntity
import com.bekzodjon.voicereminder.ui.ReminderFilters
import com.bekzodjon.voicereminder.ui.components.ReminderCallbacks
import com.bekzodjon.voicereminder.ui.components.ReminderCard
import java.time.LocalDate

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query, onValueChange = onChange, singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, null) },
        label = { Text(stringResource(R.string.search)) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RemindersColumn(items: List<ReminderEntity>, cb: ReminderCallbacks) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (items.isEmpty()) {
            item { Text(stringResource(R.string.empty_list), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(items, key = { it.id }) { ReminderCard(it, cb) }
    }
}

/** UPCOMING / ALL REMINDERS / UNSCHEDULED, switchable with chips, with search. */
@Composable
fun UpcomingScreen(all: List<ReminderEntity>, cb: ReminderCallbacks) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val base = when (filter) {
        0 -> ReminderFilters.upcoming(all, LocalDate.now())
        1 -> all
        else -> ReminderFilters.unscheduled(all)
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.nav_upcoming), style = MaterialTheme.typography.headlineSmall)
        SearchField(query) { query = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(filter == 0, { filter = 0 }, { Text(stringResource(R.string.filter_upcoming)) })
            FilterChip(filter == 1, { filter = 1 }, { Text(stringResource(R.string.filter_all)) })
            FilterChip(filter == 2, { filter = 2 }, { Text(stringResource(R.string.filter_unscheduled)) })
        }
        RemindersColumn(ReminderFilters.search(base, query), cb)
    }
}

@Composable
fun CompletedScreen(all: List<ReminderEntity>, cb: ReminderCallbacks) {
    var query by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.nav_completed), style = MaterialTheme.typography.headlineSmall)
        SearchField(query) { query = it }
        RemindersColumn(ReminderFilters.search(ReminderFilters.completed(all), query), cb)
    }
}
