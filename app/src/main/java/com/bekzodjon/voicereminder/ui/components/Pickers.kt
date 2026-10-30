package com.bekzodjon.voicereminder.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.time.LocalDate
import java.time.LocalTime

fun pickDate(context: Context, initial: LocalDate, onPick: (LocalDate) -> Unit) {
    DatePickerDialog(
        context, { _, y, m, d -> onPick(LocalDate.of(y, m + 1, d)) },
        initial.year, initial.monthValue - 1, initial.dayOfMonth
    ).show()
}

fun pickTime(context: Context, initial: LocalTime, onPick: (LocalTime) -> Unit) {
    TimePickerDialog(context, { _, h, m -> onPick(LocalTime.of(h, m)) }, initial.hour, initial.minute, true).show()
}
