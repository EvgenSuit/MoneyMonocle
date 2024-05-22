package com.money.monocle.ui.presentation

import kotlinx.coroutines.CoroutineScope

class CoroutineScopeProvider(private val coroutineScope: CoroutineScope? = null) {
    fun provide() = coroutineScope
}