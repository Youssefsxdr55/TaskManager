
package com.joe.taskmanager.data.repository

import com.joe.taskmanager.data.local.dao.EventDao
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.EventType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PRD 1.3 principle 5: event logging ships in v1.0 even though analytics screens
 * ship in v1.3. Every mutation goes through here so nothing is missed.
 *
 * Logging is deliberately best-effort and never allowed to fail the user's action:
 * a failed analytics write must not roll back a completed task.
 */
@Singleton
class EventLogger @Inject constructor(private val eventDao: EventDao) {

    suspend fun log(
        type: EventType,
        entityType: String,
        entityId: Long?,
        payload: Map<String, Any?> = emptyMap()
    ) {
        runCatching {
            eventDao.insert(
                Event(
                    type = type,
                    entityType = entityType,
                    entityId = entityId,
                    payloadJson = payload.toJson()
                )
            )
        }
    }

    private fun Map<String, Any?>.toJson(): String =
        entries.joinToString(",", "{", "}") { (k, v) ->
            val value = when (v) {
                null -> "null"
                is Number, is Boolean -> v.toString()
                else -> "\"${v.toString().replace("\"", "\\\"")}\""
            }
            "\"$k\":$value"
        }
}
