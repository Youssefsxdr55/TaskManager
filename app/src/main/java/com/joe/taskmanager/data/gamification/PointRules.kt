
package com.joe.taskmanager.data.gamification

import com.joe.taskmanager.data.local.entity.Priority
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The PRD 7.8 point table and level curve, with no dependencies.
 *
 * Separated from [PointsEngine] so the numbers can be unit tested without Room,
 * Hilt, or coroutines (PRD 11). These values are deliberately the single source
 * of truth for both awarding and penalties, which is what makes the table
 * auditable (PRD 1.3 principle 4: honest gamification).
 */
object PointRules {

    // --- Completion awards (PRD 7.8 table) ---
    const val TASK_HIGH = 10
    const val TASK_MEDIUM = 5
    const val TASK_LOW = 2
    const val TASK_NONE = 1

    // --- Habit ---
    const val HABIT_BOOLEAN = 3
    const val HABIT_NUMERIC_MAX = 3

    // --- Streak milestones ---
    const val STREAK_7 = 10
    const val STREAK_30 = 50
    const val STREAK_100 = 200

    // --- Focus ---
    const val FOCUS_SESSION = 2
    const val FOCUS_DAILY_CAP = 8

    // --- Penalties ---
    const val MISSED_HABIT_DAY = -2
    const val STREAK_BREAK_EXTRA = -5
    const val OVERDUE_RATE = 0.25
    const val MIN_OVERDUE_PENALTY_MAGNITUDE = 1

    fun completionValue(priority: Priority): Int = when (priority) {
        Priority.HIGH -> TASK_HIGH
        Priority.MEDIUM -> TASK_MEDIUM
        Priority.LOW -> TASK_LOW
        Priority.NONE -> TASK_NONE
    }

    /**
     * -25% of the completion value, with a floor on the MAGNITUDE of 1.
     *
     * "minimum -1" in the PRD means a penalty is never smaller than one point, so
     * the magnitude is floored, not capped: a HIGH task loses 3, not 1. Clamping
     * with max(penalty, -1) instead capped the severity and made the penalty
     * table meaningless.
     */
    fun overduePenaltyFor(priority: Priority): Int {
        val base = completionValue(priority)
        val magnitude = max((base * OVERDUE_RATE).roundToInt(), MIN_OVERDUE_PENALTY_MAGNITUDE)
        return -magnitude
    }

    /**
     * Numeric habit progress counts proportionally toward the cap (PRD 7.6, 7.8).
     */
    fun habitNumericAward(ratio: Double): Int =
        (HABIT_NUMERIC_MAX * ratio.coerceIn(0.0, 1.0)).roundToInt()

    /** Cumulative lifetime earned points required to reach a level. */
    fun pointsRequiredForLevel(level: Int): Int = 25 * level * (level - 1)

    /**
     * PRD OQ-5: level comes from lifetime EARNED points only, so penalties never
     * de-level the user.
     */
    fun levelFor(lifetimeEarned: Int): Int {
        var level = 1
        while (pointsRequiredForLevel(level + 1) <= lifetimeEarned) level++
        return level
    }
}
