
package com.joe.taskmanager.gamification

import com.joe.taskmanager.gamification.completionDedupeKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * PRD 7.8: points are awarded once per task, per occurrence for recurring tasks.
 * The dedupe key is what enforces it, so its shape is part of the contract.
 */
class CompletionDedupeKeyTest {

    @Test
    fun `same task produces the same key`() {
        assertEquals(completionDedupeKey(42, null), completionDedupeKey(42, null))
    }

    @Test
    fun `different tasks produce different keys`() {
        assertNotEquals(completionDedupeKey(42, null), completionDedupeKey(43, null))
    }

    @Test
    fun `each occurrence of a series gets its own key`() {
        val a = completionDedupeKey(42, 1_000L)
        val b = completionDedupeKey(42, 2_000L)
        assertNotEquals(a, b)
    }

    @Test
    fun `an occurrence key differs from the plain task key`() {
        assertNotEquals(completionDedupeKey(42, null), completionDedupeKey(42, 1_000L))
    }

    @Test
    fun `key is prefixed so it cannot collide with other entry types`() {
        assertEquals(true, completionDedupeKey(1, null).startsWith("complete:"))
        assertNotEquals("overdue:1:0", completionDedupeKey(1, null))
    }
}
