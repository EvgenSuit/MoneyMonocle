package com.money.monocle.domain.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.CustomResult
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class WelcomeRepository(
    private val auth: FirebaseAuth,
    private val firestore: CollectionReference
) {
    suspend fun setBalance(currency: CurrencyEnum,
                   amount: Float): Flow<CustomResult> = flow {
        try {
            emit(CustomResult.InProgress)
            val ref = firestore.document(auth.currentUser!!.uid).collection("balance")
            ref.document("balance").set(Balance(currency.ordinal, amount)).await()
            emit(CustomResult.Success)
        } catch (e: Exception) {
            emit(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
        }
    }
}