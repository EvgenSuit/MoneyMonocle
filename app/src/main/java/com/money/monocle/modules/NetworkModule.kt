package com.money.monocle.modules

import com.money.monocle.domain.network.FrankfurterApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
open class NetworkModule {
     protected open fun baseUrl() = "https://api.frankfurter.app".toHttpUrl()
    @Provides
    @Singleton
    fun provideFrankfurterApi(): FrankfurterApi =
        Retrofit.Builder().baseUrl(baseUrl())
            .addConverterFactory(GsonConverterFactory.create()).build().create()

}