package com.money.monocle.domain.useCases

class CurrencyFormatValidator(private val maxAmountLength: Int) {
    operator fun invoke(input: String, onValue: (String) -> Unit) {
        if (input.isEmpty() || (input.toFloatOrNull() != null && input.toFloat() >= 0)) {
            onValue(if (input.length > maxAmountLength) input.substring(0, maxAmountLength) else input)
        }
    }
}