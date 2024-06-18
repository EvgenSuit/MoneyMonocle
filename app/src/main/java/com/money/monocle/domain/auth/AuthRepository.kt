package com.money.monocle.domain.auth

import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.BuildConfig
import com.money.monocle.data.Balance
import com.money.monocle.ui.presentation.auth.AuthState
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val oneTapClient: SignInClient
) {
    val authRef = auth

    suspend fun signUp(authState: AuthState) {
        auth.createUserWithEmailAndPassword(authState.email!!, authState.password!!).await()
        auth.currentUser?.updateProfile(UserProfileChangeRequest.Builder()
            .setDisplayName(authState.username!!).build())?.await()
        setBalance()
    }
    suspend fun signIn(authState: AuthState) {
        auth.signInWithEmailAndPassword(authState.email!!, authState.password!!).await()
    }

    suspend fun googleSignIn(): IntentSender? {
        val res = oneTapClient.beginSignIn(buildSignInRequest()).await()
        return res?.pendingIntent?.intentSender
    }

    private suspend fun setBalance() {
        firestore.collection("data").document(auth.currentUser!!.uid).collection("balance")
            .document("balance").set(Balance()).await()
    }

    suspend fun signInWithIntent(intent: Intent) {
        val googleIdToken = oneTapClient.getSignInCredentialFromIntent(intent).googleIdToken
        val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
        val res = auth.signInWithCredential(googleCredentials).await()
        if (res.additionalUserInfo?.isNewUser == true) {
            setBalance()
        }
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        val apiKey = BuildConfig.GOOGLE_AUTH_WEB_API_KEY
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.Builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(apiKey)
                    .build())
            .setAutoSelectEnabled(true)
            .build()
    }
}

