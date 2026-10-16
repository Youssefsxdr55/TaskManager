package com.joe.taskmanager.ui.screens.tags
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.dao.TagDao
import com.joe.taskmanager.data.local.entity.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagDao: TagDao
) : ViewModel() {
    val tags: StateFlow<List<Tag>> = tagDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun save(tag: Tag?, name: String, color: Int) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            val duplicate = tagDao.findByName(cleanName)
            if (duplicate != null && duplicate.id != tag?.id) return@launch
            if (tag == null) tagDao.insert(Tag(name = cleanName, color = color))
            else tagDao.update(tag.copy(name = cleanName, color = color))
        }
    }
    fun delete(tag: Tag) {
        viewModelScope.launch { tagDao.delete(tag) }
    }
}
