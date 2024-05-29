package com.money.monocle.data


enum class CurrencyEnum {
    USD,
    EUR,
}
fun simpleCurrencyMapper(ordinal: Int): String =
    when(ordinal) {
        CurrencyEnum.USD.ordinal -> "$"
        CurrencyEnum.EUR.ordinal -> "€"
        else -> ""
    }

data class Currency(val currency: Int = 0)
data class Balance(val balance: Float = 0f)