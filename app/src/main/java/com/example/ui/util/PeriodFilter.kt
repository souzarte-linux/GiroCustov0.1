package com.example.ui.util

import com.example.ui.Period
import java.util.Calendar

fun getCurrentMonthRange(): Pair<Long, Long> {
    val calStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val calEnd = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return Pair(calStart.timeInMillis, calEnd.timeInMillis)
}

fun <T> filterByPeriod(
    records: List<T>,
    period: Period,
    customStart: Long = 0L,
    customEnd: Long = 0L,
    timestampSelector: (T) -> Long
): List<T> {
    val now = System.currentTimeMillis()
    return when (period) {
        Period.PERSONALIZADO -> {
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
        }
        Period.MENSAL -> {
            // Mês corrente (do dia 1 às 00:00 até o final do mês corrente às 23:59:59.999)
            val (startMonth, endMonth) = getCurrentMonthRange()
            records.filter { timestampSelector(it) in startMonth..endMonth }
        }
        Period.ULTIMOS_30_DIAS -> {
            val limit = now - 30L * 24 * 60 * 60 * 1000
            records.filter { timestampSelector(it) >= limit }
        }
        Period.SEMANA -> {
            val limit = now - 7L * 24 * 60 * 60 * 1000
            records.filter { timestampSelector(it) >= limit }
        }
        Period.QUINZENA -> {
            val limit = now - 15L * 24 * 60 * 60 * 1000
            records.filter { timestampSelector(it) >= limit }
        }
    }
}
