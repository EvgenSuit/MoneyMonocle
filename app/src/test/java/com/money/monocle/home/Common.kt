package com.money.monocle.home

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.money.monocle.BalanceListener
import com.money.monocle.StatsListener
import com.money.monocle.data.AccountName
import com.money.monocle.data.Balance
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.userId
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

fun mockHomeFirestore(balanceListener: BalanceListener,
                      statsListener: StatsListener,
                      fiveDaysAgoSlot: CapturingSlot<Long>? = null): FirebaseFirestore = mockk {
        every { collection(userId).document(AccountName.MAIN.name).collection("balance")
            .addSnapshotListener(capture(balanceListener))} returns mockk<ListenerRegistration>()
        every { collection(userId).document(AccountName.MAIN.name).collection("balance")
            .addSnapshotListener(capture(balanceListener)).remove() } returns Unit

        every { collection(userId).document(AccountName.MAIN.name).collection("records").whereGreaterThan("timestamp",
            if (fiveDaysAgoSlot != null) capture(fiveDaysAgoSlot) else any())
            .addSnapshotListener(capture(statsListener))} returns mockk<ListenerRegistration>()
        every { collection(userId).document(AccountName.MAIN.name).collection("records").whereGreaterThan("timestamp",
            if (fiveDaysAgoSlot != null) capture(fiveDaysAgoSlot) else any())
            .addSnapshotListener(capture(statsListener)).remove() } returns Unit
}

fun mockDataStoreManager(
    accountId: String = AccountName.MAIN.name,
    isAccountLoadedSlot: CapturingSlot<Boolean>,
                         isWelcomeScreenShownSlot: CapturingSlot<Boolean>,
                         balanceSlot: CapturingSlot<Balance>) = mockk<DataStoreManager> {
    coEvery { changeAccountState(capture(isAccountLoadedSlot)) } returns Unit
    coEvery { isWelcomeScreenShown(capture(isWelcomeScreenShownSlot)) } returns Unit
    coEvery { setBalance(capture(balanceSlot)) } returns Unit
    // could've used flowOf if the flow didn't emit a capture slot which is not captured
    // at the time of mocking (the code inside a flow builder does not run until the flow is collected)
    coEvery { accountStateFlow() } returns flow {
        emit(isAccountLoadedSlot.captured)
    }
    coEvery { isWelcomeScreenShownFlow() } returns flow {
        emit(isWelcomeScreenShownSlot.captured)
    }
    coEvery { balanceFlow() } returns flow {
        emit(balanceSlot.captured)
    }
    coEvery { changeAccountState(any()) } returns Unit
    coEvery { setAccount(any()) } answers {
        coEvery { accountFlow() } returns flowOf(firstArg())
    }
    coEvery { accountFlow() } returns flowOf(accountId)
}