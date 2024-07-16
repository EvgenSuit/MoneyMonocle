package com.money.monocle.data

import java.time.Instant
import java.util.UUID

data class Account(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val timestamp: Long = 0)

enum class AccountName {
    MAIN
}