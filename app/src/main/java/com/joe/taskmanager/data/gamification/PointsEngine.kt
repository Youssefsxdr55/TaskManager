
package com.joe.taskmanager.data.gamification

import com.joe.taskmanager.data.local.dao.GamificationDao
import com.joe.taskmanager.data.local.dao.TaskDao
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.EventType
import com.joe.taskmanager.data.local.entity.PointsLedgerEntry
import com.joe.taskmanager.data.local.entity.Priority
import com.joe.taskmanager.data.repository.TaskRepository
import com.joe.taskmanager.util.DateUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PRD 7.8 points, penalties, and levels.
 *
 * Every award and penalty is appended to the ledger with a UNIQUE dedupeKey, so
 * a rerun of the daily worker can never double-apply. Balance is the sum of the
 * ledger; nothing is ever edited in place.
 */
@Singleton
class PointsEngine @Inject constructor(
    private val gamificationDao: GamificationDao,
    private val taskDao: TaskDao,
    private val taskRepository: TaskRepository
) {
    companion object {
        /**
         * The point table and level curve live in [PointRules] so there is exactly
         * one place to change them (and one place to test them). These aliases keep
         * call sites readable without re-declaring the values.
         */
        const val TASK_HIGH = PointRules.TASK_HIGH
        const val TASK_MEDIUM = PointRules.TASK_MEDIUM
        const val TASK_LOW = PointRules.TASK_LOW
        const val TASK_NONE = PointRules.TASK_NONE

        const val STREAK_7 = PointRules.STREAK_7
        const val STREAK_30 = PointRules.STREAK_30
        const val STREAK_100 = PointRules.STREAK_100

        const val FOCUS_SESSION = PointRules.FOCUS_SESSION
        const val FOCUS_DAILY_CAP = PointRules.FOCUS_DAILY_CAP

        const val MISSED_HABIT_DAY = PointRules.MISSED_HABIT_DAY
        const val STREAK_BREAK_EXTRA = PointRules.STREAK_BREAK_EXTRA

        /** Level n requires 25 * n * (n-1) cumulative earned points. */
        fun pointsRequiredForLevel(level: Int): Int = PointRules.pointsRequiredForLevel(level)

        fun levelFor(lifetimeEarned: Int): Int = PointRules.levelFor(lifetimeEarned)
    }

    /** Points a task completion is worth, by priority. */
    fun completionValue(priority: Priority): Int = PointRules.completionValue(priority)

    /**
     * Award for completing a task, once per task (per occurrence for recurring
     * tasks). Un-completing and re-completing does NOT award again: the dedupeKey
     * is derived from the task identity, not from the completion count.
     */
    suspend fun awardTaskCompletion(taskId: Long, priority: Priority, occurrenceDate: Long? = null): Boolean {
        val key = completionDedupeKey(taskId, occurrenceDate)
        val id = gamificationDao.append(
            PointsLedgerEntry(
                delta = completionValue(priority),
                reason = "Task completed",
                entityType = "task",
                entityId = taskId,
                dedupeKey = key
            )
        )
        val applied = id != -1L
        if (applied) {
            taskDao.logEvent(
                Event(
                    type = EventType.POINTS_AWARDED,
                    entityType = "task",
                    entityId = taskId,
                    payloadJson = """{"points":${completionValue(priority)}}"""
                )
            )
        }
        return applied
    }

    /** PRD 7.8: completion value minus 25%, minimum -1, per day overdue. */
    fun overduePenaltyFor(priority: Priority): Int = PointRules.overduePenaltyFor(priority)

    /**
     * PRD 10 Day Close. Idempotent: each write carries a date-scoped dedupeKey, so
     * calling this twice for the same day changes nothing the second time.
     */
    suspend fun runDayClose(dayStart: Long = DateUtils.startOfToday()) {
        applyOverduePenalties(dayStart)
        // Habit evaluation, streak breaks, streak milestones, and occurrence
        // materialization land here in v1.1 (the tables already exist).
    }

    private suspend fun applyOverduePenalties(dayStart: Long) {
        val dayEnd = DateUtils.endOfDay(dayStart)
        val overdue = taskDao.overdueAsOf(dayStart, dayEnd)
        overdue.forEach { task ->
            val key = "overdue:${task.id}:${dayStart}"
            val id = gamificationDao.append(
                PointsLedgerEntry(
                    delta = overduePenaltyFor(task.priority),
                    reason = "Overdue",
                    entityType = "task",
                    entityId = task.id,
                    dedupeKey = key
                )
            )
            if (id != -1L) {
                taskDao.logEvent(
                    Event(
                        type = EventType.POINTS_PENALTY_APPLIED,
                        entityType = "task",
                        entityId = task.id,
                        payloadJson = """{"day":$dayStart}"""
                    )
                )
            }
        }
    }

    /**
     * PRD 7.8 escape hatch: writes a reset entry so the balance returns to zero
     * while the history stays intact and auditable.
     */
    suspend fun resetPoints(): Boolean {
        val balance = gamificationDao.currentBalance()
        if (balance == 0) return false
        val id = gamificationDao.append(
            PointsLedgerEntry(
                delta = -balance,
                reason = "Reset points",
                entityType = "reset",
                entityId = null,
                dedupeKey = "reset:${System.currentTimeMillis()}"
            )
        )
        return id != -1L
    }

    suspend fun redeemReward(rewardId: Long, name: String, cost: Int): Boolean {
        // PRD OQ-11: redemption requires balance >= cost.
        val balance = gamificationDao.currentBalance()
        if (balance < cost) return false

        val ledgerId = gamificationDao.append(
            PointsLedgerEntry(
                delta = -cost,
                reason = "Redeemed: $name",
                entityType = "reward",
                entityId = rewardId,
                dedupeKey = "redeem:$rewardId:${System.currentTimeMillis()}"
            )
        )
        if (ledgerId == -1L) return false

        val redemptionId = gamificationDao.insertRedemption(
            com.joe.taskmanager.data.local.entity.RewardRedemption(
                rewardId = rewardId, cost = cost, ledgerId = ledgerId
            )
        )
        return redemptionId != -1L
    }

    /**
     * Focus sessions cap at 8 per day so points cannot be farmed by running many
     * short sessions (PRD 13, points-farming risk).
     */
    suspend fun awardFocusSession(): Boolean {
        val start = DateUtils.startOfToday()
        val end = DateUtils.endOfToday()
        val already = gamificationDao.focusCountBetween(start, end)
        if (already >= FOCUS_DAILY_CAP) return false

        return gamificationDao.append(
            PointsLedgerEntry(
                delta = FOCUS_SESSION,
                reason = "Focus session",
                entityType = "focus",
                entityId = null,
                dedupeKey = "focus:$start:$already"
            )
        ) != -1L
    }

}

/** Occurrence-scoped dedupe key so each occurrence of a series is its own award. */
internal fun completionDedupeKey(taskId: Long, occurrenceDate: Long?): String =
    if (occurrenceDate != null) "complete:$taskId:$occurrenceDate" else "complete:$taskId"
