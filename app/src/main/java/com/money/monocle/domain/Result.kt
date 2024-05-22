package com.money.monocle.domain

sealed class Result(val data: String = "", val error: String = "") {
    data object Idle: Result()
    data object InProgress: Result()
    class Success(data: String): Result(data)
    class Error(error: String): Result(error = error)
}