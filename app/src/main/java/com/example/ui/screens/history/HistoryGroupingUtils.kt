package com.example.ui.screens

import com.example.data.DailyRecord
import com.example.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale

val ptMonths = arrayOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
)

sealed class HistoryListItem {
    data class YearHeader(
        val year: Int,
        val totalGross: Double,
        val totalNet: Double,
        val totalDeliveries: Int = 0,
        val totalKm: Double = 0.0,
        val activeDays: Int = 0,
        val dailyAverage: Double = 0.0,
        val growthPercentage: Double? = null
    ) : HistoryListItem()

    data class MonthHeader(
        val year: Int,
        val month: Int,
        val totalGross: Double,
        val totalNet: Double,
        val activeDays: Int = 0,
        val totalDeliveries: Int = 0,
        val totalKm: Double = 0.0
    ) : HistoryListItem()

    data class WeekHeader(
        val weekStartMillis: Long,
        val weekEndMillis: Long,
        val totalGross: Double,
        val totalNet: Double,
        val weekNumber: Int = 1,
        val totalDeliveries: Int = 0,
        val totalWorkMinutes: Long = 0L,
        val hourlyRate: Double = 0.0
    ) : HistoryListItem()

    data class DayHeader(
        val dateTimestamp: Long,
        val label: String,
        val totalGross: Double = 0.0,
        val totalDeliveries: Int = 0,
        val totalWorkMinutes: Long = 0L,
        val platformCount: Int = 0
    ) : HistoryListItem()

    data class RecordRow(val record: DailyRecord) : HistoryListItem()
}

fun getHistoryListItemKey(item: HistoryListItem): String = when (item) {
    is HistoryListItem.YearHeader -> "year_${item.year}"
    is HistoryListItem.MonthHeader -> "month_${item.year}_${item.month}"
    is HistoryListItem.WeekHeader -> "week_${item.weekStartMillis}_${item.weekEndMillis}"
    is HistoryListItem.DayHeader -> "day_${item.dateTimestamp}_${item.label}"
    is HistoryListItem.RecordRow -> "record_${item.record.id}"
}

/**
 * Calculates start and end of a week subdivision truncated by month.
 */
fun getWeekSubdivisionBoundaries(recordTimestamp: Long, year: Int, month: Int): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = recordTimestamp
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val daysToMonday = when (dayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> -1
        Calendar.WEDNESDAY -> -2
        Calendar.THURSDAY -> -3
        Calendar.FRIDAY -> -4
        Calendar.SATURDAY -> -5
        Calendar.SUNDAY -> -6
        else -> 0
    }

    val mondayCal = Calendar.getInstance().apply {
        timeInMillis = cal.timeInMillis
        add(Calendar.DAY_OF_YEAR, daysToMonday)
    }

    val sundayCal = Calendar.getInstance().apply {
        timeInMillis = mondayCal.timeInMillis
        add(Calendar.DAY_OF_YEAR, 6)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    val startOfMonth = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val endOfMonth = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    val finalStart = maxOf(mondayCal.timeInMillis, startOfMonth.timeInMillis)
    val finalEnd = minOf(sundayCal.timeInMillis, endOfMonth.timeInMillis)

    return Pair(finalStart, finalEnd)
}

fun formatDayLabel(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale.forLanguageTag("pt-BR"))
    val raw = sdf.format(Date(timestamp))
    return if (raw.isNotEmpty()) {
        val capitalized = raw.substring(0, 1).uppercase(Locale.forLanguageTag("pt-BR")) + raw.substring(1)
        val deIndex = capitalized.lastIndexOf(" de ")
        if (deIndex != -1 && deIndex + 4 < capitalized.length) {
            capitalized.substring(0, deIndex + 4) +
                capitalized.substring(deIndex + 4, deIndex + 5).uppercase(Locale.forLanguageTag("pt-BR")) +
                capitalized.substring(deIndex + 5)
        } else {
            capitalized
        }
    } else {
        raw
    }
}

/**
 * Groups daily records hierarchically into a list of list items.
 * A week that spans across two months is divided; the days that fall in each month
 * count towards that specific month's totals (so week groups are subdivisions within the calendar month).
 */
fun groupRecordsHierarchically(records: List<DailyRecord>, isAscending: Boolean): List<HistoryListItem> {
    if (records.isEmpty()) return emptyList()

    // 1. Sort records first
    val sortedRecords = if (isAscending) {
        records.sortedWith(compareBy<DailyRecord> { it.dateTimestamp }.thenBy { it.id })
    } else {
        records.sortedWith(compareByDescending<DailyRecord> { it.dateTimestamp }.thenByDescending { it.id })
    }

    data class DayKey(val year: Int, val month: Int, val dayOfMonth: Int)
    data class WeekKey(val year: Int, val month: Int, val weekStart: Long, val weekEnd: Long)

    val yearToMonths = LinkedHashMap<Int, LinkedHashMap<Int, LinkedHashMap<WeekKey, LinkedHashMap<DayKey, List<DailyRecord>>>>>()

    for (record in sortedRecords) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = record.dateTimestamp
            firstDayOfWeek = Calendar.MONDAY
        }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)

        val boundaries = getWeekSubdivisionBoundaries(record.dateTimestamp, y, m)
        val weekKey = WeekKey(y, m, boundaries.first, boundaries.second)
        val dayKey = DayKey(y, m, d)

        val monthsMap = yearToMonths.getOrPut(y) { LinkedHashMap() }
        val weeksMap = monthsMap.getOrPut(m) { LinkedHashMap() }
        val daysMap = weeksMap.getOrPut(weekKey) { LinkedHashMap() }
        val dayRecords = daysMap[dayKey] ?: emptyList()
        daysMap[dayKey] = dayRecords + record
    }

    val resultList = mutableListOf<HistoryListItem>()

    for ((y, monthsMap) in yearToMonths) {
        var yearGross = 0.0
        var yearNet = 0.0
        var yearDeliveries = 0
        var yearKm = 0.0
        val yearDaysSet = mutableSetOf<DayKey>()
        val yearItems = mutableListOf<HistoryListItem>()

        for ((m, weeksMap) in monthsMap) {
            var monthGross = 0.0
            var monthNet = 0.0
            var monthDeliveries = 0
            var monthKm = 0.0
            val monthDaysSet = mutableSetOf<DayKey>()
            val monthItems = mutableListOf<HistoryListItem>()

            for ((weekKey, daysMap) in weeksMap) {
                var weekGross = 0.0
                var weekNet = 0.0
                var weekDeliveries = 0
                var weekKm = 0.0
                var weekWorkMinutes = 0L
                val weekItems = mutableListOf<HistoryListItem>()

                for ((dayKey, dayRecords) in daysMap) {
                    monthDaysSet.add(dayKey)
                    yearDaysSet.add(dayKey)

                    val representativeTimestamp = dayRecords.first().dateTimestamp
                    val dayLabel = formatDayLabel(representativeTimestamp)

                    var dayGross = 0.0
                    var dayDeliveries = 0
                    var dayKm = 0.0
                    var dayWorkMinutes = 0L
                    val platforms = mutableSetOf<String>()

                    for (rec in dayRecords) {
                        val g = CurrencyUtils.centsToDouble(rec.grossEarnings)
                        val n = CurrencyUtils.centsToDouble(rec.netProfit)
                        dayGross += g
                        dayDeliveries += rec.deliveriesCount
                        dayKm += rec.kmRodados
                        dayWorkMinutes += rec.totalWorkMinutes
                        if (rec.platform.isNotBlank()) {
                            platforms.add(rec.platform.trim())
                        }
                        weekGross += g
                        weekNet += n
                        weekDeliveries += rec.deliveriesCount
                        weekKm += rec.kmRodados
                        weekWorkMinutes += rec.totalWorkMinutes
                    }

                    weekItems.add(
                        HistoryListItem.DayHeader(
                            dateTimestamp = representativeTimestamp,
                            label = dayLabel,
                            totalGross = dayGross,
                            totalDeliveries = dayDeliveries,
                            totalWorkMinutes = dayWorkMinutes,
                            platformCount = platforms.size
                        )
                    )
                    for (rec in dayRecords) {
                        weekItems.add(HistoryListItem.RecordRow(rec))
                    }
                }

                val weekCal = Calendar.getInstance().apply {
                    timeInMillis = weekKey.weekStart
                    firstDayOfWeek = Calendar.MONDAY
                }
                val weekNum = weekCal.get(Calendar.WEEK_OF_MONTH).coerceAtLeast(1)
                val weekHourlyRate = if (weekWorkMinutes > 0) {
                    weekGross / (weekWorkMinutes.toDouble() / 60.0)
                } else 0.0

                monthItems.add(
                    HistoryListItem.WeekHeader(
                        weekStartMillis = weekKey.weekStart,
                        weekEndMillis = weekKey.weekEnd,
                        totalGross = weekGross,
                        totalNet = weekNet,
                        weekNumber = weekNum,
                        totalDeliveries = weekDeliveries,
                        totalWorkMinutes = weekWorkMinutes,
                        hourlyRate = weekHourlyRate
                    )
                )
                monthItems.addAll(weekItems)

                monthGross += weekGross
                monthNet += weekNet
                monthDeliveries += weekDeliveries
                monthKm += weekKm
            }

            yearItems.add(
                HistoryListItem.MonthHeader(
                    year = y,
                    month = m,
                    totalGross = monthGross,
                    totalNet = monthNet,
                    activeDays = monthDaysSet.size,
                    totalDeliveries = monthDeliveries,
                    totalKm = monthKm
                )
            )
            yearItems.addAll(monthItems)

            yearGross += monthGross
            yearNet += monthNet
            yearDeliveries += monthDeliveries
            yearKm += monthKm
        }

        val activeDays = yearDaysSet.size
        val dailyAvg = if (activeDays > 0) yearGross / activeDays else 0.0

        resultList.add(
            HistoryListItem.YearHeader(
                year = y,
                totalGross = yearGross,
                totalNet = yearNet,
                totalDeliveries = yearDeliveries,
                totalKm = yearKm,
                activeDays = activeDays,
                dailyAverage = dailyAvg,
                growthPercentage = null
            )
        )
        resultList.addAll(yearItems)
    }

    return resultList
}
