package com.money.monocle.domain.useCases

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class DateFormatter(private val currentDate: LocalDate? = null) {
    operator fun invoke(timestamp: Long): String {
        val recordDate = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val currentDate = currentDate ?: LocalDate.now()
        var format = "d MMM"
        if (recordDate.year != currentDate.year) format += " yyyy"
        val formatter = SimpleDateFormat(format, Locale.getDefault())
        return formatter.format(timestamp)
    }
}