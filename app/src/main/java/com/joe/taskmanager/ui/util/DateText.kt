
package com.joe.taskmanager.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.joe.taskmanager.R
import com.joe.taskmanager.util.DateUtils
import java.util.Calendar
import java.util.Locale

/**
 * Date formatting for the UI.
 *
 * PRD 8 requires Western digits (0-9) in both locales, so formatting always uses
 * Locale.US for digits and only the month/day names come from resources.
 */
object DateText {

    @Composable
    fun format(millis: Long): String {
        val now = System.currentTimeMillis()
        return when {
            DateUtils.isToday(millis) -> "${stringResource(R.string.today)} ${time(millis)}"
            DateUtils.isTomorrow(millis) -> "${stringResource(R.string.tomorrow)} ${time(millis)}"
            DateUtils.isYesterday(millis) -> "${stringResource(R.string.yesterday)} ${time(millis)}"
            else -> "${date(millis)} ${time(millis)}"
        }
    }

    @Composable
    fun dateOnly(millis: Long): String {
        val now = System.currentTimeMillis()
        return when {
            DateUtils.isToday(millis) -> stringResource(R.string.today)
            DateUtils.isTomorrow(millis) -> stringResource(R.string.tomorrow)
            DateUtils.isYesterday(millis) -> stringResource(R.string.yesterday)
            else -> date(millis)
        }
    }

    @Composable
    fun time(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val h24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val is24h = android.text.format.DateFormat.is24HourFormat(
            androidx.compose.ui.platform.LocalContext.current
        )
        return if (is24h) {
            String.format(Locale.US, "%02d:%02d", h24, minute)
        } else {
            val amPm = if (h24 < 12) "AM" else "PM"
            val h12 = when (val r = h24 % 12) { 0 -> 12; else -> r }
            String.format(Locale.US, "%d:%02d %s", h12, minute, amPm)
        }
    }

    /** e.g. "Oct 4" / "4 Oct" depending on locale, always with Western digits. */
    private fun date(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthName = MONTHS_KEYS.getOrNull(month) ?: String.format(Locale.US, "%d", month + 1)
        return String.format(Locale.US, "%s %d", monthName, day)
    }

    private val MONTHS_KEYS = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
}
