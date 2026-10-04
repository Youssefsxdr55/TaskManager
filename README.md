# Task Manager — Android (v1.0 Core)

Offline task manager built from `PRD_Task_Manager_App.md`. Kotlin + Jetpack Compose +
Material 3 + Room, MVVM with unidirectional data flow, no network permission declared.

Status: **v1.0 scope implemented, not yet compiled.** See "Verification status" below.

## Requirements to build

| Tool | Version used |
|---|---|
| JDK | 17 |
| Android SDK | compileSdk 35, build-tools 35 |
| Gradle | 8.9+ (via the wrapper — no local Gradle needed) |
| minSdk | 26 (Android 8.0) |

## Build

1. Open the folder in Android Studio (Hedgehog or newer) and let it sync.
2. Or from the command line:

```bash
# Point at your SDK once
echo "sdk.dir=C:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk" > local.properties

./gradlew :app:testDebugUnitTest      # run the 77 unit tests
./gradlew :app:assembleDebug          # debug APK -> app/build/outputs/apk/debug/
./gradlew :app:assembleRelease        # unsigned unless keystore.properties exists
```

There is no Gradle wrapper JAR checked in. Either open in Android Studio (it generates
one) or run `gradle wrapper --gradle-version 8.9` once from a machine that has Gradle.

## Signed release builds

Create `keystore.properties` in the project root (gitignored):

```properties
storeFile=../keystore/taskmanager.jks
storePassword=...
keyAlias=taskmanager
keyPassword=...
```

`app/build.gradle.kts` detects the file and applies the signing config automatically.
Absent that file, `assembleRelease` still produces an unsigned APK.

## What is implemented (v1.0)

- **Tasks** — title/notes/subtasks/tags/priority/due date+time, soft-delete trash (30d),
  undo on complete, sorting (due/priority/created/manual)
- **Organization** — Folder > List > Sublist with depth validation, implicit Inbox
- **Reminders** — multiple per task, three types, exact alarms with inexact fallback,
  reschedule on reboot / app update / time / timezone change, standard notification with
  Done + Snooze actions, full-screen alarm for High priority
- **Reliability check** — notifications, exact alarm, full-screen intent, battery
  optimization, plus OEM guidance for Xiaomi/Samsung/Oppo/Huawei/Vivo
- **Search** — Room FTS4 over titles, notes, subtasks, tags; input escaped by
  `SearchQueryBuilder`
- **Backup** — manual export/import and automatic daily backup to a SAF folder with
  persisted URI permission (survives uninstall), retention pruning, versioned JSON schema
- **Settings / onboarding** — language (AR/EN), theme, snooze duration
- **Event log** — every mutation logged from day one (analytics UI ships in v1.3)

## Schema is future-proofed

Tables for habits, points ledger, rewards, badges, focus sessions, and task series already
exist at version 1, so v1.1–v1.3 need data, not migrations. `Migrations.ALL` is empty and
`AppDatabase` deliberately has **no** `fallbackToDestructiveMigration()` — a missing
migration throws rather than silently wiping data.

Pure logic that v1.1 needs is already written and tested:
`RecurrenceEngine` (all four PRD patterns incl. month-end/leap-year clamping),
`PointRules` (full point table), `HabitStreakRules` (streaks, weekly freeze).

## Verification status

**The project has never been compiled.** This machine has no JDK, no Gradle, and no
Android SDK, so `./gradlew assembleDebug` has not been run.

What *was* verified, in two passes:

**Logic** — the pure algorithmic cores were ported to Python and their test assertions
executed. 59 assertions across `RecurrenceEngine`, `DateUtils`, `PointRules`,
`HabitStreakRules`, and `SearchQueryBuilder` pass. That found three real bugs, now fixed:

1. `DateUtils.saturdayStartDayIndex` used `(calDay + 1) % 7`, rotating the entire week by
   one day — every habit schedule and weekday recurrence was off by a day. Now `calDay % 7`.
2. `PointRules.overduePenaltyFor` used `max(penalty, -1)`, which capped penalty severity so
   a High task lost 1 point instead of 3, making the penalty table meaningless. The PRD's
   "minimum -1" is a magnitude floor; now `-max(round(value * 0.25), 1)`.
3. `RecurrenceEngine` "last day of month" recurred never advanced out of January.

**Static integrity** — an automated audit checks every `R.*` resource reference, every
DAO method call against its interface, every own-package import against declared symbols,
and manifest components and Hilt wiring. Current state: 0 missing imports, 0 missing
resources, 0 unused imports, 0 duplicate imports, all braces/parens balanced. This pass
found and fixed six defects:

4. `TaskManagerNavHost` imported `TagListScreen`, which does not exist (stale import).
5. `BackupScreen` was used in the nav graph but never imported.
6. `R.string.nav_tasks` was referenced by `SettingsScreen` but declared in no locale.
7. `TodayViewModel.snackbarHostState` was a `MutableStateFlow<SnackbarHostState?>` where a
   `SnackbarHostState` was required, and the Undo action was never wired to the screen.
8. `BackupViewModel.chooseFolder()` was an empty function with no SAF launcher anywhere,
   so the backup folder could never actually be chosen; the replace-all import also ran
   with no confirmation. Both now use a real `OpenDocumentTree` launcher and an
   `AlertDialog` gate.
9. `WorkScheduler` was defined but never called, so Day Close and trash purge were never
   enqueued — penalties, streak evaluation, and automatic backup would silently never run.
   It is now invoked from `TaskManagerApp.onCreate`, along with a reminder reschedule.

Compilation errors in the Compose/UI layer are still possible on first build: no static
check covers composable parameter names or overload resolution. Expect to fix those once.
