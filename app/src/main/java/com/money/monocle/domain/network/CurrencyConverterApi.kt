package com.money.monocle.domain.network

import com.money.monocle.data.ExchangeCurrency
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

import kotlinx.coroutines.Deferred
interface FrankfurterApi {
    @GET("latest")
    suspend fun convert(
        @Query("amount") amount: Float,
        @Query("from") from: String,
        @Query("to") to: String
    ): ExchangeCurrency
}