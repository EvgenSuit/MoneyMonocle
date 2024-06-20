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

data class Balance(val currency: Int = -1, val balance: Float = 0f)
data class LastTimeUpdated(val lastTimeUpdated: Long? = null)