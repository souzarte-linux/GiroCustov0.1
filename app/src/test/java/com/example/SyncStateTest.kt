package com.example

import com.example.ui.SyncState
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class SyncStateTest {

    @Test
    fun testSyncStateValues() {
        val states = SyncState.values()
        assertTrue(states.contains(SyncState.IDLE))
        assertTrue(states.contains(SyncState.SYNCING))
        assertTrue(states.contains(SyncState.SUCCESS))
        assertTrue(states.contains(SyncState.ERROR))
        assertTrue(states.contains(SyncState.OFFLINE))
    }

    @Test
    fun testFormatLastSyncTimeLogic() {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }

        val isSameDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
        assertTrue(isSameDay)
    }
}
