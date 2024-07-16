package com.money.monocle.domain.accounts

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.R
import com.money.monocle.data.Account
import com.money.monocle.data.Balance
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.isActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await

class AccountsRepository(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    private val dataStoreManager: DataStoreManager
) {
    private val userRef = firestore.collection(auth.currentUser!!.uid)
    private val accountsRef = userRef.document("accounts").collection("accounts")
    suspend fun fetchAccounts(onAccounts: (List<Account>) -> Unit,
                              onBalances: (List<AccountBalance>) -> Unit) = flow {
        try {
            val accountsCollection = accountsRef.orderBy("timestamp").get().await()
            val accounts = if (!accountsCollection.isEmpty) accountsCollection.documents.mapNotNull { it.toObject(Account::class.java) } else listOf()
            val balances = accounts.mapNotNull {
                val balance = userRef.document(it.id)
                    .collection("balance").document("balance").get().await().toObject(Balance::class.java)
                if (balance != null) AccountBalance(it.id, balance) else null
            }
            onBalances(balances)
            onAccounts(accounts)
            emit(CustomResult.Success)
        } catch (e: Exception) {
            emit(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
        }
    }
    suspend fun createAccount(account: Account, balance: Balance) = flow {
        try {
            if (accountsRef.count().get(AggregateSource.SERVER).await().count >= 5) {
                emit(CustomResult.ResourceError(R.string.max_accounts_restriction))
                return@flow
            }
            accountsRef.document(account.id).set(account).await()
            userRef.document(account.id).collection("balance").document("balance").set(balance).await()
            emit(CustomResult.Success)
        } catch (e: Exception) {
            emit(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
        }
    }
    suspend fun editAccountName(accountId: String, name: String) = flow {
        try {
            accountsRef.document(accountId).update("name", name).await()
            emit(CustomResult.Success)
        } catch (e: Exception) {
            emit(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
        }
    }
    suspend fun deleteAccount(accountId: String) = flow {
        try {
            accountsRef.document(accountId).delete().await()
            userRef.document(accountId).collection("balance").document("balance").delete().await()
            emit(CustomResult.Success)
        } catch (e: Exception) {
            emit(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
        }
    }
    suspend fun switchAccount(accountId: String) = flow {
        try {
            dataStoreManager.setAccount(accountId)
            emit(CustomResult.Success)
        } catch (e: Exception) {
            emit(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
        }
    }
}
data class AccountBalance(
    val accountId: String,
    val balance: Balance
)