package com.money.monocle.home

import com.money.monocle.domain.datastore.DataStoreManager
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow

fun mockDataStoreManager(isAccountLoadedSlot: CapturingSlot<Boolean>) = mockk<DataStoreManager> {
    coEvery { changeAccountState(capture(isAccountLoadedSlot)) } returns Unit
    coEvery { accountStateFlow() } returns flow {
        emit(isAccountLoadedSlot.captured)
    }
}