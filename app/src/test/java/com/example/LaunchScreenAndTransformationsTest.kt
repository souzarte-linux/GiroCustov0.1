package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyRecord
import com.example.data.GiroCustoDatabase
import com.example.data.GiroCustoRepository
import com.example.data.Vehicle
import com.example.ui.GiroCustoViewModel
import com.example.util.CurrencyVisualTransformation
import com.example.util.PauseDurationVisualTransformation
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LaunchScreenAndTransformationsTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: GiroCustoDatabase
    private lateinit var repository: GiroCustoRepository
    private lateinit var context: Context
    private lateinit var viewModel: GiroCustoViewModel

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
        viewModel = GiroCustoViewModel(context as Application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testCurrencyVisualTransformation_formattingAndOffsets() {
        val transformation = CurrencyVisualTransformation(prefix = "R$ ")

        // Test 1: Empty text
        val emptyResult = transformation.filter(AnnotatedString(""))
        assertEquals("", emptyResult.text.text)
        assertEquals(0, emptyResult.offsetMapping.originalToTransformed(0))
        assertEquals(0, emptyResult.offsetMapping.transformedToOriginal(0))

        // Test 2: Value text "150,50"
        val input = "150,50"
        val result = transformation.filter(AnnotatedString(input))
        assertEquals("R$ 150,50", result.text.text)

        // Monotonicity and bounds in originalToTransformed
        var prevTransformed = -1
        for (i in 0..input.length) {
            val trans = result.offsetMapping.originalToTransformed(i)
            assertTrue("Index $i out of bounds: $trans", trans in 0..result.text.length)
            assertTrue("originalToTransformed not monotonic at $i: $trans < $prevTransformed", trans >= prevTransformed)
            prevTransformed = trans
        }

        // Monotonicity and bounds in transformedToOriginal
        var prevOrig = -1
        for (i in 0..result.text.length) {
            val orig = result.offsetMapping.transformedToOriginal(i)
            assertTrue("Transformed index $i mapped out of bounds: $orig", orig in 0..input.length)
            assertTrue("transformedToOriginal not monotonic at $i: $orig < $prevOrig", orig >= prevOrig)
            prevOrig = orig
        }

        // Out of bound inputs gracefully clamped
        assertEquals(result.text.length, result.offsetMapping.originalToTransformed(100))
        assertEquals(3, result.offsetMapping.originalToTransformed(-5))
        assertEquals(input.length, result.offsetMapping.transformedToOriginal(100))
        assertEquals(0, result.offsetMapping.transformedToOriginal(-5))
    }

    @Test
    fun testPauseDurationVisualTransformation_formattingCases() {
        val transformation = PauseDurationVisualTransformation()

        // 1. Empty/blank
        assertEquals("", transformation.filter(AnnotatedString("")).text.text)
        assertEquals("   ", transformation.filter(AnnotatedString("   ")).text.text)

        // 2. Digits only: "30" -> "30 minutos"
        assertEquals("30 minutos", transformation.filter(AnnotatedString("30")).text.text)

        // 3. Digits only: "1" -> "1 minuto"
        assertEquals("1 minuto", transformation.filter(AnnotatedString("1")).text.text)

        // 4. Digits only: "60" -> "1 hora"
        assertEquals("1 hora", transformation.filter(AnnotatedString("60")).text.text)

        // 5. Digits only: "120" -> "2 horas"
        assertEquals("2 horas", transformation.filter(AnnotatedString("120")).text.text)

        // 6. Digits only: "90" -> "1 hora e 30 minutos"
        assertEquals("1 hora e 30 minutos", transformation.filter(AnnotatedString("90")).text.text)

        // 7. Digits only: "0" -> "0 minuto"
        assertEquals("0 minuto", transformation.filter(AnnotatedString("0")).text.text)

        // 8. With colon: "1:30" -> "1 hora e 30 minutos"
        assertEquals("1 hora e 30 minutos", transformation.filter(AnnotatedString("1:30")).text.text)

        // 9. With colon: "01:00" -> "1 hora"
        assertEquals("1 hora", transformation.filter(AnnotatedString("01:00")).text.text)

        // 10. With colon: "02:00" -> "2 horas"
        assertEquals("2 horas", transformation.filter(AnnotatedString("02:00")).text.text)

        // 11. With colon: "00:01" -> "1 minuto"
        assertEquals("1 minuto", transformation.filter(AnnotatedString("00:01")).text.text)

        // 12. With colon: "00:00" -> "0 minuto"
        assertEquals("0 minuto", transformation.filter(AnnotatedString("00:00")).text.text)
    }

    @Test
    fun testPauseDurationVisualTransformation_offsetMappingSafety() {
        val transformation = PauseDurationVisualTransformation()
        val inputs = listOf("30", "90", "1:30", "02:00", "0", "125")

        for (input in inputs) {
            val result = transformation.filter(AnnotatedString(input))
            val textLen = result.text.length

            // originalToTransformed
            var prevTrans = -1
            for (i in 0..input.length) {
                val trans = result.offsetMapping.originalToTransformed(i)
                assertTrue("originalToTransformed out of bounds for input '$input' at offset $i: $trans", trans in 0..textLen)
                assertTrue("originalToTransformed monotonic violation for input '$input' at offset $i: $trans < $prevTrans", trans >= prevTrans)
                prevTrans = trans
            }

            // transformedToOriginal
            var prevOrig = -1
            for (i in 0..textLen) {
                val orig = result.offsetMapping.transformedToOriginal(i)
                assertTrue("transformedToOriginal out of bounds for input '$input' at offset $i: $orig", orig in 0..input.length)
                assertTrue("transformedToOriginal monotonic violation for input '$input' at offset $i: $orig < $prevOrig", orig >= prevOrig)
                prevOrig = orig
            }

            // Extreme clamps
            assertEquals(textLen, result.offsetMapping.originalToTransformed(999))
            assertEquals(0, result.offsetMapping.originalToTransformed(-10))
            assertEquals(input.length, result.offsetMapping.transformedToOriginal(999))
            assertEquals(0, result.offsetMapping.transformedToOriginal(-10))
        }
    }

    @Test
    fun testDailyLaunchCoordinator_normalizePauseDuration() {
        assertEquals("", com.example.ui.delegates.DailyLaunchCoordinator.normalizePauseDuration(""))
        assertEquals("", com.example.ui.delegates.DailyLaunchCoordinator.normalizePauseDuration("   "))
        assertEquals("00:30", com.example.ui.delegates.DailyLaunchCoordinator.normalizePauseDuration("30"))
        assertEquals("01:30", com.example.ui.delegates.DailyLaunchCoordinator.normalizePauseDuration("90"))
        assertEquals("01:30", com.example.ui.delegates.DailyLaunchCoordinator.normalizePauseDuration("1:30"))
        assertEquals("01:30", com.example.ui.delegates.DailyLaunchCoordinator.normalizePauseDuration("01:30"))
        assertEquals("02:00", com.example.ui.delegates.DailyLaunchCoordinator.normalizePauseDuration("2:00"))
        assertEquals("00:45", com.example.ui.delegates.DailyLaunchCoordinator.normalizePauseDuration(":45"))
        assertEquals("01:00", com.example.ui.delegates.DailyLaunchCoordinator.normalizePauseDuration("1:"))
        assertEquals("00:00", com.example.ui.delegates.DailyLaunchCoordinator.normalizePauseDuration("0"))
    }

    @Test
    fun testSaveDailyRecord_normalizesPauseDurationBeforeSaving() = runTest(testDispatcher) {
        val vehicle = Vehicle(id = 1L, model = "Mobi", active = true, currentOdometer = 10000.0)
        db.vehicleDao().insertVehicle(vehicle)
        repository.setActiveVehicle(1L)

        // Set inputs: raw "90" for pause duration
        viewModel.startOdometer.value = "10000"
        viewModel.endOdometer.value = "10100"
        viewModel.grossEarnings.value = "200.00"
        viewModel.deliveriesCount.value = "10"
        viewModel.fuelPrice.value = "5.50"
        viewModel.pauseDuration.value = "90" // 90 min

        var successCalled = false
        var errorMsg: String? = null

        viewModel.saveDailyRecord(
            onSuccess = { successCalled = true },
            onError = { errorMsg = it }
        )

        assertNull(errorMsg)
        assertTrue(successCalled)

        val records = repository.allRecordsFlow.first()
        assertEquals(1, records.size)
        val saved = records.first()

        // pauseDuration must be normalized to "01:30"
        assertEquals("01:30", saved.pauseDuration)

        // After save, form must be reset:
        // startOdometer becomes endOdometer of last record: "10100" (no .0 if integer!)
        assertEquals("10100", viewModel.startOdometer.value)
        assertEquals("", viewModel.endOdometer.value)
        assertEquals("", viewModel.fuelPrice.value)
        assertEquals("", viewModel.grossEarnings.value)
        assertEquals("", viewModel.pauseDuration.value)
    }

    @Test
    fun testPrefillLaunchFields_formattingOfDecimalVsIntegerOdometer() = runTest(testDispatcher) {
        // Decimal odometer: 10050.5
        val vehicle = Vehicle(id = 2L, model = "CG 160", active = true, currentOdometer = 10050.5)
        db.vehicleDao().insertVehicle(vehicle)
        repository.setActiveVehicle(2L)

        viewModel.prefillLaunchFields()
        assertEquals("10050.5", viewModel.startOdometer.value)
        assertEquals("", viewModel.fuelPrice.value)

        // Integer odometer: 10050.0 -> without .0
        val vehicle2 = Vehicle(id = 3L, model = "Factor", active = true, currentOdometer = 10050.0)
        db.vehicleDao().insertVehicle(vehicle2)
        repository.setActiveVehicle(3L)

        viewModel.prefillLaunchFields()
        assertEquals("10050", viewModel.startOdometer.value)
        assertEquals("", viewModel.fuelPrice.value)
    }
}
