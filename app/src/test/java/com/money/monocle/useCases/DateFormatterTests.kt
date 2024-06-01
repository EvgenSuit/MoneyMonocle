package com.money.monocle.useCases

import com.money.monocle.domain.DateFormatter
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

class DateFormatterTests {
    @Test
    fun moveOneDay_isFormatCorrect() {
        val currentDate = LocalDate.now().plusDays(1)
        val inputTimestamp = Instant.now().toEpochMilli()
        val formatter = SimpleDateFormat("d MMMM", Locale.getDefault())
        assertEquals(DateFormatter(currentDate).invoke(inputTimestamp),
            formatter.format(inputTimestamp))
    }
}