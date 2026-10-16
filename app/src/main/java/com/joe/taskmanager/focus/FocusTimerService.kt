package com.joe.taskmanager.focus

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.joe.taskmanager.MainActivity
import com.joe.taskmanager.R
import com.joe.taskmanager.data.local.dao.EventDao
import com.joe.taskmanager.data.local.dao.FocusDao
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.EventType
import com.joe.taskmanager.data.local.entity.FocusSession
import com.joe.taskmanager.data.local.entity.FocusStatus
import com.joe.taskmanager.data.gamification.PointsEngine
import com.joe.taskmanager.notification.NotificationChannels
import com.joe.taskmanager.widget.TaskWidgetUpdater
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FocusTimerService : Service() {
    @Inject lateinit var focusDao: FocusDao
    @Inject lateinit var eventDao: EventDao
    @Inject lateinit var pointsEngine: PointsEngine
    @Inject lateinit var widgetUpdater: TaskWidgetUpdater

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var deadline = 0L
    private var remaining = 0L
    private var plannedMinutes = 25
    private var shortBreakMinutes = 5
    private var longBreakMinutes = 15
    private var sessionsBeforeLong = 4
    private var completedSessions = 0
    private var taskId: Long? = null
    private var phase = PHASE_FOCUS
    private var autoStart = false
    private var useDnd = false
    private var startedAt = 0L
    private var paused = false
    private var pending = false
    private var previousFilter: Int? = null
    private var notificationId = 71

    private val tick = object : Runnable {
        override fun run() {
            if (!paused && !pending) {
                remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                persist()
                updateNotification()
                if (remaining == 0L) finishPhase() else handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                plannedMinutes = intent.getIntExtra(EXTRA_FOCUS, 25).coerceIn(1, 180)
                shortBreakMinutes = intent.getIntExtra(EXTRA_SHORT, 5).coerceIn(1, 60)
                longBreakMinutes = intent.getIntExtra(EXTRA_LONG, 15).coerceIn(1, 90)
                sessionsBeforeLong = intent.getIntExtra(EXTRA_SESSIONS, 4).coerceIn(2, 10)
                autoStart = intent.getBooleanExtra(EXTRA_AUTO, false)
                useDnd = intent.getBooleanExtra(EXTRA_DND, false)
                taskId = intent.getLongExtra(EXTRA_TASK, -1L).takeIf { it > 0 }
                completedSessions = 0
                phase = PHASE_FOCUS
                pending = false
                applyDnd(true)
                startPhase(PHASE_FOCUS)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer(logAbort = true)
            ACTION_NEXT -> { restoreConfig(); pending = false; startPhase(phase) }
            else -> if (activeSnapshot(this).active) restoreSnapshot()
        }
        return START_STICKY
    }

    private fun startPhase(next: String) {
        phase = next
        paused = false
        pending = false
        val minutes = if (phase == PHASE_FOCUS) plannedMinutes
            else if (completedSessions % sessionsBeforeLong == 0) longBreakMinutes else shortBreakMinutes
        remaining = minutes * 60_000L
        deadline = SystemClock.elapsedRealtime() + remaining
        if (phase == PHASE_FOCUS) {
            startedAt = System.currentTimeMillis()
            applyDnd(true)
            eventDaoInsert(Event(type = EventType.FOCUS_STARTED, entityType = "focus", entityId = taskId,
                payloadJson = """{"plannedMinutes":$plannedMinutes}"""))
        } else applyDnd(false)
        persist()
        startForegroundCompat(buildNotification())
        handler.removeCallbacks(tick)
        handler.post(tick)
        updateFocusWidget()
    }

    private fun pauseTimer() {
        if (pending || paused) return
        remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0)
        paused = true
        handler.removeCallbacks(tick)
        persist(); updateNotification(); updateFocusWidget()
    }

    private fun resumeTimer() {
        if (pending || !paused) return
        paused = false
        deadline = SystemClock.elapsedRealtime() + remaining
        persist(); updateNotification(); handler.post(tick); updateFocusWidget()
    }

    private fun finishPhase() {
        handler.removeCallbacks(tick)
        if (phase == PHASE_FOCUS) {
            val session = FocusSession(taskId = taskId, startedAt = startedAt, plannedMinutes = plannedMinutes,
                actualMinutes = plannedMinutes, status = FocusStatus.COMPLETED)
            serviceScope.launch { focusDao.insert(session); pointsEngine.awardFocusSession(); pointsEngine.checkFocusBadges() }
            eventDaoInsert(Event(type = EventType.FOCUS_COMPLETED, entityType = "focus", entityId = taskId,
                payloadJson = """{"plannedMinutes":$plannedMinutes}"""))
            completedSessions++
            phase = PHASE_BREAK
        } else {
            phase = PHASE_FOCUS
        }
        soundAndVibrate()
        remaining = 0
        paused = false
        applyDnd(false)
        if (autoStart) startPhase(phase) else {
            pending = true
            persist()
            updateNotification()
            updateFocusWidget()
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun stopTimer(logAbort: Boolean) {
        handler.removeCallbacks(tick)
        if (logAbort && phase == PHASE_FOCUS && !pending) {
            val elapsed = ((plannedMinutes * 60_000L - remaining.coerceAtLeast(0)) / 60_000L).toInt().coerceAtLeast(0)
            val session = FocusSession(taskId = taskId, startedAt = startedAt, plannedMinutes = plannedMinutes,
                actualMinutes = elapsed, status = FocusStatus.ABORTED)
            serviceScope.launch { focusDao.insert(session) }
            eventDaoInsert(Event(type = EventType.FOCUS_ABORTED, entityType = "focus", entityId = taskId,
                payloadJson = """{"actualMinutes":$elapsed}"""))
        }
        applyDnd(false)
        pending = false; paused = false; remaining = 0
        persist()
        updateFocusWidget()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun restoreConfig() {
        val p = getSharedPreferences(PREFS, MODE_PRIVATE)
        plannedMinutes = p.getInt(KEY_FOCUS, 25); shortBreakMinutes = p.getInt(KEY_SHORT, 5)
        longBreakMinutes = p.getInt(KEY_LONG, 15); sessionsBeforeLong = p.getInt(KEY_SESSIONS, 4)
        autoStart = p.getBoolean(KEY_AUTO, false); useDnd = p.getBoolean(KEY_DND, false)
        taskId = p.getLong(KEY_TASK, -1L).takeIf { it > 0 }; completedSessions = p.getInt(KEY_COMPLETED, 0)
    }

    private fun restoreSnapshot() {
        val p = getSharedPreferences(PREFS, MODE_PRIVATE)
        restoreConfig(); phase = p.getString(KEY_PHASE, PHASE_FOCUS) ?: PHASE_FOCUS
        pending = p.getBoolean(KEY_PENDING, false); paused = p.getBoolean(KEY_PAUSED, false)
        remaining = if (!paused && !pending) (p.getLong(KEY_END_WALL, System.currentTimeMillis()) - System.currentTimeMillis()).coerceAtLeast(0)
            else p.getLong(KEY_REMAINING, plannedMinutes * 60_000L)
        startedAt = p.getLong(KEY_STARTED_AT, System.currentTimeMillis())
        deadline = SystemClock.elapsedRealtime() + remaining
        applyDnd(phase == PHASE_FOCUS && !paused && !pending)
        startForegroundCompat(buildNotification())
        if (remaining == 0L && !paused && !pending) finishPhase() else if (!paused && !pending) handler.post(tick)
        updateFocusWidget()
    }

    private fun persist() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(KEY_ACTIVE, !pending && remaining > 0).putBoolean(KEY_PAUSED, paused).putBoolean(KEY_PENDING, pending)
            .putLong(KEY_REMAINING, remaining).putLong(KEY_END_WALL, if (!paused && !pending) System.currentTimeMillis() + remaining else 0L)
            .putString(KEY_PHASE, phase).putLong(KEY_STARTED_AT, startedAt)
            .putInt(KEY_FOCUS, plannedMinutes).putInt(KEY_SHORT, shortBreakMinutes).putInt(KEY_LONG, longBreakMinutes)
            .putInt(KEY_SESSIONS, sessionsBeforeLong).putInt(KEY_COMPLETED, completedSessions)
            .putBoolean(KEY_AUTO, autoStart).putBoolean(KEY_DND, useDnd).putLong(KEY_TASK, taskId ?: -1L).apply()
    }

    private fun updateNotification() {
        val manager = getSystemService<NotificationManager>() ?: return
        manager.notify(notificationId, buildNotification())
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(this, 72, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, NotificationChannels.FOCUS_TIMER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (phase == PHASE_FOCUS) getString(R.string.focus_phase) else getString(R.string.focus_break_phase))
            .setContentText(if (pending) getString(R.string.focus_phase_ready) else if (paused) getString(R.string.focus_paused) else getString(R.string.focus_running))
            .setOngoing(!pending).setOnlyAlertOnce(true).setContentIntent(open)
        if (!pending && !paused) builder.setWhen(SystemClock.elapsedRealtime() + remaining).setUsesChronometer(true).setChronometerCountDown(true)
        if (pending) builder.addAction(0, getString(R.string.focus_start_next), actionPendingIntent(ACTION_NEXT, 73))
        else if (paused) builder.addAction(0, getString(R.string.focus_resume), actionPendingIntent(ACTION_RESUME, 74))
        else builder.addAction(0, getString(R.string.focus_pause), actionPendingIntent(ACTION_PAUSE, 75))
        builder.addAction(0, getString(R.string.focus_stop), actionPendingIntent(ACTION_STOP, 76))
        return builder.build()
    }

    private fun actionPendingIntent(action: String, id: Int) = PendingIntent.getService(this, id,
        Intent(this, FocusTimerService::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(notificationId, notification)
    }

    private fun applyDnd(enabled: Boolean) {
        if (!useDnd) return
        val manager = getSystemService<NotificationManager>() ?: return
        if (!manager.isNotificationPolicyAccessGranted) return
        runCatching {
            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
            if (enabled) {
                if (previousFilter == null) {
                    val stored = prefs.getInt(KEY_PREVIOUS_FILTER, -1)
                    previousFilter = if (stored >= 0) stored else manager.currentInterruptionFilter.also {
                        prefs.edit().putInt(KEY_PREVIOUS_FILTER, it).apply()
                    }
                }
                manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            } else {
                val stored = prefs.getInt(KEY_PREVIOUS_FILTER, -1)
                (previousFilter ?: stored.takeIf { it >= 0 })?.let(manager::setInterruptionFilter)
                previousFilter = null
                prefs.edit().remove(KEY_PREVIOUS_FILTER).apply()
            }
        }
    }

    private fun soundAndVibrate() {
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75).apply { startTone(ToneGenerator.TONE_PROP_BEEP, 450); release() } }
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) getSystemService<VibratorManager>()?.defaultVibrator else getSystemService<Vibrator>()
            if (Build.VERSION.SDK_INT >= 26) vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            else vibrator?.vibrate(300)
        }
    }

    private fun updateFocusWidget() {
        serviceScope.launch { widgetUpdater.updatePomodoro() }
    }

    private fun eventDaoInsert(event: Event) { serviceScope.launch { eventDao.insert(event) } }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        applyDnd(false)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.joe.taskmanager.focus.START"
        const val ACTION_PAUSE = "com.joe.taskmanager.focus.PAUSE"
        const val ACTION_RESUME = "com.joe.taskmanager.focus.RESUME"
        const val ACTION_STOP = "com.joe.taskmanager.focus.STOP"
        const val ACTION_NEXT = "com.joe.taskmanager.focus.NEXT"
        const val EXTRA_FOCUS = "focusMinutes"
        const val EXTRA_SHORT = "shortMinutes"
        const val EXTRA_LONG = "longMinutes"
        const val EXTRA_SESSIONS = "sessionsBeforeLong"
        const val EXTRA_AUTO = "autoStart"
        const val EXTRA_DND = "doNotDisturb"
        const val EXTRA_TASK = "taskId"
        const val PREFS = "focus_timer"
        private const val KEY_ACTIVE = "active"
        private const val KEY_PAUSED = "paused"
        private const val KEY_PENDING = "pending"
        private const val KEY_REMAINING = "remaining"
        private const val KEY_END_WALL = "endWall"
        private const val KEY_PREVIOUS_FILTER = "previousInterruptionFilter"
        private const val KEY_PHASE = "phase"
        private const val KEY_STARTED_AT = "startedAt"
        private const val KEY_FOCUS = "focus"
        private const val KEY_SHORT = "short"
        private const val KEY_LONG = "long"
        private const val KEY_SESSIONS = "sessions"
        private const val KEY_COMPLETED = "completedSessions"
        private const val KEY_AUTO = "autoStart"
        private const val KEY_DND = "dnd"
        private const val KEY_TASK = "taskId"
        const val PHASE_FOCUS = "FOCUS"
        const val PHASE_BREAK = "BREAK"
        fun activeSnapshot(context: Context): TimerSnapshot {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return TimerSnapshot(p.getBoolean(KEY_ACTIVE, false), p.getBoolean(KEY_PAUSED, false), p.getBoolean(KEY_PENDING, false),
                p.getLong(KEY_REMAINING, 0), p.getString(KEY_PHASE, PHASE_FOCUS) ?: PHASE_FOCUS, p.getInt(KEY_COMPLETED, 0))
        }
        fun command(context: Context, action: String, extras: Intent? = null) {
            val intent = Intent(context, FocusTimerService::class.java).setAction(action)
            extras?.extras?.let { intent.putExtras(it) }
            if (action == ACTION_START || action == ACTION_NEXT) androidx.core.content.ContextCompat.startForegroundService(context, intent)
            else context.startService(intent)
        }
    }
}

data class TimerSnapshot(val active: Boolean, val paused: Boolean, val pending: Boolean, val remainingMillis: Long, val phase: String, val completedSessions: Int)
