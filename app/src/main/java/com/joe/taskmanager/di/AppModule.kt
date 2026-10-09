
package com.joe.taskmanager.di

import android.content.Context
import com.joe.taskmanager.data.local.AppDatabase
import com.joe.taskmanager.data.local.dao.EventDao
import com.joe.taskmanager.data.local.dao.FocusDao
import com.joe.taskmanager.data.local.dao.FolderDao
import com.joe.taskmanager.data.local.dao.GamificationDao
import com.joe.taskmanager.data.local.dao.HabitDao
import com.joe.taskmanager.data.local.dao.ReminderDao
import com.joe.taskmanager.data.local.dao.SearchIndexDao
import com.joe.taskmanager.data.local.dao.SearchableTaskDao
import com.joe.taskmanager.data.local.dao.TaskIndexQueriesDao
import com.joe.taskmanager.data.local.dao.SeriesDao
import com.joe.taskmanager.data.local.dao.SubtaskDao
import com.joe.taskmanager.data.local.dao.TagDao
import com.joe.taskmanager.data.local.dao.TaskDao
import com.joe.taskmanager.data.local.dao.TaskListDao
import com.joe.taskmanager.notification.NotificationChannels
import com.joe.taskmanager.notification.ReminderScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase = AppDatabase.get(context)

    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
    @Provides fun provideFolderDao(db: AppDatabase): FolderDao = db.folderDao()
    @Provides fun provideTaskListDao(db: AppDatabase): TaskListDao = db.taskListDao()
    @Provides fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
    @Provides fun provideSubtaskDao(db: AppDatabase): SubtaskDao = db.subtaskDao()
    @Provides fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()
    @Provides fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()
    @Provides fun provideGamificationDao(db: AppDatabase): GamificationDao = db.gamificationDao()
    @Provides fun provideHabitDao(db: AppDatabase): HabitDao = db.habitDao()
    @Provides fun provideSeriesDao(db: AppDatabase): SeriesDao = db.seriesDao()
    @Provides fun provideFocusDao(db: AppDatabase): FocusDao = db.focusDao()
    @Provides fun provideSearchableTaskDao(db: AppDatabase): SearchableTaskDao = db.searchableTaskDao()
    @Provides fun provideSearchIndexDao(db: AppDatabase): SearchIndexDao = db.searchIndexDao()
    @Provides fun provideTaskIndexQueriesDao(db: AppDatabase): TaskIndexQueriesDao = db.taskIndexQueriesDao()

    @Provides
    @Singleton
    fun provideNotificationChannels(@ApplicationContext context: Context) = NotificationChannels(context)

    @Provides
    @Singleton
    fun provideReminderScheduler(
        @ApplicationContext context: Context,
        channels: NotificationChannels
    ) = ReminderScheduler(context, channels)
}
