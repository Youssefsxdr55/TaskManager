package com.joe.taskmanager.util

import java.util.Calendar
import java.util.Locale

data class QuickAddResult(val title: String, val dueDate: Long?, val hasTime: Boolean)

/** Offline rule parser for common English and Egyptian Arabic date/time phrases. */
object QuickAddParser {
    fun parse(text: String, now: Long = System.currentTimeMillis()): QuickAddResult {
        var cleaned = text.trim()
        var targetDay: Long? = null
        val datePatterns = listOf(
            Regex("\\bthe day after tomorrow\\b", RegexOption.IGNORE_CASE) to 2,
            Regex("\\bday after tomorrow\\b", RegexOption.IGNORE_CASE) to 2,
            Regex("بعد\\s+بكرة|بعد\\s+غد") to 2,
            Regex("\\btomorrow\\b", RegexOption.IGNORE_CASE) to 1,
            Regex("بكرة|بكره|بكرا") to 1,
            Regex("\\btoday\\b", RegexOption.IGNORE_CASE) to 0,
            Regex("النهارده|النهاردة|اليوم") to 0,
            Regex("\\bnext\\s+week\\b", RegexOption.IGNORE_CASE) to 7,
            Regex("الأسبوع\\s+الجاي|الاسبوع\\s+الجاي") to 7
        )
        for ((pattern, days) in datePatterns) if (pattern.containsMatchIn(cleaned)) {
            targetDay = DateUtils.plusDays(DateUtils.startOfDay(now), days)
            cleaned = pattern.replace(cleaned, " ")
            break
        }
        val weekdayPatterns = listOf(
            "saturday" to Calendar.SATURDAY, "sunday" to Calendar.SUNDAY, "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY, "wednesday" to Calendar.WEDNESDAY, "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "السبت" to Calendar.SATURDAY, "الأحد" to Calendar.SUNDAY, "الاحد" to Calendar.SUNDAY,
            "الاثنين" to Calendar.MONDAY, "الإثنين" to Calendar.MONDAY, "التلات" to Calendar.TUESDAY,
            "الثلاثاء" to Calendar.TUESDAY, "الأربعاء" to Calendar.WEDNESDAY, "الاربعاء" to Calendar.WEDNESDAY,
            "الخميس" to Calendar.THURSDAY, "الجمعة" to Calendar.FRIDAY
        )
        if (targetDay == null) for ((word, weekday) in weekdayPatterns) {
            val pattern = Regex(Regex.escape(word), RegexOption.IGNORE_CASE)
            if (pattern.containsMatchIn(cleaned)) {
                val cal = Calendar.getInstance().apply { timeInMillis = DateUtils.startOfDay(now) }
                var delta = (weekday - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
                if (delta == 0) delta = 7
                targetDay = DateUtils.plusDays(cal.timeInMillis, delta)
                cleaned = pattern.replace(cleaned, " ")
                break
            }
        }
        var hasTime = false
        var hour: Int? = null
        var minute = 0
        val timePattern = Regex("(?<!\\d)([0-9]{1,2})(?::([0-9]{2}))?\\s*(am|pm|ص|م)(?![\\p{L}])", RegexOption.IGNORE_CASE)
        val match = timePattern.find(cleaned)
        if (match != null) {
            var h = match.groupValues[1].toIntOrNull()?.coerceIn(1, 12) ?: 12
            minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
            val suffix = match.groupValues[3].lowercase(Locale.ROOT)
            if (suffix == "pm" || suffix == "م") { if (h < 12) h += 12 } else if (h == 12) h = 0
            hour = h; hasTime = true
            cleaned = cleaned.removeRange(match.range)
        }
        if (hour != null && targetDay == null) targetDay = DateUtils.startOfDay(now)
        val due = targetDay?.let { day -> if (hour != null) DateUtils.combine(day, hour, minute) else day }
        val title = cleaned.replace(Regex("\\s+"), " ").trim().trim(',', '،', '-', '–')
        return QuickAddResult(title, due, hasTime)
    }
}
