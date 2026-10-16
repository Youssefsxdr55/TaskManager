package com.joe.taskmanager.ui.screens.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.gamification.PointsEngine
import com.joe.taskmanager.data.local.dao.GamificationDao
import com.joe.taskmanager.data.local.entity.Reward
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val dao: GamificationDao,
    private val engine: PointsEngine
) : ViewModel() {
    val rewards: StateFlow<List<Reward>> = dao.observeRewards().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val redemptions = dao.observeRedemptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val balance: StateFlow<Int> = dao.observeBalance().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val badges = dao.observeUnlockedBadges().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val earned: StateFlow<Int> = dao.observeLifetimeEarned().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun save(old: Reward?, name: String, emoji: String, cost: Int, note: String) {
        if (name.isBlank() || cost <= 0) return
        viewModelScope.launch {
            val item = Reward(id = old?.id ?: 0, name = name.trim(), emoji = emoji.trim().ifEmpty { null },
                cost = cost, note = note.trim().ifEmpty { null })
            if (old == null) dao.insertReward(item) else dao.updateReward(item)
        }
    }
    fun archive(id: Long) { viewModelScope.launch { dao.archiveReward(id, true) } }
    fun redeem(item: Reward) { viewModelScope.launch { engine.redeemReward(item.id, item.name, item.cost) } }
}
