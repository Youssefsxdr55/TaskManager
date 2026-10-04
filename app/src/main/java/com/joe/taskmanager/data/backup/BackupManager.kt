
package com.joe.taskmanager.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.joe.taskmanager.data.local.AppDatabase
import com.joe.taskmanager.data.local.SearchIndexMaintainer
import com.joe.taskmanager.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PRD 7.13 Backup and Restore.
 *
 * The automatic backup writes into a folder the user picked through the Storage
 * Access Framework, with a persisted URI permission, so backups survive
 * uninstalling the app. Manual export produces the same JSON file and hands it
 * to the user via a share/create-document intent.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val searchIndex: SearchIndexMaintainer,
    private val settings: SettingsRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    /** Take a durable grant on the picked folder so it survives reboots. */
    fun persistPermission(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
    }

    /** Build the backup payload from the current database contents. */
    suspend fun buildBackup(): BackupFile {
        val db = database.openHelper.readableDatabase
        return BackupFile(
            schemaVersion = BackupFile.CURRENT_SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            tasks = db.rawQuery(
                "SELECT id,title,notes,listId,dueDate,hasTime,priority,status,completedAt," +
                    "seriesId,occurrenceDate,sortOrder,createdAt,updatedAt,deletedAt FROM tasks",
                null
            ).use { c -> buildList {
                while (c.moveToNext()) {
                    add(
                        TaskDto(
                            id = c.getLong(0), title = c.getString(1), notes = c.getString(2),
                            listId = c.getLongOrNull(3), dueDate = c.getLongOrNull(4),
                            hasTime = c.getInt(5) == 1, priority = c.getString(6),
                            status = c.getString(7), completedAt = c.getLongOrNull(8),
                            seriesId = c.getLongOrNull(9), occurrenceDate = c.getLongOrNull(10),
                            sortOrder = c.getInt(11), createdAt = c.getLong(12),
                            updatedAt = c.getLong(13), deletedAt = c.getLongOrNull(14)
                        )
                    )
                }
            } },
            lists = db.rawQuery(
                "SELECT id,name,color,folderId,parentListId,sortOrder,archived FROM task_lists", null
            ).use { c -> buildList {
                while (c.moveToNext()) {
                    add(
                        ListDto(
                            id = c.getLong(0), name = c.getString(1), color = c.getInt(2),
                            folderId = c.getLongOrNull(3), parentListId = c.getLongOrNull(4),
                            sortOrder = c.getInt(5), archived = c.getInt(6) == 1
                        )
                    )
                }
            } },
            folders = db.rawQuery("SELECT id,name,color,sortOrder FROM folders", null).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(FolderDto(c.getLong(0), c.getString(1), c.getInt(2), c.getInt(3)))
                    }
                }
            },
            tags = db.rawQuery("SELECT id,name,color FROM tags", null).use { c -> buildList {
                while (c.moveToNext()) add(TagDto(c.getLong(0), c.getString(1), c.getInt(2)))
            } },
            subtasks = db.rawQuery("SELECT id,taskId,title,done,sortOrder FROM subtasks", null).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(SubtaskDto(c.getLong(0), c.getLong(1), c.getString(2), c.getInt(3) == 1, c.getInt(4)))
                    }
                }
            },
            reminders = db.rawQuery(
                "SELECT id,taskId,type,offsetMinutes,absoluteTime,scheduledAt FROM reminders", null
            ).use { c -> buildList {
                while (c.moveToNext()) {
                    add(
                        ReminderDto(
                            id = c.getLong(0), taskId = c.getLong(1), type = c.getString(2),
                            offsetMinutes = c.getIntOrNull(3), absoluteTime = c.getLongOrNull(4),
                            scheduledAt = c.getLongOrNull(5)
                        )
                    )
                }
            } },
            events = db.rawQuery(
                "SELECT id,type,entityType,entityId,occurredAt,payloadJson FROM events", null
            ).use { c -> buildList {
                while (c.moveToNext()) {
                    add(
                        EventDto(
                            id = c.getLong(0), type = c.getString(1), entityType = c.getString(2),
                            entityId = c.getLongOrNull(3), occurredAt = c.getLong(4),
                            payloadJson = c.getString(5) ?: "{}"
                        )
                    )
                }
            } },
            ledger = db.rawQuery(
                "SELECT id,delta,reason,entityType,entityId,dedupeKey,createdAt FROM points_ledger", null
            ).use { c -> buildList {
                while (c.moveToNext()) {
                    add(
                        LedgerDto(
                            id = c.getLong(0), delta = c.getInt(1), reason = c.getString(2),
                            entityType = c.getString(3), entityId = c.getLongOrNull(4),
                            dedupeKey = c.getString(5), createdAt = c.getLong(6)
                        )
                    )
                }
            } },
            rewards = db.rawQuery("SELECT id,name,emoji,cost,note,archived FROM rewards", null).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(
                            RewardDto(
                                id = c.getLong(0), name = c.getString(1), emoji = c.getStringOrNull(2),
                                cost = c.getInt(3), note = c.getStringOrNull(4), archived = c.getInt(5) == 1
                            )
                        )
                    }
                }
            },
            badges = db.rawQuery("SELECT badgeCode,unlockedAt FROM badge_unlocks", null).use { c ->
                buildList {
                    while (c.moveToNext()) add(BadgeDto(c.getString(0), c.getLong(1)))
                }
            }
        )
    }

    fun serialize(backup: BackupFile): String = json.encodeToString(BackupFile.serializer(), backup)

    /**
     * Write a backup into the user-picked folder and prune old ones, keeping the
     * newest N (default 7). A missing folder is reported so the caller can ask
     * the user to re-select it rather than silently losing backups.
     */
    suspend fun exportToConfiguredFolder(): Result<Unit> = runCatching {
        val uriString = settings.settings.first().backupFolderUri
            ?: error("No backup folder configured")
        val folder = DocumentFile.fromTreeUri(context, Uri.parse(uriString))
            ?: error("Backup folder is no longer accessible")

        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(java.util.Date())
        val file = folder.createFile("application/json", "taskmanager-$stamp.json")
            ?: error("Could not create backup file")

        context.contentResolver.openOutputStream(file.uri)?.use { out ->
            out.write(serialize(buildBackup()).toByteArray())
        } ?: error("Could not open backup file for writing")

        pruneOldBackups(folder, settings.settings.first().backupRetention)
        settings.setLastBackupDate(SettingsRepository.todayKey())
    }

    private fun pruneOldBackups(folder: DocumentFile, keep: Int) {
        val backups = folder.listFiles()
            .filter { it.name?.startsWith("taskmanager-") == true }
            .sortedByDescending { it.lastModified() }
        backups.drop(keep).forEach { it.delete() }
    }

    /** Manual export: the Composable supplies a destination from a create-document intent. */
    suspend fun exportTo(uri: Uri): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(serialize(buildBackup()).toByteArray())
        } ?: error("Could not write backup")
    }

    /**
     * PRD 7.13 / OQ-4: v1 import is replace-all. The wipe and the insert run in
     * one transaction so a failure cannot leave the database half-restored.
     */
    suspend fun restoreFrom(uri: Uri): Result<Unit> = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Could not read backup")
        val parsed = json.decodeFromString(BackupFile.serializer(), text)
        require(parsed.schemaVersion <= BackupFile.CURRENT_SCHEMA_VERSION) {
            "Backup is from a newer version of the app"
        }
        replaceAll(parsed)
        searchIndex.rebuildAll()
    }

    suspend fun importLatestReplaceAll(): Result<Unit> = runCatching {
        val uriString = settings.settings.first().backupFolderUri
            ?: error("No backup folder configured")
        val folder = DocumentFile.fromTreeUri(context, Uri.parse(uriString))
            ?: error("Backup folder is no longer accessible")
        val latest = folder.listFiles()
            .filter { it.name?.startsWith("taskmanager-") == true }
            .maxByOrNull { it.lastModified() }
            ?: error("No backup file found")
        restoreFrom(latest.uri)
    }

    private suspend fun replaceAll(backup: BackupFile) {
        // Uses Room's writableDatabase so observers of these tables are
        // invalidated and the UI refreshes after a restore.
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            // Order matters: children before parents to satisfy FK constraints.
            db.execSQL("DELETE FROM task_tags")
            db.execSQL("DELETE FROM subtasks")
            db.execSQL("DELETE FROM reminders")
            db.execSQL("DELETE FROM tasks")
            db.execSQL("DELETE FROM task_lists")
            db.execSQL("DELETE FROM folders")
            db.execSQL("DELETE FROM tags")

            backup.folders.forEach {
                db.execSQL("INSERT INTO folders (id,name,color,sortOrder) VALUES (?,?,?,?)",
                    arrayOf(it.id, it.name, it.color, it.sortOrder))
            }
            backup.lists.forEach {
                db.execSQL(
                    "INSERT INTO task_lists (id,name,color,folderId,parentListId,sortOrder,archived) VALUES (?,?,?,?,?,?,?)",
                    arrayOf(it.id, it.name, it.color, it.folderId, it.parentListId, it.sortOrder, if (it.archived) 1 else 0)
                )
            }
            backup.tags.forEach {
                db.execSQL("INSERT INTO tags (id,name,color) VALUES (?,?,?)",
                    arrayOf(it.id, it.name, it.color))
            }
            backup.tasks.forEach { t ->
                db.execSQL(
                    "INSERT INTO tasks (id,title,notes,listId,dueDate,hasTime,priority,status,completedAt," +
                        "seriesId,occurrenceDate,sortOrder,createdAt,updatedAt,deletedAt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    arrayOf(t.id, t.title, t.notes, t.listId, t.dueDate, if (t.hasTime) 1 else 0,
                        t.priority, t.status, t.completedAt, t.seriesId, t.occurrenceDate,
                        t.sortOrder, t.createdAt, t.updatedAt, t.deletedAt)
                )
            }
            backup.subtasks.forEach {
                db.execSQL("INSERT INTO subtasks (id,taskId,title,done,sortOrder) VALUES (?,?,?,?,?)",
                    arrayOf(it.id, it.taskId, it.title, if (it.done) 1 else 0, it.sortOrder))
            }
            backup.reminders.forEach {
                db.execSQL(
                    "INSERT INTO reminders (id,taskId,type,offsetMinutes,absoluteTime,scheduledAt) VALUES (?,?,?,?,?,?)",
                    arrayOf(it.id, it.taskId, it.type, it.offsetMinutes, it.absoluteTime, it.scheduledAt)
                )
            }
            backup.events.forEach {
                db.execSQL(
                    "INSERT INTO events (id,type,entityType,entityId,occurredAt,payloadJson) VALUES (?,?,?,?,?,?)",
                    arrayOf(it.id, it.type, it.entityType, it.entityId, it.occurredAt, it.payloadJson)
                )
            }
            backup.ledger.forEach {
                db.execSQL(
                    "INSERT OR IGNORE INTO points_ledger (id,delta,reason,entityType,entityId,dedupeKey,createdAt) VALUES (?,?,?,?,?,?,?)",
                    arrayOf(it.id, it.delta, it.reason, it.entityType, it.entityId, it.dedupeKey, it.createdAt)
                )
            }
            backup.rewards.forEach {
                db.execSQL("INSERT INTO rewards (id,name,emoji,cost,note,archived) VALUES (?,?,?,?,?,?)",
                    arrayOf(it.id, it.name, it.emoji, it.cost, it.note, if (it.archived) 1 else 0))
            }
            backup.badges.forEach {
                db.execSQL("INSERT OR IGNORE INTO badge_unlocks (badgeCode,unlockedAt) VALUES (?,?)",
                    arrayOf(it.badgeCode, it.unlockedAt))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

// --- SQLite cursor helpers: a NULL column must not be read as 0 ---
private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)

private fun android.database.Cursor.getIntOrNull(index: Int): Int? =
    if (isNull(index)) null else getInt(index)

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)
