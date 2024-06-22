package com.money.monocle.domain.useCases

class CurrencyFormatValidator(private val maxAmountLength: Int) {
    operator fun invoke(input: String,
                        onValue: (String) -> Unit) {
        if ((input.isEmpty() || input.toFloatOrNull() != null)
            && input.length <= maxAmountLength) {
            onValue(input)
        }
    }
}