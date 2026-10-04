
package com.joe.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.joe.taskmanager.data.local.entity.BadgeUnlock
import com.joe.taskmanager.data.local.entity.PointsLedgerEntry
import com.joe.taskmanager.data.local.entity.Reward
import com.joe.taskmanager.data.local.entity.RewardRedemption
import kotlinx.coroutines.flow.Flow

@Dao
interface GamificationDao {

    // ---------- Append-only ledger (PRD 7.8) ----------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun append(entry: PointsLedgerEntry): Long

    /**
     * IGNORE above means a duplicate dedupeKey returns -1 instead of throwing.
     * Callers treat -1 as "already applied, skip" which is what makes the daily
     * worker safe to rerun (PRD 10).
     */
    @Query("SELECT COALESCE(SUM(delta), 0) FROM points_ledger")
    fun observeBalance(): Flow<Int>

    @Query("SELECT COALESCE(SUM(delta), 0) FROM points_ledger WHERE delta > 0")
    fun observeLifetimeEarned(): Flow<Int>

    @Query("SELECT COALESCE(SUM(delta), 0) FROM points_ledger WHERE delta > 0")
    suspend fun lifetimeEarned(): Int

    @Query("SELECT * FROM points_ledger ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentEntries(limit: Int): Flow<List<PointsLedgerEntry>>

    @Query("SELECT * FROM points_ledger WHERE entityType = :entityType AND entityId = :entityId ORDER BY createdAt DESC")
    fun observeForEntity(entityType: String, entityId: Long): Flow<List<PointsLedgerEntry>>

    @Query("SELECT COALESCE(SUM(delta), 0) FROM points_ledger WHERE createdAt >= :from AND createdAt < :to")
    fun observeSumBetween(from: Long, to: Long): Flow<Int>

    /** PRD 7.8 "Reset points": writes a reset entry, history stays intact. */
    @Query("SELECT COALESCE(SUM(delta), 0) FROM points_ledger")
    suspend fun currentBalance(): Int

    @Query("SELECT id FROM points_ledger WHERE dedupeKey = :key LIMIT 1")
    suspend fun findByDedupeKey(key: String): Long?

    /** PRD 7.8 / 13: counts rewarded focus sessions today to enforce the daily cap. */
    @Query(
        """
        SELECT COUNT(*) FROM points_ledger
        WHERE entityType = 'focus' AND createdAt >= :from AND createdAt < :to
        """
    )
    suspend fun focusCountBetween(from: Long, to: Long): Int

    // ---------- Rewards ----------

    @Insert suspend fun insertReward(reward: Reward): Long
    @Update suspend fun updateReward(reward: Reward)
    @Query("SELECT * FROM rewards WHERE archived = 0 ORDER BY cost ASC")
    fun observeRewards(): Flow<List<Reward>>
    @Query("SELECT * FROM rewards WHERE archived = 0 AND cost <= :balance ORDER BY cost ASC")
    fun observeRedeemable(balance: Int): Flow<List<Reward>>
    @Query("SELECT * FROM rewards WHERE id = :id")
    suspend fun rewardById(id: Long): Reward?
    @Query("UPDATE rewards SET archived = :archived WHERE id = :id")
    suspend fun archiveReward(id: Long, archived: Boolean)

    @Insert suspend fun insertRedemption(redemption: RewardRedemption): Long
    @Query("SELECT * FROM reward_redemptions ORDER BY redeemedAt DESC")
    fun observeRedemptions(): Flow<List<RewardRedemption>>
    @Query("UPDATE reward_redemptions SET ledgerId = :ledgerId WHERE id = :id")
    suspend fun linkRedemptionToLedger(id: Long, ledgerId: Long)

    // ---------- Badges ----------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlockBadge(unlock: BadgeUnlock): Long
    @Query("SELECT * FROM badge_unlocks ORDER BY unlockedAt DESC")
    fun observeUnlockedBadges(): Flow<List<BadgeUnlock>>
    @Query("SELECT COUNT(*) FROM badge_unlocks")
    suspend fun unlockedCount(): Int
}
