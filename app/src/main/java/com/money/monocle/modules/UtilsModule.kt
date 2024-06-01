package com.money.monocle.modules

import android.content.Context
import com.money.monocle.domain.DateFormatter
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.datastore.accountDataStore
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UtilsModule {
    @Provides
    fun provideCoroutineScopeProvider(): CoroutineScopeProvider =
        CoroutineScopeProvider()
    @Provides
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager =
        DataStoreManager(context.accountDataStore)
    @Provides
    fun provideDateFormatter(): DateFormatter = DateFormatter()
}