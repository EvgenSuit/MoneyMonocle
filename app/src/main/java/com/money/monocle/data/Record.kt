package com.money.monocle.data

import com.money.monocle.R

data class Record(
    val expense: Boolean = false,
    val category: Int = 0,
    val date: Long = 0,
    val timestamp: Long = 0,
    val amount: Float = 0f
)

data class Category(
    val id: String = "",
    val name: String = "",
    val res: Int? = null
)



val expenseIcons = mapOf(
    R.string.entertainment to R.drawable.entertainment,
    R.string.groceries to R.drawable.groceries,
    R.string.insurance to R.drawable.insurance,
    R.string.transportation to R.drawable.transportation,
    R.string.utilities to R.drawable.utilities
)

val incomeIcons = mapOf(
    R.string.wage to R.drawable.wage,
    R.string.business to R.drawable.business,
    R.string.interest to R.drawable.interest,
    R.string.investment to R.drawable.investment,
    R.string.gift to R.drawable.gift,
    R.string.government_payment to R.drawable.government_payment
)

enum class IconIds {
    ENTERTAINMENT
}