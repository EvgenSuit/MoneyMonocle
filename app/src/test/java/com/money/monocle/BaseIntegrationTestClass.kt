package com.money.monocle

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import io.mockk.every
import io.mockk.mockk

open class BaseIntegrationTestClass: BaseTestClass() {
    val currentBalance = 233.4f
    val currency = CurrencyEnum.EUR
    fun showHomeScreen(balanceListener: BalanceListener) {
        val mockedDocs = listOf(mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { toObject(Balance::class.java) } returns Balance(currency.ordinal, currentBalance)
        })
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns mockedDocs
        }
        balanceListener.captured.onEvent(mockedSnapshot, null)
    }
    fun showWelcomeScreen(balanceListener: BalanceListener) {
        val mockedDocs = listOf(mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { toObject(Balance::class.java) } returns Balance()
        })
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns mockedDocs
        }
        balanceListener.captured.onEvent(mockedSnapshot, null)
    }
}

