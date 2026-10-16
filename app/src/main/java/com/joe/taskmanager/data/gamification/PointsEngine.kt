
package com.joe.taskmanager.data.gamification

import androidx.room.withTransaction
import com.joe.taskmanager.data.local.AppDatabase
import com.joe.taskmanager.data.local.dao.FocusDao
import com.joe.taskmanager.data.local.dao.GamificationDao
import com.joe.taskmanager.data.local.dao.HabitDao
import com.joe.taskmanager.data.local.dao.TaskDao
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.EventType
import com.joe.taskmanager.data.local.entity.BadgeUnlock
import com.joe.taskmanager.data.local.entity.Habit
import com.joe.taskmanager.data.local.entity.HabitLog
import com.joe.taskmanager.data.local.entity.PointsLedgerEntry
import com.joe.taskmanager.data.local.entity.Priority
import com.joe.taskmanager.data.recurrence.RecurrenceMaterializer
import com.joe.taskmanager.gamification.completionDedupeKey
import com.joe.taskmanager.util.DateUtils
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

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
    private val recurrenceMaterializer: RecurrenceMaterializer,
    private val habitDao: HabitDao,
    private val database: AppDatabase,
    private val focusDao: FocusDao
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
            unlockTaskBadges()
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
        val snapshotExists = taskDao.dayBalanceSnapshot(dayStart) != null
        val balanceBefore = gamificationDao.currentBalance()
        applyOverduePenalties(dayStart)
        val closedDay = DateUtils.plusDays(dayStart, -1)
        applyHabitDayClose(closedDay)
        checkPerfectDay(closedDay)
        recurrenceMaterializer.materialize(dayStart)
        if (!snapshotExists) {
            val balanceAfter = gamificationDao.currentBalance()
            val dailyDelta = balanceAfter - balanceBefore
            val previous = taskDao.recentDayBalanceSnapshots(dayStart, 2)
            val wasNegativeForTwoDays = previous.size == 2 && previous.all {
                runCatching { JSONObject(it.payloadJson).getInt("balance") < 0 }.getOrDefault(false)
            }
            if (dailyDelta > 0 && wasNegativeForTwoDays) unlockBadge("comeback")
            taskDao.logEvent(Event(type = EventType.DAY_BALANCE_SNAPSHOT, entityType = "day-close", entityId = dayStart,
                payloadJson = """{"balance":$balanceAfter,"delta":$dailyDelta}"""))
        }
    }

    private suspend fun checkPerfectDay(dayStart: Long) {
        val dayEnd = DateUtils.endOfDay(dayStart)
        val due = taskDao.countDueBetween(dayStart, dayEnd)
        if (due > 0 && taskDao.countCompletedDueBetween(dayStart, dayEnd) == due) unlockBadge("perfect_day")
    }

    private suspend fun applyHabitDayClose(day: Long) {
        val habits = habitDao.activeHabits()
        for (habit in habits) {
            if (!isScheduled(habit, day)) continue
            val existing = habitDao.logFor(habit.id, day)
            if (existing?.completed == true) {
                awardHabitStreakMilestone(habit, day)
                continue
            }
            if (existing?.freezeUsed == true) continue
            val weekStart = DateUtils.startOfWeek(day)
            val weekLogs = habitDao.logsBetween(habit.id, weekStart, DateUtils.endOfDay(day))
            val freezeAvailable = weekLogs.none { it.freezeUsed }
            val wasProtected = freezeAvailable
            val missed = existing ?: HabitLog(
                habitId = habit.id, date = day, value = 0.0, completed = false,
                countsForStreak = wasProtected, freezeUsed = wasProtected
            )
            if (existing == null) habitDao.upsertLog(missed)
            else habitDao.updateLog(habit.id, day, 0.0, false, wasProtected, wasProtected)
            if (!wasProtected) {
                taskDao.logEvent(Event(type = EventType.HABIT_MISSED, entityType = "habit", entityId = habit.id,
                    payloadJson = """{"date":$day}"""))
                appendHabitPenalty(habit.id, day, MISSED_HABIT_DAY, "missed")
                if (priorStreak(habit, day) >= 3) appendHabitPenalty(habit.id, day, -STREAK_BREAK_EXTRA, "streak-break")
            }
        }
    }

    private suspend fun awardHabitStreakMilestone(habit: Habit, day: Long) {
        val streak = priorStreak(habit, DateUtils.plusDays(day, 1))
        val milestone = when (streak) { 7 -> STREAK_7; 30 -> STREAK_30; 100 -> STREAK_100; else -> 0 }
        if (milestone == 0) return
        if (streak >= 7) unlockBadge("habit_7_day")
        if (streak >= 30) unlockBadge("habit_30_day")
        val ledgerId = gamificationDao.append(PointsLedgerEntry(delta = milestone, reason = "Habit streak $streak",
            entityType = "habit", entityId = habit.id, dedupeKey = "habit-streak:${habit.id}:$day:$streak"))
        if (ledgerId != -1L) taskDao.logEvent(Event(type = EventType.POINTS_AWARDED, entityType = "habit", entityId = habit.id,
            payloadJson = """{"points":$milestone,"streak":$streak,"date":$day}"""))
    }

    private suspend fun priorStreak(habit: Habit, before: Long): Int {
        var day = DateUtils.plusDays(before, -1)
        var streak = 0
        var inspected = 0
        while (inspected < 370) {
            if (isScheduled(habit, day)) {
                val log = habitDao.logFor(habit.id, day)
                if (log?.countsForStreak != true) break
                streak++
            }
            day = DateUtils.plusDays(day, -1)
            inspected++
        }
        return streak
    }

    private fun isScheduled(habit: Habit, day: Long): Boolean =
        (habit.scheduleDays and (1 shl DateUtils.saturdayStartDayIndex(day))) != 0

    private suspend fun unlockTaskBadges() {
        val completed = gamificationDao.taskCompletionAwardCount()
        if (completed >= 1) unlockBadge("first_task")
        if (completed >= 10) unlockBadge("tasks_10")
        if (completed >= 100) unlockBadge("tasks_100")
        if (completed >= 1000) unlockBadge("tasks_1000")
    }

    private suspend fun unlockBadge(code: String) {
        if (gamificationDao.unlockBadge(BadgeUnlock(code)) != -1L) {
            taskDao.logEvent(Event(type = EventType.BADGE_UNLOCKED, entityType = "badge", entityId = null,
                payloadJson = """{"code":"$code"}"""))
        }
    }

    private suspend fun appendHabitPenalty(habitId: Long, day: Long, delta: Int, suffix: String) {
        val id = gamificationDao.append(PointsLedgerEntry(delta = delta, reason = "Habit $suffix", entityType = "habit",
            entityId = habitId, dedupeKey = "habit:$suffix:$habitId:$day"))
        if (id != -1L) taskDao.logEvent(Event(type = EventType.POINTS_PENALTY_APPLIED, entityType = "habit",
            entityId = habitId, payloadJson = """{"date":$day,"delta":$delta,"reason":"$suffix"}"""))
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

    suspend fun redeemReward(rewardId: Long, name: String, cost: Int): Boolean = database.withTransaction {
        if (cost <= 0 || gamificationDao.currentBalance() < cost) return@withTransaction false
        val reward = gamificationDao.rewardById(rewardId) ?: return@withTransaction false
        if (reward.archived || reward.cost != cost) return@withTransaction false
        val ledgerId = gamificationDao.append(
            PointsLedgerEntry(
                delta = -cost,
                reason = "Redeemed: $name",
                entityType = "reward",
                entityId = rewardId,
                dedupeKey = "redeem:$rewardId:${System.currentTimeMillis()}"
            )
        )
        if (ledgerId == -1L) return@withTransaction false
        gamificationDao.insertRedemption(
            com.joe.taskmanager.data.local.entity.RewardRedemption(
                rewardId = rewardId, cost = cost, ledgerId = ledgerId
            )
        ) != -1L
    }

    suspend fun checkFocusBadges() {
        val minutes = focusDao.lifetimeCompletedMinutes()
        if (minutes >= 60) unlockBadge("focus_hour")
        if (minutes >= 600) unlockBadge("focus_10_hours")
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

        val id = gamificationDao.append(
            PointsLedgerEntry(
                delta = FOCUS_SESSION,
                reason = "Focus session",
                entityType = "focus",
                entityId = null,
                dedupeKey = "focus:$start:$already"
            )
        )
        if (id != -1L) taskDao.logEvent(Event(type = EventType.POINTS_AWARDED, entityType = "focus", entityId = null,
            payloadJson = """{"points":$FOCUS_SESSION}"""))
        checkFocusBadges()
        return id != -1L
    }

}
