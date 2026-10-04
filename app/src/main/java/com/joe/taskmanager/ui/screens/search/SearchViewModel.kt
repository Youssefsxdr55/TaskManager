
package com.joe.taskmanager.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.SearchQueryBuilder
import com.joe.taskmanager.data.local.SearchResultRow
import com.joe.taskmanager.data.local.SearchableTaskDao
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
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResultRow> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchableTaskDao: SearchableTaskDao
) : ViewModel() {

    private val query = MutableStateFlow("")
    val uiState: StateFlow<SearchUiState> = query
        // Debounced so typing does not run a query per keystroke.
        .debounce(200)
        .flatMapLatest { raw ->
            val match = SearchQueryBuilder.build(raw)
            if (match.isEmpty()) flowOf(emptyList()) else searchableTaskDao.search(match, MAX_RESULTS)
        }
        .let { resultsFlow ->
            kotlinx.coroutines.flow.combine(query, resultsFlow) { q, r -> SearchUiState(q, r) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clear() {
        query.value = ""
    }

    companion object {
        const val MAX_RESULTS = 100
    }
}
