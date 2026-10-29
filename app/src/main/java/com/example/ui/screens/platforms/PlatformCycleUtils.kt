package com.example.ui.screens.platforms

data class LocalCycleEntry(val cut: String, val payDelay: String)

fun parseCycleEntries(json: String): List<LocalCycleEntry> {
    if (json.isBlank()) return listOf(LocalCycleEntry("1", "7"), LocalCycleEntry("16", "7"))
    return try {
        json.split(",").mapNotNull { entryStr ->
            val parts = entryStr.split(":")
            if (parts.size == 2) {
                val cut = parts[0]
                val payDelay = parts[1]
                LocalCycleEntry(cut, payDelay)
            } else null
        }
    } catch (_: Exception) {
        listOf(LocalCycleEntry("1", "7"), LocalCycleEntry("16", "7"))
    }
}

fun formatCycleEntries(entries: List<LocalCycleEntry>): String {
    return entries.joinToString(",") { 
        val cutVal = it.cut.toIntOrNull()?.coerceIn(1, 28) ?: 1
        val delayVal = it.payDelay.toIntOrNull()?.coerceAtLeast(1) ?: 7
        "$cutVal:$delayVal"
    }
}
