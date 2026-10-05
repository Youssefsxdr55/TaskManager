# Build Failure Report — TaskManager v1.0

**Repo:** https://github.com/Youssefsxdr55/TaskManager
**Failing run:** 37246497988
**Failing task:** `:app:kspDebugKotlin`
**Date:** 2026-10-05

---

## Current error

```
e: [ksp] ModuleProcessingStep was unable to process 'com.joe.taskmanager.di.AppModule'
   because 'error.NonExistentClass' could not be resolved.

e: [ksp] BindingMethodProcessingStep was unable to process
   'provideDatabase(android.content.Context)' because 'error.NonExistentClass'
   could not be resolved.

Dependency trace:
    => element (OBJECT): com.joe.taskmanager.di.AppModule
    => element (METHOD): provideSearchableTaskDao(com.joe.taskmanager.data.local.AppDatabase)
    => type (ERROR return type): error.NonExistentClass
```

All 17 Hilt providers in `AppModule` fail the same way.

## What it means

`provideSearchableTaskDao` is the **first** provider the processor reaches and
it fails first. Room never generated `AppDatabase_Impl` for that DAO's return
type, so Dagger records an ERROR return type, and every downstream provider
(`provideTaskDao`, `provideFolderDao`, ... `provideTaskIndexQueriesDao`) that
depends on `AppDatabase` inherits the failure.

This is a **single root cause with 17 symptoms**, not 17 separate bugs.

## Most likely cause

`SearchableTaskDao.search()` returns `Flow<List<SearchResultRow>>`, where
`SearchResultRow` is a `data class` declared in the *same file* as the DAO:

```kotlin
@Dao
interface SearchableTaskDao {
    @Query("""
        SELECT t.id AS id, t.title AS title, ...
        FROM searchable_task
        JOIN tasks t ON t.id = searchable_task.rowid
        WHERE searchable_task MATCH :match
        ORDER BY bm25(searchable_task)
    """)
    fun search(match: String, limit: Int): Flow<List<SearchResultRow>>
}

data class SearchResultRow(          // <-- same file as the @Dao interface
    val id: Long,
    val title: String,
    val status: String,
    ...
)
```

Room requires a POJO returned from a `@Query` to be resolvable when the DAO
interface is processed. A result class declared alongside the DAO, in a file
named after the DAO, can fail this check and abort generation for the whole
database. The `error.NonExistentClass` is Room's way of saying "I could not
build a type here".

### Candidate fixes, in order of likelihood

**A. Move `SearchResultRow` to its own file.**
Smallest change, addresses the most likely cause. Create
`data/local/SearchResultRow.kt` containing only the data class, and delete the
declaration from `SearchableTaskDao.kt`.

**B. Return `@Entity`-backed rows instead of a projection.**
Add `@SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)` and return the actual
`Task` entity from the query, doing the join filtering in Kotlin.

**C. Simplify the query.**
The `bm25(searchable_task)` ordering on a standalone FTS4 table joined to
`tasks` is the most complex part. Try ordering by `searchable_task.rowid`
instead, which sidesteps FTS-specific functions entirely.

---

## Important: this is NOT a code-logic problem

Everything below was already found and fixed. Room's own schema validation is
now silent, meaning the entities, foreign keys, indices and FTS setup all
compile cleanly:

- Missing Gradle wrapper
- `gradlew` not executable in git's index
- `java.util.Properties` unresolved in Kotlin DSL
- Six XML files with a blank line before `<?xml`
- Manifest `tools:replace` conflict with WorkManager
- **Three entity files missing `import androidx.room.ForeignKey`**
- `Reward` had a self-referencing foreign key to a nonexistent `rewardId`
- FTS4 external-content entity misconfigured
- `android:windowTurnScreenOn` is not a real Android attribute
- `androidx.room` plugin applied against Room 2.6.1

Two logic bugs worth remembering were also fixed earlier, both found by
executing the algorithms rather than reading them:

- `DateUtils.saturdayStartDayIndex` used `(calDay + 1) % 7`, rotating the whole
  week by one day, so every habit schedule and weekday recurrence was off by a day.
- `PointRules.overduePenaltyFor` used `max(penalty, -1)`, which capped penalty
  severity: a High-priority task lost 1 point instead of 3.

---

## Current dependency versions

| Library | Version |
|---|---|
| Kotlin | 2.0.21 |
| AGP | 8.7.2 |
| KSP | 2.0.21-1.0.28 |
| Room | 2.7.1 |
| Hilt/Dagger | 2.54 |
| Gradle | 8.9 |
| compileSdk / minSdk | 35 / 26 |

## Recommended next step

Open the project in Android Studio and let Gradle sync. That installs the JDK
and Android SDK (neither exists on this machine yet) and surfaces the error
interactively, which is much faster than iterating through CI.

If staying on CI, apply **fix A** above first — it is a one-file change and the
dependency trace points straight at it.