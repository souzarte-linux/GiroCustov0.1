package com.example.ui.util

import com.example.ui.Period
import java.util.Calendar

fun <T> filterByPeriod(
    records: List<T>,
    period: Period,
    customStart: Long,
    customEnd: Long,
    timestampSelector: (T) -> Long
): List<T> {
    val now = System.currentTimeMillis()
    return if (period == Period.PERSONALIZADO) {
        val calStart = Calendar.getInstance().apply {
            timeInMillis = customStart
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            timeInMillis = customEnd
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val startRange = calStart.timeInMillis
        val endRange = calEnd.timeInMillis
        records.filter { timestampSelector(it) in startRange..endRange }
    } else {
        val limit = when (period) {
            Period.SEMANA -> now - 7L * 24 * 60 * 60 * 1000
            Period.QUINZENA -> now - 15L * 24 * 60 * 60 * 1000
            Period.MENSAL -> now - 30L * 24 * 60 * 60 * 1000
            else -> 0L
        }
        records.filter { timestampSelector(it) >= limit }
    }
}
