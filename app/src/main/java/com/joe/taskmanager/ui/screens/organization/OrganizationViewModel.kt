package com.joe.taskmanager.ui.screens.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.dao.FolderDao
import com.joe.taskmanager.data.local.dao.TaskListDao
import com.joe.taskmanager.data.local.entity.Folder
import com.joe.taskmanager.data.local.entity.TaskList
import com.joe.taskmanager.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrganizationState(val folders: List<Folder> = emptyList(), val lists: List<TaskList> = emptyList())

@HiltViewModel
class OrganizationViewModel @Inject constructor(
    private val folderDao: FolderDao,
    private val listDao: TaskListDao,
    private val repository: TaskRepository
) : ViewModel() {
    val state: StateFlow<OrganizationState> = combine(folderDao.observeAll(), listDao.observeAll()) { folders, lists ->
        OrganizationState(folders, lists)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrganizationState())

    fun addFolder(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { folderDao.insert(Folder(name = clean, color = 0xFF8B5E3C.toInt())) }
    }

    fun addList(name: String, folderId: Long? = null, parentListId: Long? = null) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            if (repository.validateListDepth(parentListId)) {
                listDao.insert(
                    TaskList(
                        name = clean,
                        color = 0xFF8B5E3C.toInt(),
                        folderId = folderId,
                        parentListId = parentListId
                    )
                )
            }
        }
    }

    fun deleteList(listId: Long, deleteTasks: Boolean) {
        viewModelScope.launch {
            val lists = state.value.lists
            val descendants = descendantsOf(listId, lists)
            repository.clearListContents(descendants, deleteTasks)
            listDao.getById(listId)?.let { listDao.delete(it) }
        }
    }

    fun deleteFolder(folderId: Long, deleteTasks: Boolean) {
        viewModelScope.launch {
            val lists = state.value.lists
            val roots = lists.filter { it.folderId == folderId && it.parentListId == null }
            val ids = roots.flatMap { descendantsOf(it.id, lists) }
            repository.clearListContents(ids, deleteTasks)
            roots.forEach { listDao.delete(it) }
            folderDao.getById(folderId)?.let { folderDao.delete(it) }
        }
    }

    private fun descendantsOf(rootId: Long, lists: List<TaskList>): List<Long> {
        val children = lists.filter { it.parentListId == rootId }
        return listOf(rootId) + children.flatMap { descendantsOf(it.id, lists) }
    }

}
