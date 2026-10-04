
package com.joe.taskmanager.data.local

import androidx.room.TypeConverter
import com.joe.taskmanager.data.local.entity.Habit
import com.joe.taskmanager.data.local.entity.Priority
import com.joe.taskmanager.data.local.entity.ReminderType
import com.joe.taskmanager.data.local.entity.TaskStatus

/**
 * Room cannot store enums directly. All of these are stored as their stable
 * ordinal name so a future reorder of the enum cannot silently reinterpret
 * existing rows.
 */
class Converters {
    @TypeConverter fun priorityToString(v: Priority): String = v.name
    @TypeConverter fun stringToPriority(v: String): Priority = Priority.valueOf(v)

    @TypeConverter fun taskStatusToString(v: TaskStatus): String = v.name
    @TypeConverter fun stringToTaskStatus(v: String): TaskStatus = TaskStatus.valueOf(v)

    @TypeConverter fun reminderTypeToString(v: ReminderType): String = v.name
    @TypeConverter fun stringToReminderType(v: String): ReminderType = ReminderType.valueOf(v)

    @TypeConverter fun habitTypeToString(v: Habit.HabitType): String = v.name
    @TypeConverter fun stringToHabitType(v: String): Habit.HabitType = Habit.HabitType.valueOf(v)
}
