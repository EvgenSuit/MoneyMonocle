package com.money.monocle.domain.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.Result
import kotlinx.coroutines.tasks.await


inline fun Result.transform(
    onSuccess: (String) -> Result,
    onFailure: (String) -> Result,
): Result {
    return when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> onFailure(error)
        else -> this
    }
}


class WelcomeRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun setBalance(currency: CurrencyEnum,
                   amount: Float): Result {
        return runCatching {
            val ref = firestore.collection(auth.currentUser!!.uid).document("balance")
            ref.update("currency", currency.ordinal).await()
            ref.update("balance", amount).await()
        }.fold(
            onSuccess = { Result.Success("") },
            onFailure = { Result.Error(it.message ?: it.toString()) }
        )
    }
}