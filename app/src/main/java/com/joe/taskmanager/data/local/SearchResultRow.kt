package com.joe.taskmanager.data.local

/**
 * Projection of a task that matched the FTS query.
 *
 * Deliberately in its own file. Room resolves a @Query's return type while
 * processing the @Dao interface; declaring this POJO inside SearchableTaskDao.kt
 * made the processor fail with error.NonExistentClass, which aborted generation
 * of AppDatabase_Impl and broke every Hilt binding in AppModule as a result.
 */
data class SearchResultRow(
    val id: Long,
    val title: String,
    val status: String,
    val dueDate: Long?,
    val hasTime: Boolean,
    val priority: String,
    val listId: Long?
)
