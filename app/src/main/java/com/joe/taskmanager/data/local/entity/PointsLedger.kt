
package com.joe.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PRD 7.8: append-only ledger. Balance = sum of deltas. Nothing is edited in
 * place. `dedupeKey` is UNIQUE so an award or penalty can be applied exactly once
 * no matter how often the daily worker reruns.
 */
@Entity(
    tableName = "points_ledger",
    indices = [Index("dedupeKey", unique = true), Index("createdAt"), Index("entityId")]
)
data class PointsLedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val delta: Int,
    val reason: String,
    val entityType: String,
    val entityId: Long?,
    val dedupeKey: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "rewards",
    foreignKeys = [
        ForeignKey(
            entity = Reward::class,
            parentColumns = ["id"],
            childColumns = ["rewardId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Reward(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String?,
    val cost: Int,
    val note: String? = null,
    val archived: Boolean = false
)

@Entity(
    tableName = "reward_redemptions",
    foreignKeys = [
        ForeignKey(
            entity = Reward::class,
            parentColumns = ["id"],
            childColumns = ["rewardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("rewardId")]
)
data class RewardRedemption(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rewardId: Long,
    val cost: Int,
    val redeemedAt: Long = System.currentTimeMillis(),
    val ledgerId: Long? = null
)

/** PRD 7.8 badges. Definitions live in code; unlocks are stored. */
@Entity(tableName = "badge_unlocks")
data class BadgeUnlock(
    @PrimaryKey val badgeCode: String,
    val unlockedAt: Long = System.currentTimeMillis()
)
