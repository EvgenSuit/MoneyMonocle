package com.money.monocle.home

import com.money.monocle.domain.datastore.DataStoreManager
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

fun mockDataStoreManager(isAccountLoadedSlot: CapturingSlot<Boolean>) = mockk<DataStoreManager> {
    coEvery { changeAccountState(capture(isAccountLoadedSlot)) } returns Unit
    // could've used flowOf if the flow didn't emit a capture slot which is not captured
    // at the time of mocking (the code inside a flow builder does not run until the flow is collected)
    coEvery { accountStateFlow() } returns flow {
        emit(isAccountLoadedSlot.captured)
    }
}