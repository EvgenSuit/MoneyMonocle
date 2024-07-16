package com.money.monocle

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.accounts.accounts
import com.money.monocle.data.AccountName
import com.money.monocle.data.Balance
import com.money.monocle.domain.datastore.DataStoreManager
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
open class BaseTestClass {
    var testScope = TestScope(StandardTestDispatcher())
    val snackbarScope = TestScope()
    open var auth: FirebaseAuth = mockAuth()
    lateinit var firestore: FirebaseFirestore

    val accountFlow = MutableSharedFlow<String>(replay = 1)
    val balanceFlow = MutableSharedFlow<Balance>(replay = 1)
    open var dataStoreManager = mockDataStoreManager(accountFlow, balanceFlow)

    val exception = Exception("exception")

    fun cancelJobs() {
        testScope.coroutineContext.cancelChildren()
    }

    @Before
    fun initBaseTestClass() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun TestScope.setAccountTest(i: Int): String {
        val accountId = accounts[i].id
        dataStoreManager.setAccount(accountId)
        advanceUntilIdle()
        return accountId
    }

    @After
    fun clean() {
        Dispatchers.resetMain()
        unmockkAll()
    }
}