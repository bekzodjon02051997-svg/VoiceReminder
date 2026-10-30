# Voice Reminder (Ovozli eslatma)

Native Android reminder app controlled by **Uzbek voice input**. Speak (or type) one sentence with several tasks and the app splits it into separate, numbered reminders and schedules a notification for each.

> Package: `com.bekzodjon.voicereminder` · Kotlin · Jetpack Compose · Room · AlarmManager · min SDK 26 (Android 8.0), target SDK 34

## Features
- Voice input in Uzbek (`uz-UZ`) through the system speech recognizer; manual text fallback if recognition fails or is unavailable
- Offline Uzbek parser: splits "Ertaga soat 9 da darsga borishim kerak, soat 12 da Akmalga qo'ng'iroq qilishim kerak, soat 5 da do'konga borishim kerak" into 3 reminders (09:00, 12:00, 17:00 tomorrow)
- No date/time spoken -> saved as **Unscheduled** (nothing is invented); assign a time later
- Automatic reminder numbers (#1, #2, ...)
- **Two-stage completion**: notification `DONE` -> second notification `CONFIRM DONE` / `NOT YET`. Only CONFIRM DONE completes. Swiping a notification never completes anything and the notification comes back (can be turned off in Settings)
- Actions work with the app closed (BroadcastReceiver + PendingIntent); every status change is an atomic database update, so duplicate taps are ignored
- Recurring: daily, weekly, monthly, every N days/weeks/months; confirming one occurrence moves to the next instead of deleting
- Today / Upcoming / All / Completed / Unscheduled, search, edit, reschedule, delete
- Light and dark theme, Uzbek-first UI
- Survives reboot, app update, clock and timezone changes (`BootReceiver`)

## Screenshots
_Placeholders - add your own images here:_
- `docs/screenshot-home.png`
- `docs/screenshot-notification.png`

## Spoken examples
| You say | Result |
|---|---|
| Bugun soat 15:00 da Akmalga qo'ng'iroq qilishni eslat | "Akmalga qo'ng'iroq qilish" today 15:00 |
| Har kuni ertalab soat 7 da dori ichishni eslat | "Dori ichish", daily 07:00 |
| Har dushanba soat 10 da yig'ilish | weekly, Monday 10:00 |
| Har oyning 5-kuni to'lov qilish | monthly, day 5, default time |
| Do'kondan non, sut va tuxum olishim kerak | one unscheduled reminder |
| 30 daqiqadan keyin choy qo'yish | in 30 minutes |

Rules: `soat 1-6` without a period is PM (soat 5 = 17:00), `soat 7-11` is AM; `ertalab/kechqurun/kechasi` override this. A time without a date means today, or tomorrow if it has passed. A date without a time uses the default time from Settings (09:00).

## Architecture (MVVM)
```
app/src/main/java/com/bekzodjon/voicereminder/
  MainActivity, VoiceReminderApp, Graph (service locator)
  data/          ReminderEntity, Prefs
  database/      AppDatabase, ReminderDao (atomic transitions)
  repository/    ReminderRepository (DB + alarms + notifications)
  domain/        enums, ReminderStateMachine, RecurrenceCalculator   (pure Kotlin)
  parser/        UzbekReminderParser                                  (pure Kotlin)
  scheduler/     ReminderScheduler (AlarmManager)
  notifications/ NotificationHelper (channel, DONE / CONFIRM / NOT YET)
  receivers/     ReminderReceiver, BootReceiver, ReminderActions
  speech/        SpeechHelper (uz-UZ)
  ui/            AppRoot, MainViewModel, screens/, components/, theme/
```

## Build
**Android Studio:** open the `VoiceReminder` folder (the one containing `settings.gradle.kts`), let Gradle sync, press Run. Requires JDK 17 (bundled with recent Android Studio).

**Command line:** `./gradlew assembleDebug` -> `app/build/outputs/apk/debug/app-debug.apk`; unit tests: `./gradlew testDebugUnitTest`.

**GitHub Actions:** `.github/workflows/build.yml` builds the debug APK on every push; download it from the run's *Artifacts* section.

## Upload to GitHub (no nested folder)
1. Unzip `VoiceReminder_GitHub_Ready.zip`. You get a folder `VoiceReminder/`.
2. Open that folder so you see `app/`, `gradle/`, `README.md`, `settings.gradle.kts`.
3. Create an empty repository on GitHub, click *uploading an existing file*, and drag the **contents** of `VoiceReminder/` (not the folder itself) into the page. Make sure the hidden `.github` and `.gitignore` are included. Commit.
4. With git instead: `cd VoiceReminder && git init && git add . && git commit -m "Initial commit" && git branch -M main && git remote add origin <url> && git push -u origin main`.
5. If Actions reports `gradlew: Permission denied`, the workflow already runs `chmod +x gradlew`.

## Permissions
| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` (Android 13+) | show reminders |
| `RECORD_AUDIO` | voice input |
| `SCHEDULE_EXACT_ALARM` (Android 12+) | on-time alarms; without it alarms may be late. Grant in Settings -> "Aniq vaqtli signal ruxsati" |
| `RECEIVE_BOOT_COMPLETED` | re-arm alarms after restart |
| `VIBRATE` | notification vibration |

## Known limitations
- Speech recognition is provided by the device's speech service (usually Google). Uzbek support and offline packs depend on the device; if unavailable the app shows a message and a text box.
- The parser understands Latin-script Uzbek; Cyrillic Uzbek is not parsed. Very free-form phrasing may need a manual edit.
- Notification sound/vibration is configured in the system channel settings (button in Settings), as Android requires.
- Some manufacturers (Xiaomi, Huawei, Oppo, Samsung) kill background apps: disable battery optimization for the app.
- Android may let users block exact alarms or notifications at any time.
- A swiped notification is re-posted by the app; users can still turn notifications off in system settings.

## Troubleshooting
- *Mic button shows "ovozni aniqlash xizmati topilmadi"*: install/update the Google app and an Uzbek voice pack, or type the text.
- *No notifications*: allow notifications for the app and check that the "Eslatmalar" channel is not muted.
- *Reminders late*: grant the exact alarm permission and disable battery optimization.
- *Gradle sync fails*: use JDK 17 and an internet connection to Google/Maven repositories.

## Testing status
Pure-Kotlin logic (`parser`, `domain`) has JUnit tests in `app/src/test`; an instrumented Room test is in `app/src/androidTest`. See the delivery notes for what was actually run.

## License
MIT
