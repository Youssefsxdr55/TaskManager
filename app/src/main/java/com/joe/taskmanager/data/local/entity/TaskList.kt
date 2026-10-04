
package com.joe.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PRD 9 Organization. Three levels maximum: Folder > List > Sublist.
 * Enforced in code by TaskRepository.validateListDepth(); SQLite cannot express
 * "depth <= 3" as a constraint.
 */
@Entity(
    tableName = "task_lists",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TaskList::class,
            parentColumns = ["id"],
            childColumns = ["parentListId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folderId"), Index("parentListId")]
)
data class TaskList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val folderId: Long? = null,
    val parentListId: Long? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false
) {
    val isSublist: Boolean get() = parentListId != null
    val isTopLevelList: Boolean get() = folderId != null && parentListId == null
    val isRootList: Boolean get() = folderId == null && parentListId == null
}
