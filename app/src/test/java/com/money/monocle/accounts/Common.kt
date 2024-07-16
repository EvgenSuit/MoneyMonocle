package com.money.monocle.accounts

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.data.Account
import com.money.monocle.data.AccountName
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.mockTask
import com.money.monocle.userId
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk

val accounts = List(3) {
    Account(name = "account number $it")
} + Account(id = AccountName.MAIN.name, name = AccountName.MAIN.name)
val balances = List(4) {
    Balance(
        balance = it.toFloat() + 1f,
        currency = CurrencyEnum.entries.random().ordinal)
}

fun mockAccountsFirestore(
    isMoreThanFourAccounts: Boolean = false,
    customAccounts: List<Account>? = null,
    accountSlot: CapturingSlot<Account>? = null,
    balanceSlot: CapturingSlot<Balance>? = null,
    empty: Boolean = false,
    fetchException: Exception? = null,
    creationException: Exception? = null,
    nameChangeException: Exception? = null,
    deletionException: Exception? = null
): FirebaseFirestore =
    mockk {
        val currentAccounts = customAccounts ?: accounts
        every { collection(userId).document("accounts").collection("accounts")
            .orderBy("timestamp").get() } returns mockTask(
            mockk {
                every { isEmpty } returns empty
                every { documents } returns if (!empty) currentAccounts.map { mockk { every { toObject(Account::class.java) } returns it} }
                else listOf()
            },
            exception = fetchException)

        for (account in currentAccounts) {
            every { collection(userId).document(account.id).collection("balance").document("balance").get() } returns mockTask(
                mockk { every { toObject(Balance::class.java) } returns balances[currentAccounts.indexOf(account)] }
            )
            every { collection(userId).document(account.id).collection("balance").document("balance").delete() } returns mockTask(exception = deletionException)
        }
        if (accountSlot != null && balanceSlot != null) {
            every { collection(userId).document("accounts").collection("accounts").document(any()).set(capture(accountSlot)) } answers {
                every { collection(userId).document(accountSlot.captured.id).collection("balance").document("balance").set(capture(balanceSlot)) } returns mockTask(exception = creationException)
                mockTask(exception = creationException)
            }
        }

        every { collection(userId).document("accounts").collection("accounts").count().get(AggregateSource.SERVER) } returns mockTask(
            mockk {
                every { count } returns if (isMoreThanFourAccounts) 5 else 4
            }
        )


        every { collection(userId).document("accounts").collection("accounts").document(any()).update("name", any()) } returns mockTask(exception = nameChangeException)
        every { collection(userId).document("accounts").collection("accounts").document(any()).delete() } returns mockTask(exception = deletionException)
    }