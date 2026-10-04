
package com.joe.taskmanager.reminder

import com.joe.taskmanager.data.local.entity.Reminder
import com.joe.taskmanager.data.local.entity.ReminderType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PRD 7.3: "A date with no time produces no reminder unless a time is explicitly
 * set." These cases decide whether an alarm row is valid at all.
 */
class ReminderValidityTest {

    @Test
    fun `at due time needs a scheduled instant`() {
        assertTrue(
            Reminder(taskId = 1, type = ReminderType.AT_DUE_TIME, scheduledAt = 100L).isValid
        )
        assertFalse(
            Reminder(taskId = 1, type = ReminderType.AT_DUE_TIME, scheduledAt = null).isValid
        )
    }

    @Test
    fun `before needs both an offset and a scheduled instant`() {
        assertTrue(
            Reminder(
                taskId = 1, type = ReminderType.BEFORE,
                offsetMinutes = 30, scheduledAt = 100L
            ).isValid
        )
        assertFalse(
            Reminder(
                taskId = 1, type = ReminderType.BEFORE,
                offsetMinutes = 30, scheduledAt = null
            ).isValid
        )
        assertFalse(
            Reminder(
                taskId = 1, type = ReminderType.BEFORE,
                offsetMinutes = null, scheduledAt = 100L
            ).isValid
        )
    }

    @Test
    fun `absolute needs an absolute time`() {
        assertTrue(
            Reminder(taskId = 1, type = ReminderType.ABSOLUTE, absoluteTime = 50L).isValid
        )
        assertFalse(
            Reminder(taskId = 1, type = ReminderType.ABSOLUTE, absoluteTime = null).isValid
        )
    }
}
