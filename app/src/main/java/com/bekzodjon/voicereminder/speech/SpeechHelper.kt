package com.bekzodjon.voicereminder.speech

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent

/**
 * Uzbek speech-to-text through the system recognizer (uz-UZ). Uses the device's recognition service,
 * so it needs no API key. Whether it works offline depends on the installed speech pack.
 */
object SpeechHelper {
    const val LOCALE = "uz-UZ"

    fun recognizerIntent(prompt: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LOCALE)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, LOCALE)
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

    fun isAvailable(context: Context): Boolean =
        recognizerIntent("").resolveActivity(context.packageManager) != null

    fun extractText(data: Intent?): String? =
        data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}
