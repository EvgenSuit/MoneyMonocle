package com.money.monocle.ui.presentation

fun Exception.toStringIfMessageIsNull() = this.message ?: this.toString()