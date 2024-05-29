package com.money.monocle

data class Record(
    val isExpense: Boolean = false,
    val category: Int = 0,
    val timestamp: Long = 0,
    val amount: Float = 0f
)