package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CalculatorEngine
import com.example.util.AgeCalculatorEngine
import com.example.util.IndianNumberToWords
import com.example.util.UnitConverterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Calculator", appName)
    }

    @Test
    fun `test BODMAS calculation`() {
        // 10 + 5 * 2 = 20 in BODMAS
        val bodmasResult = CalculatorEngine.evaluate("10 + 5 × 2", isBodmas = true)
        assertTrue(bodmasResult is CalculatorEngine.CalculationResult.Success)
        assertEquals("20", (bodmasResult as CalculatorEngine.CalculationResult.Success).formatted)

        // 10 + 5 * 2 = 30 in Sequential (Non-BODMAS)
        val seqResult = CalculatorEngine.evaluate("10 + 5 × 2", isBodmas = false)
        assertTrue(seqResult is CalculatorEngine.CalculationResult.Success)
        assertEquals("30", (seqResult as CalculatorEngine.CalculationResult.Success).formatted)
    }

    @Test
    fun `test percentage calculation`() {
        val result = CalculatorEngine.evaluate("25000 × 6.4%", isBodmas = true)
        assertTrue(result is CalculatorEngine.CalculationResult.Success)
        assertEquals("1,600", (result as CalculatorEngine.CalculationResult.Success).formatted)
    }

    @Test
    fun `test Indian amount to words`() {
        val enWords = IndianNumberToWords.convertToEnglish("125500")
        assertEquals("Rupees One Lakh Twenty Five Thousand Five Hundred Only", enWords)

        val enDecimalWords = IndianNumberToWords.convertToEnglish("1250000.50")
        assertEquals("Rupees Twelve Lakh Fifty Thousand and Fifty Paise Only", enDecimalWords)
    }

    @Test
    fun `test unit converter`() {
        val result = UnitConverterEngine.convert(UnitConverterEngine.Category.LENGTH, "m", "cm", 5.0)
        assertEquals(500.0, result, 0.001)
    }

    @Test
    fun `test age calculator`() {
        val dob = LocalDate.of(2000, 1, 1)
        val target = LocalDate.of(2025, 1, 1)
        val age = AgeCalculatorEngine.calculateAge(dob, target)
        assertEquals(25, age?.years)
        assertEquals(0, age?.months)
        assertEquals(0, age?.days)
    }
}
