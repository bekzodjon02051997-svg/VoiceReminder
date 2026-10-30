package com.bekzodjon.voicereminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bekzodjon.voicereminder.Graph
import com.bekzodjon.voicereminder.data.ReminderEntity
import com.bekzodjon.voicereminder.domain.Recurrence
import com.bekzodjon.voicereminder.domain.ReminderSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Graph.repository(app)
    val prefs = repo.prefs

    val reminders: StateFlow<List<ReminderEntity>> =
        repo.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun canScheduleExact(): Boolean = repo.canScheduleExact()

    fun addFromText(text: String, source: ReminderSource, onResult: (Int) -> Unit) {
        viewModelScope.launch { onResult(repo.addFromText(text, source)) }
    }

    fun addManual(title: String, description: String?, at: Long?, rec: Recurrence, interval: Int) {
        viewModelScope.launch { repo.addManual(title, description, at, rec, interval) }
    }

    fun saveEdit(id: Long, title: String, description: String?, at: Long?, rec: Recurrence, interval: Int) {
        viewModelScope.launch { repo.saveEdit(id, title, description, at, rec, interval) }
    }

    fun reschedule(id: Long, at: Long?) { viewModelScope.launch { repo.reschedule(id, at) } }
    fun delete(id: Long) { viewModelScope.launch { repo.delete(id) } }
    fun done(id: Long) { viewModelScope.launch { repo.onDone(id) } }
    fun confirm(id: Long) { viewModelScope.launch { repo.onConfirm(id) } }
    fun notYet(id: Long) { viewModelScope.launch { repo.onNotYet(id) } }
}
