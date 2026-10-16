
package com.joe.taskmanager.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.SearchQueryBuilder
import com.joe.taskmanager.data.local.dao.SearchableTaskDao
import com.joe.taskmanager.data.local.dao.TagDao
import com.joe.taskmanager.data.local.dao.TaskListDao
import com.joe.taskmanager.data.local.entity.Tag
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.local.entity.TaskList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SearchFilters(
    val listId: Long? = null,
    val tagId: Long? = null,
    val priority: String? = null,
    val status: String? = "OPEN",
    val dueFrom: Long? = null,
    val dueTo: Long? = null
)

data class SearchUiState(
    val query: String = "",
    val results: List<Task> = emptyList(),
    val lists: List<TaskList> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val filters: SearchFilters = SearchFilters()
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchableTaskDao: SearchableTaskDao,
    private val listDao: TaskListDao,
    private val tagDao: TagDao
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(SearchFilters())
    private val results = combine(query.debounce(200), filters) { raw, active -> raw to active }
        .flatMapLatest { (raw, active) ->
            val match = SearchQueryBuilder.build(raw)
            if (match.isEmpty()) {
                if (active == SearchFilters()) flowOf(emptyList()) else searchableTaskDao.filter(
                    active.listId, active.tagId, active.priority, active.status,
                    active.dueFrom, active.dueTo, MAX_RESULTS
                )
            } else searchableTaskDao.search(
                match, active.listId, active.tagId, active.priority, active.status,
                active.dueFrom, active.dueTo, MAX_RESULTS
            )
        }
    val uiState: StateFlow<SearchUiState> = combine(
        query, filters, results, listDao.observeAll(), tagDao.observeAll()
    ) { q, active, matches, lists, tags -> SearchUiState(q, matches, lists, tags, active) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clear() { query.value = "" }

    fun setList(id: Long?) = filters.update { it.copy(listId = id) }
    fun setTag(id: Long?) = filters.update { it.copy(tagId = id) }
    fun setPriority(priority: String?) = filters.update { it.copy(priority = priority) }
    fun setStatus(status: String?) = filters.update { it.copy(status = status) }
    fun setDateRange(from: Long?, to: Long?) = filters.update { it.copy(dueFrom = from, dueTo = to) }
    fun clearFilters() { filters.value = SearchFilters() }

    companion object {
        const val MAX_RESULTS = 100
    }
}
