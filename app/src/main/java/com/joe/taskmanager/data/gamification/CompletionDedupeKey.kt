package com.joe.taskmanager.gamification

/** Stable points-ledger key for a task or a specific recurring occurrence. */
fun completionDedupeKey(taskId: Long, occurrenceDate: Long?): String =
    if (occurrenceDate != null) "complete:$taskId:$occurrenceDate" else "complete:$taskId"
