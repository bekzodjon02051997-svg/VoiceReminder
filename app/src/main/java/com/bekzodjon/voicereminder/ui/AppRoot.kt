package com.bekzodjon.voicereminder.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bekzodjon.voicereminder.R
import com.bekzodjon.voicereminder.data.ReminderEntity
import com.bekzodjon.voicereminder.domain.ReminderSource
import com.bekzodjon.voicereminder.speech.SpeechHelper
import com.bekzodjon.voicereminder.ui.components.ConfirmDeleteDialog
import com.bekzodjon.voicereminder.ui.components.ReminderCallbacks
import com.bekzodjon.voicereminder.ui.components.ReminderEditDialog
import com.bekzodjon.voicereminder.ui.components.TextInputDialog
import com.bekzodjon.voicereminder.ui.screens.CompletedScreen
import com.bekzodjon.voicereminder.ui.screens.HomeScreen
import com.bekzodjon.voicereminder.ui.screens.SettingsScreen
import com.bekzodjon.voicereminder.ui.screens.UpcomingScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun AppRoot(vm: MainViewModel) {
    val context = LocalContext.current
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showText by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<ReminderEntity?>(null) }
    var rescheduleTarget by remember { mutableStateOf<ReminderEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<ReminderEntity?>(null) }

    fun say(msg: String) { scope.launch { snack.showSnackbar(msg) } }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val text = SpeechHelper.extractText(res.data)
            if (text == null) {
                say(context.getString(R.string.voice_failed))
                showText = true
            } else {
                vm.addFromText(text, ReminderSource.VOICE) { n -> say(context.getString(R.string.added_count, n)) }
            }
        }
    }

    val launchSpeech: () -> Unit = {
        if (!SpeechHelper.isAvailable(context)) {
            say(context.getString(R.string.voice_unavailable))
            showText = true
        } else {
            try {
                speechLauncher.launch(SpeechHelper.recognizerIntent(context.getString(R.string.speak_prompt)))
            } catch (e: ActivityNotFoundException) {
                say(context.getString(R.string.voice_unavailable))
                showText = true
            }
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> launchSpeech() }
    val requestVoice: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) launchSpeech()
        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }
    var autoStarted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!autoStarted && vm.prefs.autoStartVoice) {
            autoStarted = true
            requestVoice()
        }
    }

    val cb = ReminderCallbacks(
        onDone = { vm.done(it) },
        onConfirm = { vm.confirm(it) },
        onNotYet = { vm.notYet(it) },
        onEdit = { editTarget = it },
        onReschedule = { rescheduleTarget = it },
        onDelete = { deleteTarget = it }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Default.Home, null) }, label = { Text(stringResource(R.string.nav_home)) })
                NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Default.Event, null) }, label = { Text(stringResource(R.string.nav_upcoming)) })
                NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Default.CheckCircle, null) }, label = { Text(stringResource(R.string.nav_completed)) })
                NavigationBarItem(tab == 3, { tab = 3 }, { Icon(Icons.Default.Settings, null) }, label = { Text(stringResource(R.string.nav_settings)) })
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeScreen(
                    today = ReminderFilters.today(reminders, LocalDate.now()),
                    cb = cb,
                    onMic = requestVoice,
                    onType = { showText = true },
                    onAdd = { showCreate = true }
                )
                1 -> UpcomingScreen(reminders, cb)
                2 -> CompletedScreen(reminders, cb)
                else -> SettingsScreen(vm.prefs, vm.canScheduleExact())
            }
        }
    }

    if (showText) {
        TextInputDialog(
            initial = "",
            onSubmit = { text ->
                showText = false
                vm.addFromText(text, ReminderSource.MANUAL) { n -> say(context.getString(R.string.added_count, n)) }
            },
            onDismiss = { showText = false }
        )
    }
    if (showCreate) {
        ReminderEditDialog(
            initial = null, rescheduleOnly = false, defaultTime = vm.prefs.defaultTime,
            onSave = { r ->
                showCreate = false
                vm.addManual(r.title, r.description, r.scheduledAt, r.recurrence, r.interval)
            },
            onDismiss = { showCreate = false }
        )
    }
    editTarget?.let { target ->
        ReminderEditDialog(
            initial = target, rescheduleOnly = false, defaultTime = vm.prefs.defaultTime,
            onSave = { r ->
                editTarget = null
                vm.saveEdit(target.id, r.title, r.description, r.scheduledAt, r.recurrence, r.interval)
            },
            onDismiss = { editTarget = null }
        )
    }
    rescheduleTarget?.let { target ->
        ReminderEditDialog(
            initial = target, rescheduleOnly = true, defaultTime = vm.prefs.defaultTime,
            onSave = { r ->
                rescheduleTarget = null
                vm.reschedule(target.id, r.scheduledAt)
            },
            onDismiss = { rescheduleTarget = null }
        )
    }
    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            r = target,
            onConfirm = { deleteTarget = null; vm.delete(target.id) },
            onDismiss = { deleteTarget = null }
        )
    }
}
