package com.money.monocle.domain

import java.text.DecimalFormatSymbols

class DecimalFormatter {
    private val symbols = DecimalFormatSymbols.getInstance()

    fun clean(input: String): String {
        println("$input; ${input.matches("^[1-9]\\d*(\\.\\d+)?\$".toRegex())}")
        if (input.isBlank()) return input
        else if (!input.matches("^[1-9]\\d*(\\.\\d+)?\$".toRegex())) return ""
        else if (input.matches("0+".toRegex())) return "0"
        else if (input.count { it == symbols.decimalSeparator } > 1) return input.substringBeforeLast(symbols.decimalSeparator)
        if (input.toFloat() > 0) return input
        if (input.toIntOrNull() != null && input.toInt() % 1.0 == 0.0) return input.toInt().toString()
        else return input
    }

}