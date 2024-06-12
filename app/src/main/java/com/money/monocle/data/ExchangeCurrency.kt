package com.money.monocle.data

data class ExchangeCurrency(
    val amount: Float,
    val base: String,
    val date: String,
    val rates: Map<String, Float>
)