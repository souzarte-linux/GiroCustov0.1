package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JuneSeedingTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: GiroCustoDatabase
    private lateinit var repository: GiroCustoRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        GiroCustoDatabase.setTestDatabase(db)
        repository = GiroCustoRepository(db)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testJune2026SeedingDataProperties() = runTest(testDispatcher) {
        // Seed the June 2026 data
        repository.seedJune2026Data()

        val allRecords = repository.allRecordsFlow.first()

        // 1. Check we have exactly 25 records
        assertEquals("Should contain exactly 25 work days", 25, allRecords.size)

        // 2. Check all are in June 2026 and none on Saturday
        val cal = Calendar.getInstance()
        for (record in allRecords) {
            assertTrue("Record date should be in June 2026", record.dateString.startsWith("2026-06"))
            
            cal.timeInMillis = record.dateTimestamp
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            assertNotEquals("Should never insert data on Saturdays", Calendar.SATURDAY, dayOfWeek)

            // Validate that startOdometer is less than endOdometer
            assertTrue("End odometer should be greater than start odometer", record.endOdometer > record.startOdometer)
            
            // Validate calculations
            val km = record.endOdometer - record.startOdometer
            assertTrue("Km rodados should be positive", km > 0)
            
            // Check that calculations match
            val expectedFuelCost = (km / 40.0) * record.fuelPrice
            assertEquals(expectedFuelCost, record.fuelCost, 0.001)

            // Gross earnings and net profit must be positive and consistent
            assertTrue("Gross earnings should be positive", record.grossEarnings > 0)
            assertEquals(
                record.grossEarnings - record.fuelCost - record.wearCost - record.proportionalFixedCost - record.foodExpense,
                record.netProfit,
                0.001
            )
        }

        // 3. Check continuous odometer sequence
        // Since we sort by timestamp DESC or ASC, let's verify continuity.
        val sortedAsc = allRecords.sortedBy { it.dateTimestamp }
        for (i in 0 until sortedAsc.size - 1) {
            val current = sortedAsc[i]
            val next = sortedAsc[i + 1]
            assertEquals("Odometer should be continuous between subsequent work days", current.endOdometer, next.startOdometer, 0.001)
        }
    }
}
