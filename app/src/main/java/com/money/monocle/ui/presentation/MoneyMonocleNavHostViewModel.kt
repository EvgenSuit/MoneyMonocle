package com.money.monocle.ui.presentation

import androidx.lifecycle.ViewModel
import com.money.monocle.domain.auth.CustomAuthStateListener
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MoneyMonocleNavHostViewModel @Inject constructor(
    authStateListener: CustomAuthStateListener
): ViewModel() {
    var isUserNullFlow = authStateListener.userUserNullFlow()
}