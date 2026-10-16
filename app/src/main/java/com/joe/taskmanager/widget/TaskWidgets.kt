package com.joe.taskmanager.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.height
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.joe.taskmanager.MainActivity
import com.joe.taskmanager.R
import com.joe.taskmanager.data.local.AppDatabase
import com.joe.taskmanager.focus.FocusTimerService
import com.joe.taskmanager.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodayTasksWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasks = withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            AppDatabase.get(context).taskDao().getDueToday(DateUtils.startOfDay(now), DateUtils.endOfDay(now), 5)
        }
        val titles = tasks.map { it.title }
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFFF5F5F5))).padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()), verticalAlignment = Alignment.Vertical.Top) {
                Text(context.getString(R.string.widget_today_title), style = TextStyle(color = ColorProvider(Color(0xFF202124))))
                Spacer(GlanceModifier.height(8.dp))
                if (titles.isEmpty()) Text(context.getString(R.string.widget_today_empty))
                else titles.forEach { title -> Text("• $title", maxLines = 1) }
            }
        }
    }
}

class TodayTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayTasksWidget()
}

class PomodoroWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) { FocusTimerService.activeSnapshot(context) }
        val label = when {
            !snapshot.active -> context.getString(R.string.widget_focus_idle)
            snapshot.paused -> context.getString(R.string.focus_paused)
            else -> context.getString(R.string.focus_running)
        }
        val endTime = if (snapshot.active && !snapshot.paused)
            SimpleDateFormat("HH:mm", Locale.ROOT).format(Date(System.currentTimeMillis() + snapshot.remainingMillis)) else null
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFFF5F5F5))).padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()), verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(context.getString(R.string.widget_focus_title), style = TextStyle(color = ColorProvider(Color(0xFF202124))))
                Spacer(GlanceModifier.height(8.dp))
                Text(label)
                if (endTime != null) Text(context.getString(R.string.widget_focus_end, endTime))
                Text(context.getString(R.string.widget_open_timer), style = TextStyle(color = ColorProvider(Color(0xFF356859))))
            }
        }
    }
}

class PomodoroWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PomodoroWidget()
}
