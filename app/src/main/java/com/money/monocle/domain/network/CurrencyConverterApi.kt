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

object FrankfurterService {
    private const val BASE_URL = "https://api.frankfurter.app"
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build()
    }
    val api: FrankfurterApi by lazy {
        retrofit.create(FrankfurterApi::class.java)
    }
}