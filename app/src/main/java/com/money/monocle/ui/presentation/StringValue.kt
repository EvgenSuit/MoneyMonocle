package com.money.monocle.ui.presentation

import android.content.Context
import androidx.annotation.StringRes

sealed class StringValue {

    data class DynamicString(val value: String) : StringValue()

    data object Empty : StringValue()

    class StringResource(
        @StringRes val resId: Int
    ) : StringValue()

    fun asString(context: Context): String {
        return when (this) {
            is Empty -> ""
            is DynamicString -> value
            is StringResource -> context.getString(resId)
        }
    }
}