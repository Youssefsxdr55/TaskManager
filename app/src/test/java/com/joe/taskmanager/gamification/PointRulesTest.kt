
package com.joe.taskmanager.gamification

import com.joe.taskmanager.data.gamification.PointRules
import com.joe.taskmanager.data.local.entity.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PRD 7.8 point values and the level curve. These are the numbers the owner will
 * actually feel, so they are pinned exactly. OQ-10 marks them tunable, which
 * means changing one should be a deliberate edit that this test then catches.
 */
class PointRulesTest {

    @Test
    fun `completion value by priority matches the PRD table`() {
        assertEquals(10, PointRules.completionValue(Priority.HIGH))
        assertEquals(5, PointRules.completionValue(Priority.MEDIUM))
        assertEquals(2, PointRules.completionValue(Priority.LOW))
        assertEquals(1, PointRules.completionValue(Priority.NONE))
    }

    @Test
    fun `overdue penalty is 25 percent of completion value with a magnitude floor of one`() {
        // PRD 7.8: -25% of the completion value, minimum -1. The floor applies to
        // the MAGNITUDE, so a HIGH task loses 3 points rather than 1.
        assertEquals(-3, PointRules.overduePenaltyFor(Priority.HIGH))   // 10 * .25 = 2.5 -> 3
        assertEquals(-1, PointRules.overduePenaltyFor(Priority.MEDIUM)) // 5 * .25 = 1.25 -> 1
        assertEquals(-1, PointRules.overduePenaltyFor(Priority.LOW))    // 2 * .25 = 0.5 -> 1
        assertEquals(-1, PointRules.overduePenaltyFor(Priority.NONE))   // 1 * .25 = 0.25 -> 1
    }

    @Test
    fun `a task with no priority still loses a whole point`() {
        // 0.25 rounds down to 0, which would make the penalty a no-op.
        assertEquals(-1, PointRules.overduePenaltyFor(Priority.NONE))
    }

    @Test
    fun `overdue penalty never exceeds the completion value`() {
        Priority.entries.forEach { p ->
            assertTrue(
                "penalty for $p must be at most the awarded value",
                Math.abs(PointRules.overduePenaltyFor(p)) <= PointRules.completionValue(p)
            )
        }
    }

    @Test
    fun `numeric habit progress counts proportionally and is capped`() {
        assertEquals(0, PointRules.habitNumericAward(0.0))
        assertEquals(2, PointRules.habitNumericAward(0.5))
        assertEquals(3, PointRules.habitNumericAward(1.0))
        assertEquals(3, PointRules.habitNumericAward(1.5))  // capped
        assertEquals(0, PointRules.habitNumericAward(-1.0)) // floored
    }

    @Test
    fun `level curve is 25 times n times n minus one`() {
        assertEquals(0, PointRules.pointsRequiredForLevel(1))
        assertEquals(50, PointRules.pointsRequiredForLevel(2))
        assertEquals(150, PointRules.pointsRequiredForLevel(3))
        assertEquals(300, PointRules.pointsRequiredForLevel(4))
    }

    @Test
    fun `level is derived from lifetime earned points`() {
        assertEquals(1, PointRules.levelFor(0))
        assertEquals(1, PointRules.levelFor(49))
        assertEquals(2, PointRules.levelFor(50))
        assertEquals(2, PointRules.levelFor(149))
        assertEquals(3, PointRules.levelFor(150))
        assertEquals(4, PointRules.levelFor(300))
    }

    /** PRD OQ-5: penalties must never reduce the level. */
    @Test
    fun `level never decreases as earned points grow`() {
        var last = 1
        for (earned in 0..2000 step 25) {
            val level = PointRules.levelFor(earned)
            assertTrue("level dropped at $earned earned points", level >= last)
            last = level
        }
    }

    @Test
    fun `focus daily cap is eight`() {
        assertEquals(8, PointRules.FOCUS_DAILY_CAP)
    }
}
