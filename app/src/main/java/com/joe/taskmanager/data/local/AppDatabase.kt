
package com.joe.taskmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.joe.taskmanager.data.local.dao.EventDao
import com.joe.taskmanager.data.local.dao.FocusDao
import com.joe.taskmanager.data.local.dao.FolderDao
import com.joe.taskmanager.data.local.dao.GamificationDao
import com.joe.taskmanager.data.local.dao.HabitDao
import com.joe.taskmanager.data.local.dao.ReminderDao
import com.joe.taskmanager.data.local.dao.SearchIndexDao
import com.joe.taskmanager.data.local.dao.TaskIndexQueriesDao
import com.joe.taskmanager.data.local.dao.SeriesDao
import com.joe.taskmanager.data.local.dao.SubtaskDao
import com.joe.taskmanager.data.local.dao.TagDao
import com.joe.taskmanager.data.local.dao.TaskDao
import com.joe.taskmanager.data.local.dao.TaskListDao
import com.joe.taskmanager.data.local.entity.BadgeUnlock
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.FocusSession
import com.joe.taskmanager.data.local.entity.Folder
import com.joe.taskmanager.data.local.entity.Habit
import com.joe.taskmanager.data.local.entity.HabitLog
import com.joe.taskmanager.data.local.entity.OccurrenceMarker
import com.joe.taskmanager.data.local.entity.PointsLedgerEntry
import com.joe.taskmanager.data.local.entity.Reminder
import com.joe.taskmanager.data.local.entity.Reward
import com.joe.taskmanager.data.local.entity.RewardRedemption
import com.joe.taskmanager.data.local.entity.Subtask
import com.joe.taskmanager.data.local.entity.Tag
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.local.entity.TaskList
import com.joe.taskmanager.data.local.entity.TaskSeries
import com.joe.taskmanager.data.local.entity.TaskTag

/**
 * PRD 9 / PRD 13: explicit migrations from v1, never destructive fallback.
 * version 1 is the initial schema. To change it later, add a MIGRATION_n_n+1
 * entry here AND an instrumented migration test (see app/schemas + tests).
 */
@Database(
    entities = [
        Folder::class,
        TaskList::class,
        Task::class,
        Subtask::class,
        Tag::class,
        TaskTag::class,
        Reminder::class,
        TaskSeries::class,
        OccurrenceMarker::class,
        Habit::class,
        HabitLog::class,
        FocusSession::class,
        PointsLedgerEntry::class,
        Reward::class,
        RewardRedemption::class,
        BadgeUnlock::class,
        Event::class,
        SearchableTask::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun folderDao(): FolderDao
    abstract fun taskListDao(): TaskListDao
    abstract fun tagDao(): TagDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun eventDao(): EventDao
    abstract fun gamificationDao(): GamificationDao
    abstract fun habitDao(): HabitDao
    abstract fun seriesDao(): SeriesDao
    abstract fun focusDao(): FocusDao
    abstract fun searchableTaskDao(): SearchableTaskDao
    abstract fun searchIndexDao(): SearchIndexDao
    abstract fun taskIndexQueriesDao(): TaskIndexQueriesDao

    companion object {
        private const val DB_NAME = "taskmanager.db"

        /**
         * No fallbackToDestructiveMigration() on purpose: a silent wipe is the
         * opposite of the "0 data loss incidents" success metric (PRD 2.3).
         * If a migration is missing, Room throws instead of destroying data.
         */
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // PRD 6.3: the app starts completely empty. The only
                        // built-in container is the implicit Inbox (listId = NULL).
                    }
                })
                .addMigrations(*Migrations.ALL)
                .build()
    }
}

/** Migration registry. Empty for v1; extend as the schema evolves. */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}
