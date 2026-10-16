# Task Manager for Android

Offline-first personal task manager built against `PRD_Task_Manager_App.md` with Kotlin, Jetpack Compose, Room, DataStore, WorkManager, AlarmManager, Hilt, and Glance.

## Requirements

- JDK 17
- Android SDK platform 35 and Build Tools 35
- Gradle 8.9 (the checked-in wrapper is used)
- minSdk 26

The app does not request `android.permission.INTERNET`.

## Build and verify

Set `ANDROID_HOME` or create an ignored `local.properties` containing `sdk.dir=...`, then run:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

`.github/workflows/android.yml` runs the unit tests and creates a debug APK artifact on pushes to `main` and pull requests. A `v*.*.*` tag builds a signed release APK and attaches it to a GitHub Release.

Configure these repository Actions secrets before creating a release tag:

- `KEYSTORE_BASE64`: base64-encoded upload keystore
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The release job writes temporary signing files only on the runner. Do not commit keystores or passwords.

## Implemented scope

- Tasks, notes, subtasks, tags, priorities, due dates, sorting, soft-delete trash and undo
- Inbox and Folder > List > Sublist organization
- Multiple reminders, notification actions, high-priority alarm screen, reboot/update/time-change rescheduling, and reliability guidance
- FTS search and filters
- Recurring task series and materialized occurrences
- Boolean and numeric habits, schedules, streaks, weekly freeze, and nightly reminders
- Append-only points ledger, daily penalties, badges, levels, rewards, and reset history
- Pomodoro foreground service, optional DND, Today and Pomodoro widgets
- Month calendar, weekly insights, completion-hour analysis, habit heatmap, and postpone alerts
- English and Arabic per-app language, RTL layout, themes, onboarding, and quick-add parsing
- Versioned manual and scheduled SAF backups, including app data and settings; replace-all import rebooks reminders and rebuilds search

## Data and release notes

The app keeps its database locally, has a versioned Room schema, and does not use destructive migrations. `Migrations.ALL` is empty for the initial schema. Device-level reminder reliability, backup/restore behavior, battery use, and performance with thousands of tasks still require real-device verification.

A GitHub release build requires the signing secrets above. No signing secrets or private keys are stored in this repository.
