
package com.joe.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** PRD 9 Organization. */
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val sortOrder: Int = 0
)
