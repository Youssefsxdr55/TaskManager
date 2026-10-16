
package com.joe.taskmanager.ui.navigation
/**
 * PRD 6.1: bottom navigation shows Today and Upcoming only in v1.0; other tabs
 * appear as the features ship. Route strings are sealed so a typo cannot create
 * an unreachable screen.
 */
sealed class Route(val path: String) {
    data object Onboarding : Route("onboarding")
    data object Today : Route("today")
    data object Upcoming : Route("upcoming")
    data object Inbox : Route("inbox?inboxOnly=true")
    data object AllTasks : Route("all")
    data object Completed : Route("completed")
    data object Trash : Route("trash")
    data object Tags : Route("tags")
    data object Habits : Route("habits")
    data object Rewards : Route("rewards")
    data object Recurrence : Route("recurrence")
    data object Calendar : Route("calendar")
    data object Insights : Route("insights")
    data object Focus : Route("focus")
    data object Organization : Route("organization")
    data object Settings : Route("settings")
    data object Reliability : Route("reliability")
    data object Backup : Route("backup")
    data object Search : Route("search")
    data object AddTask : Route("task/edit?taskId={taskId}&listId={listId}&dueDate={dueDate}") {
        const val ARG_TASK_ID = "taskId"
        const val ARG_LIST_ID = "listId"
        const val ARG_DUE_DATE = "dueDate"
        fun create(taskId: Long? = null, listId: Long? = null, dueDate: Long? = null): String =
            "task/edit?taskId=${taskId ?: -1L}&listId=${listId ?: -1L}&dueDate=${dueDate ?: -1L}"
    }
    data object TaskDetail : Route("task/{taskId}") {
        const val ARG_TASK_ID = "taskId"
        fun create(taskId: Long): String = "task/$taskId"
    }
    data object TaskListView : Route("list/{listId}") {
        fun create(listId: Long): String = "list/$listId"
    }
    data object TagView : Route("tag/{tagId}") {
        fun create(tagId: Long): String = "tag/$tagId"
    }
}
/** Tabs rendered in the bottom bar for v1.0. */
val BOTTOM_TABS = listOf(Route.Today, Route.Upcoming)
