package com.money.monocle.domain.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.data.AccountName
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.CustomResult
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class WelcomeRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun setBalance(balance: Balance,
                           accountId: String) = flow {
        try {
            emit(CustomResult.InProgress)
            val ref = firestore.collection(auth.currentUser!!.uid).document(accountId).collection("balance")
            ref.document("balance").set(balance).await()
            emit(CustomResult.Success)
        } catch (e: Exception) {
            emit(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
        }
    }
}