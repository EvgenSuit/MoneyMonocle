package com.money.monocle.modules

import android.content.Context
import com.money.monocle.R
import com.money.monocle.domain.useCases.DateFormatter
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.datastore.accountDataStore
import com.money.monocle.domain.datastore.themeDataStore
import com.money.monocle.domain.useCases.CurrencyFormatValidator
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilsModule {
    @Provides
    @Singleton
    fun provideCoroutineScopeProvider(): CoroutineScopeProvider =
        CoroutineScopeProvider()
    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager =
        DataStoreManager(context.accountDataStore, context.themeDataStore)
    @Provides
    @Singleton
    fun provideDateFormatter(): DateFormatter = DateFormatter()

    @Provides
    @Singleton
    fun provideCurrencyFormatValidator(@ApplicationContext context: Context): CurrencyFormatValidator =
        CurrencyFormatValidator(context.resources.getInteger(R.integer.max_amount_length))
}