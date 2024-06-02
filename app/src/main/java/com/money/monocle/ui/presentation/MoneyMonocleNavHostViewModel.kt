package com.money.monocle.ui.presentation

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.auth.CustomAuthStateListener
import com.money.monocle.domain.datastore.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class MoneyMonocleNavHostViewModel @Inject constructor(
    authStateListener: CustomAuthStateListener,
    dataStoreManager: DataStoreManager,
): ViewModel() {
    var isUserNullFlow = authStateListener.isUserNullFlow()
    val isAccountLoadedFlow = dataStoreManager.accountStateFlow()
    val currentUser = authStateListener.userRef
}