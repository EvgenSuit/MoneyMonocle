package com.money.monocle.data

data class Record(
    val id: String = "",
    val expense: Boolean = false,
    val category: Int = 0,
    val date: Long = 0,
    val timestamp: Long = 0,
    val amount: Float = 0f
)